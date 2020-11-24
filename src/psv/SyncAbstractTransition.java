package psv;

import org.apache.commons.lang3.math.NumberUtils;
import parser.State;
import parser.ast.Agent;
import parser.ast.AgentUpdate;
import parser.ast.Update;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SyncAbstractTransition extends ChoiceListFlexi {
    private final Set<String> abstractActions;
    private final Set<String> actionSet;
    private final List<Agent> agents;

    SyncAbstractTransition(final ChoiceListFlexi choice, final Set<String> abstractActions, final List<Agent> agents, final Set<String> actionSet) {
        super(choice);
        this.abstractActions = abstractActions;
        this.actionSet = actionSet;
        this.agents = agents;
    }

    @Override
    public State computeTarget(final int i, final State currentState) throws PrismLangException {
        final State newGlobalState = new State(currentState);
        computeTarget(i, currentState, newGlobalState);
        return newGlobalState;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void computeTarget(final int i, final State currentState, final State newGlobalState) throws PrismLangException {
        super.computeTarget(i, currentState, newGlobalState);
        final Set<State> stateSet = (Set<State>) currentState.varValues[currentState.varValues.length - 1];
        newGlobalState.varValues[newGlobalState.varValues.length - 1] = new ActionSet<>(stateSet);
        final Set<State> newStateSet = (Set<State>) newGlobalState.varValues[newGlobalState.varValues.length - 1];
        for (final State state : stateSet) {
            for (final String action : abstractActions) {
                final ChoiceListFlexi choice = new ChoiceListFlexi();
                choice.add(1.0, new LinkedList<>());
                final Agent agent = agents.get(NumberUtils.createInteger(action.substring(action.lastIndexOf("_") + 1,
                        action.lastIndexOf(","))));
                for (final AgentUpdate update : agent.getUpdates()) {
                    if (update.isEnabled(state, actionSet, action)) {
                        final ChoiceListFlexi newChoice = new ChoiceListFlexi();
                        for (int j = 0; j < update.getUpdates().getNumUpdates(); ++j) {
                            final ArrayList<Update> list = new ArrayList<>();
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
                        final State newState = choice.computeTarget(j, state);
                        newStateSet.add(newState);
                    }
                }
            }
        }
    }
}
