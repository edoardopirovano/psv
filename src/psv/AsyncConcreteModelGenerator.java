package psv;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Module;
import parser.ast.*;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;
import simulator.TransitionList;

import java.util.*;

public class AsyncConcreteModelGenerator extends DefaultModelGenerator {
    AsyncSwarmFile swarmFile;

    private final VarList varList = new VarList();
    private final LabelList labelList;
    private final List<String> labelNames;
    final List<List<Module>> renamedAgents;
    private Module environment;
    final FaultProvider faultProvider;

    // Model exploration info

    // State currently being explored
    State exploreState;

    // List of currently available transitions
    List<String> actionNames = new ArrayList<>();
    TransitionList transitionList = new TransitionList();

    // Has the transition list been built?
    private boolean transitionListBuilt;

    AsyncConcreteModelGenerator(final AsyncSwarmFile swarmFile, final FaultFile ff, final List<Integer> m) throws PrismException {
        this.swarmFile = swarmFile;
        if (ff == null)
            faultProvider = new FaultProvider();
        else
            faultProvider = new FaultFileFaultProvider(ff);
        final Values constantValues = new Values();

        renamedAgents = new ArrayList<>();
        for (int i = 0; i < m.size(); ++i) {
            for (int j = 0; j < m.get(i); ++j) {
                final RenamedModule rm = new RenamedModule("agent", "agent_" + i + "_" + j);
                for (final Declaration decl : swarmFile.getAgents().get(i).getDeclarations()) {
                    final Declaration declarationCopy = (Declaration) decl.deepCopy();
                    declarationCopy.setName(decl.getName() + "_" + i + "_" + j);
                    varList.addVar(declarationCopy, 0, constantValues);
                    rm.addRename(decl.getName(), declarationCopy.getName());
                }
                if (renamedAgents.size() == i)
                    renamedAgents.add(new ArrayList<>());
                Module agentCopy = (Module) swarmFile.getAgents().get(i).deepCopy();
                agentCopy = (Module) agentCopy.rename(rm);
                renamedAgents.get(i).add(agentCopy);
                faultProvider.introduceConcrete(rm, i);
            }
        }
        final RenamedModule rm = new RenamedModule("environment", "environment");
        for (final Declaration decl : swarmFile.getEnvironment().getDeclarations()) {
            final Declaration declarationCopy = (Declaration) decl.deepCopy();
            declarationCopy.setName(decl.getName() + "_E");
            varList.addVar(declarationCopy, 0, constantValues);
            rm.addRename(decl.getName(), declarationCopy.getName());
        }
        environment = (Module) swarmFile.getEnvironment().deepCopy();
        environment = (Module) environment.rename(rm);
        labelList = (LabelList) swarmFile.getLabelList().deepCopy();
        labelNames = labelList.getLabelNames();

        final FindAllVars replacer = new FindAllVars(getVarNames(), getVarTypes());
        for (final List<Module> agents : renamedAgents) {
            for (final Module agent : agents)
                agent.accept(replacer);
        }
        environment.accept(replacer);
        labelList.accept(replacer);

        transitionList = new TransitionList();
        transitionListBuilt = false;
    }

    @Override
    public ModelType getModelType() {
        return ModelType.MDP;
    }

    @Override
    public int getNumVars() {
        return varList.getNumVars();
    }

    @Override
    public List<String> getVarNames() {
        final List<String> varNames = new ArrayList<>(varList.getNumVars());
        for (int i = 0; i < varList.getNumVars(); ++i)
            varNames.add(varList.getName(i));
        return varNames;
    }

    @Override
    public List<Type> getVarTypes() {
        final List<Type> varTypes = new ArrayList<>(varList.getNumVars());
        for (int i = 0; i < varList.getNumVars(); ++i)
            varTypes.add(varList.getType(i));
        return varTypes;
    }

    @Override
    public int getNumLabels() {
        return labelList.size();
    }

    @Override
    public List<String> getLabelNames() {
        return labelNames;
    }

    @Override
    public String getLabelName(final int i) throws PrismException {
        return labelList.getLabelName(i);
    }

    @Override
    public int getLabelIndex(final String label) {
        return labelList.getLabelIndex(label);
    }

    @Override
    public boolean hasSingleInitialState() throws PrismException {
        return true;
    }

    @Override
    public State getInitialState() throws PrismException {
        final State initialState = new State(getNumVars());
        for (int i = 0; i < varList.getNumVars(); ++i)
            initialState.setValue(i, varList.getDeclaration(i).getStartOrDefault().evaluate());
        return initialState;
    }

    @Override
    public void exploreState(final State exploreState) throws PrismException {
        this.exploreState = exploreState;
        transitionListBuilt = false;
    }

    @Override
    public State getExploreState() {
        return exploreState;
    }

    @Override
    public int getNumChoices() throws PrismException {
        return getTransitionList().getNumChoices();
    }

    @Override
    public int getNumTransitions() throws PrismException {
        return getTransitionList().getNumTransitions();
    }

    @Override
    public int getNumTransitions(final int index) throws PrismException {
        return getTransitionList().getChoice(index).size();
    }

