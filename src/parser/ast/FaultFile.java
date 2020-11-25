package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.List;

public class FaultFile extends ASTElement {
    private List<List<Fault>> faults;

    @Override
    public Object accept(final ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        for (final List<Fault> faultList : faults) {
            s.append("agent\n");
            for (final Fault fault : faultList)
                s.append(fault.toString());
        }
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        final FaultFile copy = new FaultFile();
        final List<List<Fault>> newFaults = new ArrayList<>(faults.size());
        for (final List<Fault> faultList : faults) {
            final List<Fault> faultListCopy = new ArrayList<>(faultList.size());
            for (final Fault fault : faultList)
                faultListCopy.add((Fault) fault.deepCopy());
            newFaults.add(faultListCopy);
        }
        copy.setFaults(newFaults);
        return copy;
    }

    public List<List<Fault>> getFaults() {
        return faults;
    }

    public void setFaults(final List<List<Fault>> faults) {
        this.faults = faults;
    }
}
