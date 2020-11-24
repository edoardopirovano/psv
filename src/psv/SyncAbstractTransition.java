package psv;

import parser.State;
import parser.ast.Agent;
import parser.ast.AgentUpdate;
import parser.ast.Update;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

public class SyncAbstractTransition extends ChoiceListFlexi {
    private final Set<String> abstractActions;
    private final Set<String> actionSet;
    private final Agent agent;

    SyncAbstractTransition(ChoiceListFlexi choice, Set<String> abstractActions, Agent agent, Set<String> actionSet) {
        super(choice);
        this.abstractActions = abstractActions;
        this.actionSet = actionSet;
        this.agent = agent;
    }

    @Override
    public State computeTarget(int i, State currentState) throws PrismLangException {
        State newGlobalState = new State(currentState);
        computeTarget(i, currentState, newGlobalState);
        return newGlobalState;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void computeTarget(int i, State currentState, State newGlobalState) throws PrismLangException {
        super.computeTarget(i, currentState, newGlobalState);
        Set<State> stateSet = (Set<State>) currentState.varValues[currentState.varValues.length - 1];
        newGlobalState.varValues[newGlobalState.varValues.length - 1] = new ActionSet<>(stateSet);
        Set<State> newStateSet = (Set<State>) newGlobalState.varValues[newGlobalState.varValues.length - 1];
        for (State state : stateSet) {
            for (String action : abstractActions) {
                ChoiceListFlexi choice = new ChoiceListFlexi();
                choice.add(1.0, new LinkedList<>());
                for (AgentUpdate update : agent.getUpdates()) {
                    if (update.isEnabled(state, actionSet, action)) {
                        ChoiceListFlexi newChoice = new ChoiceListFlexi();
                        for (int j = 0; j < update.getUpdates().getNumUpdates(); ++j) {
                            ArrayList<Update> list = new ArrayList<>();
                            list.add(update.getUpdates().getUpdate(j));
                            newChoice.add(update.getUpdates().getProbabilityInState(j, state), list);
                        }
                        if (newChoice.getProbabilitySum() != 1.0)
                            newChoice.add(1.0 - newChoice.getProbabilitySum(), new LinkedList<>());
                        choice.productWith(newChoice);
                    }
                }
                for (int j = 0; j < choice.size(); ++j) {
                    if (choice.getProbability(j) > 0) {
                        State newState = choice.computeTarget(j, state);
                        newStateSet.add(newState);
                    }
                }
            }
        }
    }
}
