package psv;

import parser.State;

public class SetChange {
    private State toAdd;
    private State toRemove;

    public SetChange(State toAdd, State toRemove) {
        this.toAdd = toAdd;
        this.toRemove = toRemove;
    }

    State getToAdd() {
        return toAdd;
    }

    State getToRemove() {
        return toRemove;
    }
}
