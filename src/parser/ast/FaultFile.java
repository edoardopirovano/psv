package parser.ast;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

import java.util.ArrayList;

public class FaultFile extends ASTElement {
    private ArrayList<String> commands;
    private ArrayList<Expression> guards;
    private ArrayList<Updates> updates;

    @Override
    public Object accept(ASTVisitor v) throws PrismLangException {
        return v.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < commands.size(); ++i) {
            s.append("(");
            s.append(commands.get(i));
            s.append(", ");
            s.append(guards.get(i).toString());
            s.append(") -> ");
            s.append(updates.get(i).toString());
            s.append(";\n");
        }
        return s.toString();
    }

    @Override
    public ASTElement deepCopy() {
        FaultFile copy = new FaultFile();
        ArrayList<String> newCommands = new ArrayList<>();
        ArrayList<Expression> newGuards = new ArrayList<>();
        ArrayList<Updates> newUpdates = new ArrayList<>();
        for (int i = 0; i < commands.size(); ++i) {
            newCommands.add(commands.get(i));
            newGuards.add(guards.get(i).deepCopy());
            newUpdates.add((Updates) updates.get(i).deepCopy());
        }
        copy.setCommands(newCommands);
        copy.setGuards(newGuards);
        copy.setUpdates(newUpdates);
        return copy;
    }

    public ArrayList<String> getCommands() {
        return commands;
    }

    public void setCommands(ArrayList<String> commands) {
        this.commands = commands;
    }

    public ArrayList<Expression> getGuards() {
        return guards;
    }

    public void setGuards(ArrayList<Expression> guards) {
        this.guards = guards;
    }

    public ArrayList<Updates> getUpdates() {
        return updates;
    }

    public void setUpdates(ArrayList<Updates> updates) {
        this.updates = updates;
    }
}
