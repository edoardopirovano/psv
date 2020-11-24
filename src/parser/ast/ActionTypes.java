package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.HashSet;

public class ActionTypes extends ASTElement {
    private HashSet<String> a;
    private HashSet<String> ae;
    private HashSet<String> gs;

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("asynchronous = {");
        for (String act : a)
            s.append(act).append(", ");
        if (!a.isEmpty())
            s.delete(s.length() - 2, s.length());
        s.append("}\n");
        s.append("agentEnvironment = {");
        for (String act : ae)
            s.append(act).append(", ");
        if (!ae.isEmpty())
            s.delete(s.length() - 2, s.length());
        s.append("}\n");
        s.append("globalSynchronous = {");
        for (String act : gs)
            s.append(act).append(", ");
        if (!gs.isEmpty())
            s.delete(s.length() - 2, s.length());
        s.append("}\n");
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        ActionTypes copy = new ActionTypes();
        copy.setA((HashSet<String>) a.clone());
        copy.setAe((HashSet<String>) ae.clone());
        copy.setGs((HashSet<String>) gs.clone());
        return copy;
    }

    public HashSet<String> getA() {
        return a;
    }

    public void setA(HashSet<String> a) {
        this.a = a;
    }

    public HashSet<String> getAe() {
        return ae;
    }

    public void setAe(HashSet<String> ae) {
        this.ae = ae;
    }

    public HashSet<String> getGs() {
        return gs;
    }

    public void setGs(HashSet<String> gs) {
        this.gs = gs;
    }
}
