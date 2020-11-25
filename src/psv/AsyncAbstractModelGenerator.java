package psv;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Module;
import parser.ast.*;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.PrismException;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.*;

class AsyncAbstractModelGenerator extends AsyncConcreteModelGenerator {
    private final List<Module> abstractAgents = new ArrayList<>();
    private final List<VarList> abstractVarLists = new ArrayList<>();

    AsyncAbstractModelGenerator(final AsyncSwarmFile sf, final List<Integer> index) throws PrismException {
        super(sf, index);
        for (int i = 0; i < index.size(); ++i) {
            final VarList abstractVarList = new VarList();
            final RenamedModule rm = new RenamedModule("agent", "agent" + i + "Abs");
            for (final Declaration decl : swarmFile.getAgents().get(i).getDeclarations()) {
                final Declaration declarationCopy = (Declaration) decl.deepCopy();
                declarationCopy.setName(decl.getName() + "_" + i + "_abs");
                abstractVarList.addVar(declarationCopy, 0, new Values());
                rm.addRename(decl.getName(), declarationCopy.getName());
            }
            final Module agentCopy = (Module) swarmFile.getAgents().get(i).deepCopy();
            final Module abstractAgent = (Module) agentCopy.rename(rm);
            abstractAgent.accept(
                    new FindAllVars(getAbstractVarNames(abstractVarList), getAbstractVarTypes(abstractVarList)));
            abstractAgents.add(abstractAgent);
            abstractVarLists.add(abstractVarList);
        }
    }

    private static List<String> getAbstractVarNames(final VarList abstractVarList) {
        final List<String> varNames = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varNames.add(abstractVarList.getName(i));
        return varNames;
    }

    private static List<Type> getAbstractVarTypes(final VarList abstractVarList) {
        final List<Type> varTypes = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varTypes.add(abstractVarList.getType(i));
        return varTypes;
    }

    @Override
    public State getInitialState() throws PrismException {
        final List<Set<State>> abstractStates = new ArrayList<>();
        for (final VarList abstractVarList : abstractVarLists) {
            final State initialAbstractState = new State(abstractVarList.getNumVars());
            for (int j = 0; j < abstractVarList.getNumVars(); ++j)
                initialAbstractState.setValue(j, abstractVarList.getDeclaration(j).getStartOrDefault().evaluate());
            final LinkedHashSet<State> stateSet = new LinkedHashSet<>();
            stateSet.add(initialAbstractState);
            abstractStates.add(stateSet);
        }
        final State abstractState = new State(1);
        abstractState.setValue(0, abstractStates);
        return new State(super.getInitialState(), abstractState);
    }

    @Override
    void buildTransitionList() throws PrismException {
        final HashMap<String, AsyncAbstractTransition> enabledGs = new HashMap<>();
        for (final Map.Entry<String, ChoiceListFlexi> entry : buildTransitionsExceptGlobal().entrySet())
            enabledGs.put(entry.getKey(), new AsyncAbstractTransition(entry.getValue()));

        @SuppressWarnings("unchecked") final List<Set<State>> stateSets = (List<Set<State>>) getExploreState().varValues[getExploreState().varValues.length
                - 1];
        for (int i = 0; i < stateSets.size(); ++i) {
            final Set<State> stateSet = stateSets.get(i);
            final Module abstractAgent = abstractAgents.get(i);
            for (final State state : stateSet) {
                final Set<String> localEnabledGs = new LinkedHashSet<>();
                for (final Command command : abstractAgent.getCommands()) {
                    final String act = command.getSynch();
                    if (command.getGuard().evaluateBoolean(state)) {
                        if (swarmFile.getActionTypes().getGs().contains(act)) {
                            if (enabledGs.containsKey(act)) {
                                localEnabledGs.add(command.getSynch());
                                enabledGs.get(act).addUpdates(command.getUpdates());
                            }
                        } else if (swarmFile.getActionTypes().getA().contains(act)) {
                            final AsyncAbstractTransition grow = new AsyncAbstractTransition(state, i, true);
                            final AsyncAbstractTransition shrink = new AsyncAbstractTransition(state, i, false);
                            buildGrowAndShrinkTransitions(state, command, grow, shrink);
                            transitionList.add(grow);
                            actionNames.add("(" + act + ", " + i + ", " + state + ", up)");
                            transitionList.add(shrink);
                            actionNames.add("(" + act + ", " + i + ", " + state + ", down)");
                        } else if (swarmFile.getActionTypes().getAe().contains(act)) {
                            final AsyncAbstractTransition grow = new AsyncAbstractTransition(state, i, true);
                            final AsyncAbstractTransition shrink = new AsyncAbstractTransition(state, i, false);
                            buildGrowAndShrinkTransitions(state, command, grow, shrink);
                            final Command matchingEnv = getMatchingEnvironmentCommand(act);
                            if (matchingEnv != null) {
                                final ChoiceListFlexi envChoice = getChoice(matchingEnv, exploreState, -1, 0);
                                grow.productWith(envChoice);
                                shrink.productWith(envChoice);
                                transitionList.add(grow);
                                actionNames.add("(" + act + ", " + i + ", " + state + ", up)");
                                transitionList.add(shrink);
                                actionNames.add("(" + act + ", " + i + ", " + state + ", down)");
                            }
                        } else {
                            throw new PrismException(
                                    "Action " + command.getSynch() + " was not declared as A, AE or GS.");
                        }
                    }
                }
                removedNotEnabled(enabledGs, localEnabledGs);
            }
        }

        for (final String act : enabledGs.keySet()) {
            actionNames.add(act);
            transitionList.add(enabledGs.get(act));
        }
    }

    void buildGrowAndShrinkTransitions(final State state, final Command command,
                                       final AsyncAbstractTransition grow,
                                       final AsyncAbstractTransition shrink) throws PrismLangException {
        for (int j = 0; j < command.getUpdates().getNumUpdates(); ++j) {
            final ArrayList<Update> list = new ArrayList<>();
            list.add(command.getUpdates().getUpdate(j));
            grow.add(command.getUpdates().getProbabilityInState(j, state), list);
            shrink.add(command.getUpdates().getProbabilityInState(j, state), list);
        }
    }
}
