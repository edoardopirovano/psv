package psv;

import parser.State;
import parser.ast.Update;
import parser.ast.Updates;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FaultyAbstractTransition extends ChoiceListFlexi {
    private State fromState;
    private boolean isGrow;
    private boolean isGlobal;
    private List<Updates> abstractUpdates = new ArrayList<>();

    FaultyAbstractTransition(State fromState, boolean isGrow) {
        super();
        this.fromState = fromState;
        this.isGrow = isGrow;
        this.isGlobal = false;
    }

    FaultyAbstractTransition(ChoiceListFlexi value) {
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
        newGlobalState.varValues[newGlobalState.varValues.length - 1] =
                copy((Set<State>)newGlobalState.varValues[newGlobalState.varValues.length - 1]);
        if (fromState != null) {
            State newLocalState = new State(fromState);
            for (Update up : updates.get(i)) {
                for (int j = 0; j < up.getNumElements(); ++j) {
                    if (up.getVar(j).endsWith("_abs"))
                        newLocalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(fromState));
                }
            }
            Set<State> stateSet = (Set<State>) newGlobalState.varValues[newGlobalState.varValues.length - 1];
            if (!isGrow)
                stateSet.remove(fromState);
            stateSet.add(newLocalState);
            for (Update up : updates.get(i)) {
                for (int j = 0; j < up.getNumElements(); ++j) {
                    if (up.getVar(j).endsWith("_E"))
                        newGlobalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(currentState));
                }
            }
        } else {
            super.computeTarget(i, currentState, newGlobalState);
            if (isGlobal) {
                Set<State> stateSet = (Set<State>) newGlobalState.varValues[newGlobalState.varValues.length - 1];
                Set<State> newStateSet = new LinkedHashSet<>();
                int k = 0;
                for (State state : stateSet) {
                    State newLocalState = new State(state);
                    if (abstractUpdates.get(k).getUpdates().size() > 1)
                        throw new PrismLangException("Global-synchronous transitions should be deterministic for agents!");
                    for (Update up : abstractUpdates.get(k).getUpdates()) {
                        for (int j = 0; j < up.getNumElements(); ++j) {
                            if (up.getVar(j).endsWith("_abs"))
                                newLocalState.setValue(up.getVarIndex(j), up.getExpression(j).evaluate(state));
                        }
                    }
                    ++k;
                    newStateSet.add(newLocalState);
                }
                newGlobalState.setValue(newGlobalState.varValues.length - 1, newStateSet);
            }
        }
    }

    private Set<State> copy(Set<State> stateSet) {
        Set<State> result = new LinkedHashSet<>();
        for (State state : stateSet)
            result.add(new State(state));
        return result;
    }

    void addUpdates(Updates updates) {
        abstractUpdates.add(updates);
    }
}
