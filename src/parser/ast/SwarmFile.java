package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;

public class SwarmFile extends ASTElement {
    private Agent agent;
    private Agent environment;
    private LabelList labelList = new LabelList();

    public LabelList getLabelList() {
        return labelList;
    }

    public void setLabelList(LabelList labelList) {
        this.labelList = labelList;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Agent getEnvironment() {
        return environment;
    }

    public void setEnvironment(Agent environment) {
        this.environment = environment;
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        return agent.toString() + "\n" + environment.toString() + "\n" + labelList.toString();
    }

    @Override
    public ASTElement deepCopy() {
        SwarmFile other = new SwarmFile();
        other.setAgent((Agent) agent.deepCopy());
        other.setEnvironment((Agent) environment.deepCopy());
        other.setLabelList((LabelList) labelList.deepCopy());
        return other;
    }
}
