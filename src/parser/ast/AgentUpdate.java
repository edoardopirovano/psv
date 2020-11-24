package parser.ast;

import parser.State;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.Set;

public class AgentUpdate extends ASTElement {
    public static final int IN = 0;
    public static final int EQ = 1;
    public static final int NOTIN = 2;

    private ArrayList<String> actions = new ArrayList<>();
    private Updates updates;
    private Expression stateCondition;
    private String actionName;
    private int inclusionType = AgentUpdate.IN;

    public Expression getStateCondition() {
        return stateCondition;
    }

    public void setStateCondition(Expression stateCondition) {
        this.stateCondition = stateCondition;
    }

    public ArrayList<String> getActions() {
        return actions;
    }

    public Updates getUpdates() {
        return updates;
    }

    public String getActionName() {
        return actionName;
    }

    public void addAction(String a)
    {
        actions.add(a);
    }

    public void setUpdates(Updates u)
    {
        updates = u;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public void setActions(ArrayList<String> actions) {
        this.actions = actions;
    }

    public int getInclusionType() {
        return inclusionType;
    }

    public void setInclusionType(int inclusionType) {
        this.inclusionType = inclusionType;
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("(" + actionName + "," + stateCondition.toString() + ",");
        if (inclusionType == AgentUpdate.EQ)
            s.append("=");
        if (inclusionType == AgentUpdate.NOTIN)
            s.append("!");
        s.append("{");
        if (!actions.isEmpty()) {
            for (String action : actions)
                s.append(action).append(",");
            s.deleteCharAt(s.lastIndexOf(","));
        }
        s.append("}) -> ").append(updates).append(";");
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        AgentUpdate other = new AgentUpdate();
        for (String action : actions)
            other.addAction(action);
        other.setUpdates((Updates) updates.deepCopy());
        other.setStateCondition(stateCondition.deepCopy());
        other.setActionName(actionName);
        other.setInclusionType(inclusionType);
        return other;
    }

    public boolean isEnabled(State exploreState, Set<String> actionSet, String action) throws PrismLangException {
        if (!action.substring(0,action.lastIndexOf("_")).equals(actionName))
            return false;
        if (!stateCondition.evaluateBoolean(exploreState))
            return false;
        if (inclusionType == AgentUpdate.NOTIN) {
            for (String act : actions) {
                if (actionSet.contains(act))
                    return false;
            }
            return true;
        }
        for (String act : actions) {
            if (!actionSet.contains(act))
                return false;
        }
        if (inclusionType == AgentUpdate.EQ) {
            for (String act : actionSet) {
                if (!actions.contains(act))
                    return false;
            }
        }
        return true;
    }
}
