package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;

public class Agent extends ASTElement {
    // Local variables
    private ArrayList<Declaration> decls = new ArrayList<>();
    private ArrayList<Action> actions = new ArrayList<>();
    private ArrayList<AgentUpdate> updates = new ArrayList<>();

    public ArrayList<Declaration> getDecls() {
        return decls;
    }

    public ArrayList<Action> getActions() {
        return actions;
    }

    public ArrayList<AgentUpdate> getUpdates() {
        return updates;
    }

    public void setDecls(ArrayList<Declaration> decls) {
        this.decls = decls;
    }

    public void setActions(ArrayList<Action> actions) {
        this.actions = actions;
    }

    public void setUpdates(ArrayList<AgentUpdate> updates) {
        this.updates = updates;
    }

    public void addDeclaration(Declaration d)
    {
        decls.add(d);
    }

    public void addAction(Action a) {
        actions.add(a);
    }

    public void addUpdate(AgentUpdate au) {
        updates.add(au);
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("agent\n");
        for (Declaration declaration : decls)
            s.append(declaration).append("\n");
        for (Action action : actions)
            s.append(action).append("\n");
        s.append("update\n");
        for (AgentUpdate update : updates)
            s.append(update).append("\n");
        s.append("endupdate\n");
        s.append("endagent");
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        Agent other = new Agent();
        for (Declaration declaration : decls)
            other.addDeclaration((Declaration) declaration.deepCopy());
        for (Action action : actions)
            other.addAction((Action) action.deepCopy());
        for (AgentUpdate update : updates)
            other.addUpdate((AgentUpdate) update.deepCopy());
        return other;
    }
}
