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

import java.util.*;

public class SyncAbstractModelGenerator extends SyncConcreteModelGenerator {
    private final Agent abstractAgent;
    private final VarList abstractVarList = new VarList();

    SyncAbstractModelGenerator(SyncSwarmFile sf, List<Integer> index) throws PrismException {
        super(sf, index);
        RenamedModule rm = new RenamedModule("agent", "agent_abs");
        for (Declaration decl : swarmFile.getAgent().getDecls()) {
            Declaration declarationCopy = (Declaration) decl.deepCopy();
            declarationCopy.setName(decl.getName() + "_abs");
            abstractVarList.addVar(declarationCopy, 0, new Values());
            rm.addRename(decl.getName(), declarationCopy.getName());
        }
        Agent agentCopy = (Agent) swarmFile.getAgent().deepCopy();
        abstractAgent = (Agent) agentCopy.rename(rm);
        abstractAgent.accept(new FindAllVars(getAbstractVarNames(), getAbstractVarTypes()));
    }

    @Override
    public State getInitialState() throws PrismException {
        State initialState = super.getInitialState();
        State initialAbstractState = new State(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            initialAbstractState.setValue(i, abstractVarList.getDeclaration(i).getStartOrDefault().evaluate());
        ActionSet<State> stateSet = new ActionSet<>();
        stateSet.add(initialAbstractState);
        State abstractState = new State(1);
        abstractState.setValue(0, stateSet);
        return new State(initialState, abstractState);
    }

    @Override
    void buildTransitionList() throws PrismException {
        @SuppressWarnings("unchecked")
        ActionSet<State> stateSet = (ActionSet<State>) getExploreState().varValues[getExploreState().varValues.length - 1];

        ActionSet<String> possibleActions = new ActionSet<>();
        for (State state : stateSet) {
            for (Action action : abstractAgent.getActions()) {
                if (action.getCondition().evaluateBoolean(state))
                    possibleActions.add(action.getName() + "_abs");
            }
        }
        ActionSet<Set<String>> actionSets = new ActionSet<>(Sets.powerSet(possibleActions));
        for (List<String> jointAction : getJointActions()) {
            for (Set<String> unsortedAbstract : actionSets) {
                ActionSet<String> abstractActions = new ActionSet<>(unsortedAbstract);
                StringBuilder actionString = new StringBuilder();
                for (String action : jointAction)
                    actionString.append("[").append(action).append("]");
                actionString.append("[").append(abstractActions).append("]");
                actionNames.add(actionString.toString());
                ActionSet<String> actionSet = new ActionSet<>();
                for (String action : jointAction)
                    actionSet.add(action.substring(0, action.lastIndexOf("_")));
                for (String action : abstractActions)
                    actionSet.add(action.substring(0, action.lastIndexOf("_")));
                ChoiceListFlexi choice = new ChoiceListFlexi();
                choice.add(1.0, new LinkedList<>());
                if ((exploreState.toString().equals("(true,0,[(false)])") & actionString.toString().equals("[receive_0][transmit_1][[transmit0_abs, block0_abs]]")))
                    System.out.println("Hello");
                addUpdates(jointAction.get(0), actionSet, choice, environment);
                int i = 1;
                for (Agent renamedAgent : renamedAgents)
                    addUpdates(jointAction.get(i++), actionSet, choice, renamedAgent);
                transitionList.add(new SyncAbstractTransition(choice, abstractActions, abstractAgent, actionSet));
            }
        }
    }

    private List<String> getAbstractVarNames() {
        List<String> varNames = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varNames.add(abstractVarList.getName(i));
        return varNames;
    }

    private List<Type> getAbstractVarTypes() {
        List<Type> varTypes = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varTypes.add(abstractVarList.getType(i));
        return varTypes;
    }

    @Override
    public int getNumPlayers() {
        return super.getNumPlayers() + 1;
    }

    @Override
    public Player getPlayer(int i) {
        if (i < getNumPlayers() - 1)
            return super.getPlayer(i);
        Player player = new Player("abstractAgent");
        ActionSet<String> actions = new ActionSet<>();
        for (Action action : abstractAgent.getActions())
            actions.add(action.getName() + "_abs");
        for (Set<String> actionSet : new ActionSet<>(Sets.powerSet(actions)))
            player.addAction(new ActionSet<>(actionSet).toString());
        return player;
    }
}
