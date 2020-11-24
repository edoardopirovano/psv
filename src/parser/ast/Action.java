package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class Action extends ASTElement {
    private String name;
    private Expression condition;

    public String getName() {
        return name;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Action(String name, Expression condition) {
        this.name = name;
        this.condition = condition;
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        return "[" + name + "] " + condition.toString() + ";";
    }

    @Override
    public ASTElement deepCopy() {
        return new Action(name, condition.deepCopy());
    }
}
