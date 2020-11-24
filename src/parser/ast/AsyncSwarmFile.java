package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.List;

public class AsyncSwarmFile extends ASTElement {
    private ActionTypes actionTypes;
    private List<Module> agents;
    private Module environment;
    private LabelList labelList = new LabelList();

    public LabelList getLabelList() {
        return labelList;
    }

    public void setLabelList(LabelList labelList) {
        this.labelList = labelList;
    }

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        String s = "";
        s += actionTypes.toString();
        for (int i = 0; i < agents.size(); ++i) {
            s += "agent " + i + "\n";
            s += agents.get(i).toString();
        }
        s += "enviornment\n";
        s += environment.toString();
        s += "\n" + labelList.toString();
        return s;
    }

    @Override
    public ASTElement deepCopy() {
        AsyncSwarmFile copy = new AsyncSwarmFile();
        copy.setActionTypes((ActionTypes) actionTypes.deepCopy());
        ArrayList<Module> newAgents = new ArrayList<>();
        for (Module agent : agents)
            newAgents.add((Module) agent.deepCopy());
        copy.setAgents(newAgents);
        copy.setEnvironment((Module) environment.deepCopy());
        copy.setLabelList((LabelList) labelList.deepCopy());
        return copy;
    }

    public ActionTypes getActionTypes() {
        return actionTypes;
    }

    public void setActionTypes(ActionTypes actionTypes) {
        this.actionTypes = actionTypes;
    }

    public List<Module> getAgents() {
        return agents;
    }

    public void setAgents(List<Module> agents) {
        this.agents = agents;
    }

    public Module getEnvironment() {
        return environment;
    }

    public void setEnvironment(Module environment) {
        this.environment = environment;
    }
}
