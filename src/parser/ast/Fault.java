package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class Fault extends ASTElement {
    private String command;
    private Expression guard;
    private Updates updates;

    @Override
    public Object accept(final ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append("(");
        s.append(command);
        s.append(", ");
        s.append(guard.toString());
        s.append(") -> ");
        s.append(updates.toString());
        s.append(";\n");
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        final Fault copy = new Fault();
        copy.setCommand(command);
        copy.setGuard(guard.deepCopy());
        copy.setUpdates((Updates) updates.deepCopy());
        return copy;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public Expression getGuard() {
        return guard;
    }

    public void setGuard(final Expression guard) {
        this.guard = guard;
    }

    public Updates getUpdates() {
        return updates;
    }

    public void setUpdates(final Updates updates) {
        this.updates = updates;
    }
}
