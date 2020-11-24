package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.List;

public class SyncSwarmFile extends ASTElement {
    private List<Agent> agents;
    private Agent environment;
    private LabelList labelList = new LabelList();

    public LabelList getLabelList() {
        return labelList;
    }

    public void setLabelList(final LabelList labelList) {
        this.labelList = labelList;
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(final List<Agent> agents) {
        this.agents = agents;
    }

    public Agent getEnvironment() {
        return environment;
    }

    public void setEnvironment(final Agent environment) {
        this.environment = environment;
    }

    @Override
    public Object accept(final ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        String s = "";
        for (final Agent agent : agents) {
            s += agent.toString() + "\n";
        }
        return s + environment.toString() + "\n" + labelList.toString();
    }

    @Override
    public ASTElement deepCopy() {
        final SyncSwarmFile other = new SyncSwarmFile();
        final ArrayList<Agent> newAgents = new ArrayList<>();
        for (final Agent agent : agents)
            newAgents.add((Agent) agent.deepCopy());
        other.setAgents(newAgents);
        other.setEnvironment((Agent) environment.deepCopy());
        other.setLabelList((LabelList) labelList.deepCopy());
        return other;
    }
}
