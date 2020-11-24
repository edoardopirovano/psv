package psv;

import com.google.common.collect.Sets;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.PrismException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SyncAbstractModelGenerator extends SyncConcreteModelGenerator {
    private final List<Agent> abstractAgents = new ArrayList<>();
    private final List<VarList> abstractVarLists = new ArrayList<>();

    SyncAbstractModelGenerator(final SyncSwarmFile sf, final List<Integer> index) throws PrismException {
        super(sf, index);
        for (int i = 0; i < index.size(); ++i) {
            final RenamedModule rm = new RenamedModule("agent", "agent_abs");
            final VarList abstractVarList = new VarList();
            for (final Declaration decl : swarmFile.getAgents().get(i).getDecls()) {
                final Declaration declarationCopy = (Declaration) decl.deepCopy();
                declarationCopy.setName(decl.getName() + "_abs");
                abstractVarList.addVar(declarationCopy, 0, new Values());
                rm.addRename(decl.getName(), declarationCopy.getName());
            }
            final Agent agentCopy = (Agent) swarmFile.getAgents().get(i).deepCopy();
            final Agent abstractAgent = (Agent) agentCopy.rename(rm);
            abstractAgent.accept(new FindAllVars(getAbstractVarNames(abstractVarList), getAbstractVarTypes(abstractVarList)));
            abstractAgents.add(abstractAgent);
            abstractVarLists.add(abstractVarList);
        }
    }

    @Override
    public State getInitialState() throws PrismException {
        final State initialState = super.getInitialState();
        final State abstractState = new State(abstractAgents.size());
        for (int i = 0; i < abstractAgents.size(); ++i) {
            final State initialAbstractState = new State(abstractVarLists.get(i).getNumVars());
            for (int j = 0; j < abstractVarLists.get(i).getNumVars(); ++j)
                initialAbstractState.setValue(j, abstractVarLists.get(i).getDeclaration(j).getStartOrDefault().evaluate());
            final ActionSet<State> initialSet = new ActionSet<>();
            initialSet.add(initialAbstractState);
            abstractState.setValue(i, initialSet);
        }
        return new State(initialState, abstractState);
    }

    @Override
    @SuppressWarnings("unchecked")
    void buildTransitionList() throws PrismException {
        final ActionSet<String> possibleActions = new ActionSet<>();
        for (int i = 0; i < abstractAgents.size(); ++i) {
            final ActionSet<State> stateSet = (ActionSet<State>) getExploreState().varValues[getExploreState().varValues.length - abstractAgents.size() + i];
            for (final State state : stateSet) {
                for (final Action action : abstractAgents.get(i).getActions()) {
                    if (action.getCondition().evaluateBoolean(state))
                        possibleActions.add(action.getName() + "_" + i + "_abs");
                }
            }
        }
        final ActionSet<Set<String>> actionSets = new ActionSet<>(Sets.powerSet(possibleActions));
        for (final List<String> jointAction : getJointActions()) {
            for (final Set<String> unsortedAbstract : actionSets) {
                final ActionSet<String> abstractActions = new ActionSet<>(unsortedAbstract);
                final StringBuilder actionString = new StringBuilder();
                for (final String action : jointAction)
                    actionString.append("[").append(action).append("]");
                actionString.append("[").append(abstractActions).append("]");
                actionNames.add(actionString.toString());
                final ActionSet<String> actionSet = new ActionSet<>();
                for (final String action : jointAction)
                    actionSet.add(AgentUpdate.stripIdentifier(action));
                for (final String action : abstractActions)
                    actionSet.add(AgentUpdate.stripIdentifier(action));
                final ChoiceListFlexi choice = new ChoiceListFlexi();
                choice.add(1.0, new LinkedList<>());
                addUpdates(jointAction.get(0), actionSet, choice, environment);
                int i = 1;
                for (final List<Agent> agents : renamedAgents) {
                    for (final Agent agent : agents)
                        addUpdates(jointAction.get(i++), actionSet, choice, agent);
                }
                transitionList.add(new SyncAbstractTransition(choice, abstractActions, abstractAgents, actionSet));
            }
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
    public int getNumPlayers() {
        return super.getNumPlayers() + abstractAgents.size();
    }

    @Override
    public Player getPlayer(final int i) {
        if (i < super.getNumPlayers())
            return super.getPlayer(i);
        final int abs = i - super.getNumPlayers();
        final Player player = new Player("abstractAgent_" + abs);
        final Agent abstractAgent = abstractAgents.get(abs);
        final ActionSet<String> actions = new ActionSet<>();
        for (final Action action : abstractAgent.getActions())
            actions.add(action.getName() + "_" + abs + "_abs");
        for (final Set<String> actionSet : new ActionSet<>(Sets.powerSet(actions)))
            player.addAction(new ActionSet<>(actionSet).toString());
        return player;
    }
}