    @Override
    public Object getTransitionAction(final int i) throws PrismException {
        getTransitionList();
        return actionNames.get(i);
    }

    @Override
    public Object getTransitionAction(final int i, final int offset) throws PrismException {
        return getTransitionAction(i);
    }

    @Override
    public double getTransitionProbability(final int index, final int offset) throws PrismException {
        final TransitionList transitions = getTransitionList();
        return transitions.getChoice(index).getProbability(offset);
    }

    @Override
    public State computeTransitionTarget(final int index, final int offset) throws PrismException {
        return getTransitionList().getChoice(index).computeTarget(offset, exploreState);
    }

    @Override
    public int getPlayerNumberForChoice(final int i) {
        return -1;
    }

    @Override
    public boolean isLabelTrue(final int i) throws PrismException {
        final Expression expr = labelList.getLabel(i);
        return expr.evaluateBoolean(exploreState);
    }

    @Override
    public VarList createVarList() {
        return varList;
    }

    /**
     * Returns the current list of available transitions, generating it first if
     * this has not yet been done.
     */
    private TransitionList getTransitionList() throws PrismException {
        if (!transitionListBuilt) {
            actionNames.clear();
            transitionList.clear();
            buildTransitionList();
            transitionListBuilt = true;
        }
        return transitionList;
    }

    void buildTransitionList() throws PrismException {
        final HashMap<String, ChoiceListFlexi> enabledGs = buildTransitionsExceptGlobal();
        for (final String act : enabledGs.keySet()) {
            actionNames.add(act);
            transitionList.add(enabledGs.get(act));
        }
    }

    HashMap<String, ChoiceListFlexi> buildTransitionsExceptGlobal() throws PrismException {
        final HashMap<String, ChoiceListFlexi> enabledGs = new HashMap<>();
        for (final Command command : environment.getCommands()) {
            if (command.getGuard().evaluateBoolean(exploreState)) {
                if (swarmFile.getActionTypes().getGs().contains(command.getSynch())) {
                    enabledGs.put(command.getSynch(), faultProvider.getChoice(command, exploreState, -2, 0));
                } else if (swarmFile.getActionTypes().getA().contains(command.getSynch())) {
                    actionNames.add("(" + command.getSynch() + ", E)");
                    transitionList.add(faultProvider.getChoice(command, exploreState, -1, 0));
                } else if (!swarmFile.getActionTypes().getAe().contains(command.getSynch())) {
                    throw new PrismException("Action " + command.getSynch() + " was not declared as A, AE or GS.");
                }
            }
        }
        for (int i = 0; i < renamedAgents.size(); ++i) {
            for (int j = 0; j < renamedAgents.get(i).size(); ++j) {
                final Module agent = renamedAgents.get(i).get(j);
                final HashSet<String> enabledForAgent = new HashSet<>();
                for (final Command command : agent.getCommands()) {
                    if (command.getGuard().evaluateBoolean(exploreState)) {
                        final String act = command.getSynch();
                        final ChoiceListFlexi choice = faultProvider.getChoice(command, exploreState, i, j);
                        if (swarmFile.getActionTypes().getA().contains(act)) {
                            actionNames.add("(" + act + ", (" + i + "," + j + "))");
                            transitionList.add(choice);
                        } else if (swarmFile.getActionTypes().getAe().contains(act)) {
                            final Command matchingEnv = getMatchingEnvironmentCommand(act);
                            if (matchingEnv != null) {
                                final ChoiceListFlexi envChoice = faultProvider.getChoice(matchingEnv, exploreState, -1, 0);
                                choice.productWith(envChoice);
                                actionNames.add("(" + act + ", (" + i + "," + j + "))");
                                transitionList.add(choice);
                            }
                        } else if (swarmFile.getActionTypes().getGs().contains(act)) {
                            enabledForAgent.add(act);
                            if (enabledGs.containsKey(act))
                                enabledGs.get(act).productWith(choice);
                        } else {
                            throw new PrismException("Action " + act + " was not declared as A, AE or GS.");
                        }
                    }
                }
                removedNotEnabled(enabledGs, enabledForAgent);
            }
        }
        return enabledGs;
    }

    protected static <T> void removedNotEnabled(
            final HashMap<String, T> enabledGs, final Set<String> localEnabledGs) {
        final List<String> toRemove = new LinkedList<>();
        for (final String act : enabledGs.keySet()) {
            if (!localEnabledGs.contains(act))
                toRemove.add(act);
        }
        for (final String act : toRemove)
            enabledGs.remove(act);
    }

    Command getMatchingEnvironmentCommand(final String actionName) throws PrismLangException {
        for (final Command command : environment.getCommands()) {
            if (command.getSynch().equals(actionName) && command.getGuard().evaluateBoolean(exploreState))
                return command;
        }
        return null;
    }

    @Override
    public boolean rewardStructHasTransitionRewards(final int i) {
        return false;
    }

    @Override
    public int getNumPlayers() {
        return 0;
    }

    @Override
    public Player getPlayer(final int i) {
        return null;
    }
}
