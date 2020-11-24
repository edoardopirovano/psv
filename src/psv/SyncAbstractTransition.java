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
    List<List<String>> actionsPerType = new ArrayList<>();
    private final Set<String> actionSet;
    private final List<Agent> agents;

    SyncAbstractTransition(final ChoiceListFlexi choice, final Set<String> abstractActions, final List<Agent> agents, final Set<String> actionSet) {
        super(choice);
        this.actionSet = actionSet;
        this.agents = agents;
        for (final Agent ignored : agents)
            actionsPerType.add(new ArrayList<>());
        for (final String action : abstractActions) {
            final String strippedAbs = action.substring(0, action.lastIndexOf("_"));
            final int index = strippedAbs.lastIndexOf("_");
            actionsPerType.get(NumberUtils.createInteger(action.substring(index + 1, strippedAbs.length())))
                    .add(action);
        }
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
        for (int j = 0; j < actionsPerType.size(); ++j) {
            final Set<State> stateSet = (Set<State>) currentState.varValues[currentState.varValues.length - actionsPerType.size() + j];
            newGlobalState.varValues[newGlobalState.varValues.length - actionsPerType.size() + j] = new ActionSet<>(stateSet);
            final Set<State> newStateSet = (Set<State>) newGlobalState.varValues[newGlobalState.varValues.length - actionsPerType.size() + j];
            for (final State state : stateSet) {
                for (final String action : actionsPerType.get(j)) {
                    final ChoiceListFlexi choice = new ChoiceListFlexi();
                    choice.add(1.0, new LinkedList<>());
                    final Agent agent = agents.get(j);
                    for (final AgentUpdate update : agent.getUpdates()) {
                        if (update.isEnabled(state, actionSet, action)) {
                            final ChoiceListFlexi newChoice = new ChoiceListFlexi();
                            for (int k = 0; k < update.getUpdates().getNumUpdates(); ++k) {
                                final ArrayList<Update> list = new ArrayList<>();
                                list.add(update.getUpdates().getUpdate(k));
                                newChoice.add(update.getUpdates().getProbabilityInState(k, state), list);
                            }
                            if (newChoice.getProbabilitySum() != 1.0)
                                newChoice.add(1.0 - newChoice.getProbabilitySum(), new LinkedList<>());
                            choice.productWith(newChoice);
                        }
                    }
                    for (int k = 0; k < choice.size(); ++k) {
                        if (choice.getProbability(k) > 0) {
                            final State newState = choice.computeTarget(k, state);
                            newStateSet.add(newState);
                        }
                    }
                }
            }
        }
    }
}
