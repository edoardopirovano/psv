package psv;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import parser.State;
import parser.ast.Update;
import parser.ast.Updates;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

public class AsyncAbstractTransition extends ChoiceListFlexi {
	private State fromState;
	private int agentType;
	private boolean isGrow;
	private boolean isGlobal;
	private List<Updates> abstractUpdates = new ArrayList<>();

	AsyncAbstractTransition(State fromState, int agentType, boolean isGrow) {
		super();
		this.fromState = fromState;
		this.agentType = agentType;
		this.isGrow = isGrow;
		this.isGlobal = false;
	}

	AsyncAbstractTransition(ChoiceListFlexi value) {
		super(value);
		this.isGlobal = true;
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
		if (fromState != null) {
			State newLocalState = new State(fromState);
			for (Update up : updates.get(i)) {
				for (int j = 0; j < up.getNumElements(); ++j) {
					if (up.getVar(j).endsWith("_" + agentType + "_abs"))
						newLocalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(fromState));
				}
			}
			List<Set<State>> stateSets = (List<Set<State>>) newGlobalState.varValues[newGlobalState.varValues.length
					- 1];
			Set<State> newSet = new LinkedHashSet<>(stateSets.get(agentType));
			if (!isGrow)
				newSet.remove(fromState);
			newSet.add(newLocalState);
			for (Update up : updates.get(i)) {
				for (int j = 0; j < up.getNumElements(); ++j) {
					if (up.getVar(j).endsWith("_E"))
						newGlobalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(currentState));
				}
			}
			List<Set<State>> newStateSets = new ArrayList<>(stateSets);
			newStateSets.set(agentType, newSet);
			newGlobalState.setValue(newGlobalState.varValues.length - 1, newStateSets);
		} else {
			super.computeTarget(i, currentState, newGlobalState);
			if (isGlobal) {
				List<Set<State>> stateSets = (List<Set<State>>) newGlobalState.varValues[newGlobalState.varValues.length
						- 1];
				List<Set<State>> newStateSets = new ArrayList<>();
				int k = 0;
				for (Set<State> stateSet : stateSets) {
					Set<State> newStateSet = new LinkedHashSet<>();
					for (State state : stateSet) {
						State newLocalState = new State(state);
						if (abstractUpdates.get(k).getUpdates().size() > 1)
							throw new PrismLangException(
									"Global-synchronous transitions should be deterministic for agents!");
						for (Update up : abstractUpdates.get(k).getUpdates()) {
							for (int j = 0; j < up.getNumElements(); ++j) {
								if (up.getVar(j).endsWith("_abs"))
									newLocalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(state));
							}
						}
						++k;
						newStateSet.add(newLocalState);
					}
					newStateSets.add(newStateSet);
				}
				newGlobalState.setValue(newGlobalState.varValues.length - 1, newStateSets);
			}
		}
	}

	void addUpdates(Updates updates) {
		abstractUpdates.add(updates);
	}
}
