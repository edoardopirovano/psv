package psv;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import simulator.CSGModelGenerator;
import simulator.ChoiceListFlexi;
import simulator.TransitionList;

import java.util.*;

public class SyncConcreteModelGenerator extends DefaultModelGenerator implements CSGModelGenerator {
    SyncSwarmFile swarmFile;

    private final VarList varList = new VarList();
    private final LabelList labelList;
    private final List<String> labelNames;
    List<Agent> renamedAgents;
    Agent environment;

    // Model exploration info

    // State currently being explored
    protected State exploreState;

    // List of currently available transitions
    List<String> actionNames = new ArrayList<>();
    TransitionList transitionList = new TransitionList();

    // Has the transition list been built?
    private boolean transitionListBuilt;

    SyncConcreteModelGenerator(final SyncSwarmFile swarmFile, final List<Integer> numAgents) throws PrismException {
        this.swarmFile = swarmFile;
        final Values constantValues = new Values();
        final int n = numAgents.get(0);
        renamedAgents = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            final RenamedModule rm = new RenamedModule("agent", "agent_" + i);
            for (final Declaration decl : swarmFile.getAgent().getDecls()) {
                final Declaration declarationCopy = (Declaration) decl.deepCopy();
                declarationCopy.setName(decl.getName() + "_" + i);
                varList.addVar(declarationCopy, 0, constantValues);
                rm.addRename(decl.getName(), declarationCopy.getName());
            }
            Agent agentCopy = (Agent) swarmFile.getAgent().deepCopy();
            agentCopy = (Agent) agentCopy.rename(rm);
            renamedAgents.add(agentCopy);
        }
        final RenamedModule rm = new RenamedModule("environment", "env");
        for (final Declaration decl : swarmFile.getEnvironment().getDecls()) {
            final Declaration declarationCopy = (Declaration) decl.deepCopy();
            declarationCopy.setName(decl.getName() + "_env");
            varList.addVar(declarationCopy, 0, constantValues);
            rm.addRename(decl.getName(), declarationCopy.getName());
        }
        environment = (Agent) swarmFile.getEnvironment().deepCopy();
        environment = (Agent) environment.rename(rm);
        labelList = (LabelList) swarmFile.getLabelList().deepCopy();
        labelNames = labelList.getLabelNames();

        final FindAllVars replacer = new FindAllVars(getVarNames(), getVarTypes());
        for (final Agent agent : renamedAgents)
            agent.accept(replacer);
        environment.accept(replacer);
        labelList.accept(replacer);

        transitionList = new TransitionList();
        transitionListBuilt = false;
    }

    @Override
    public ModelType getModelType() {
        return ModelType.CSG;
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
    public int getPlayerNumberForChoice(final int i) throws PrismException {
        return 1;
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
     * Returns the current list of available transitions, generating it first if this has not yet been done.
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
        for (final List<String> jointAction : getJointActions()) {
            final StringBuilder actionString = new StringBuilder();
            for (final String action : jointAction)
                actionString.append("[").append(action).append("]");
            actionNames.add(actionString.toString());
            final ActionSet<String> actionSet = new ActionSet<>();
            for (final String action : jointAction)
                actionSet.add(action.substring(0, action.lastIndexOf("_")));
            final ChoiceListFlexi choice = new ChoiceListFlexi();
            choice.add(1.0, new LinkedList<>());
            addUpdates(jointAction.get(0), actionSet, choice, environment);
            int i = 1;
            for (final Agent renamedAgent : renamedAgents)
                addUpdates(jointAction.get(i++), actionSet, choice, renamedAgent);
            transitionList.add(choice);
        }
    }

    List<List<String>> getJointActions() throws PrismLangException {
        List<List<String>> jointActions = new LinkedList<>();
        for (final Action action : environment.getActions()) {
            if (action.getCondition().evaluateBoolean(exploreState)) {
                final LinkedList<String> actions = new LinkedList<>();
                actions.add(action.getName() + "_0");
                jointActions.add(actions);
            }
        }
        for (int i = 1; i <= renamedAgents.size(); ++i) {
            final List<List<String>> newJointActions = new LinkedList<>();
            for (final Action action : renamedAgents.get(i - 1).getActions()) {
                if (action.getCondition().evaluateBoolean(exploreState)) {
                    for (final List<String> jointAction : jointActions) {
                        final LinkedList<String> newJointAction = new LinkedList<>(jointAction);
                        newJointAction.add(action.getName() + "_" + i);
                        newJointActions.add(newJointAction);
                    }
                }
            }
            jointActions = newJointActions;
        }
        return jointActions;
    }

    void addUpdates(final String action, final Set<String> actionSet, final ChoiceListFlexi choice, final Agent agent) throws PrismLangException {
        for (final AgentUpdate update : agent.getUpdates()) {
            if (update.isEnabled(exploreState, actionSet, action)) {
                final ChoiceListFlexi newChoice = new ChoiceListFlexi();
                for (int i = 0; i < update.getUpdates().getNumUpdates(); ++i) {
                    final ArrayList<Update> list = new ArrayList<>();
                    list.add(update.getUpdates().getUpdate(i));
                    newChoice.add(update.getUpdates().getProbabilityInState(i, exploreState), list);
                }
                if (newChoice.getProbabilitySum() != 1.0)
                    newChoice.add(1.0 - newChoice.getProbabilitySum(), new LinkedList<>());
                choice.productWith(newChoice);
            }
        }
    }

    @Override
    public boolean rewardStructHasTransitionRewards(final int i) {
        return false;
    }

    @Override
    public int getNumPlayers() {
        return renamedAgents.size() + 1;
    }

    @Override
    public Player getPlayer(final int i) {
        final Player player = new Player(i == 0 ? "env" : "agent_" + i);
        final Agent agent = (i == 0 ? environment : renamedAgents.get(i - 1));
        for (final Action action : agent.getActions())
            player.addAction(action.getName() + "_" + i);
        return player;
    }

    @Override
    public Map<Integer, BitSet> getSynchMap() {
        return new HashMap<>();
    }
}
