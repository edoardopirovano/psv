package psv;

import parser.ast.*;
import parser.visitor.ASTTraverse;
import prism.PrismLangException;

import java.util.HashMap;
import java.util.Map;

class IndexCalculator extends ASTTraverse {
    private int index = 1;

    private Map<String, Integer> labelValues = new HashMap<>();

    int getIndex() {
        return index;
    }

    @Override
    public void visitPre(ExpressionVar e) throws PrismLangException {
        visitVar(e.getName());
    }

    @Override
    public void visitPre(ExpressionIdent e) throws PrismLangException {
        visitVar(e.getName());
    }

    @Override
    public void visitPre(ExpressionLabel e) throws PrismLangException {
        index(labelValues.get(e.getName()));
    }

    @Override
    public void visitPre(ExpressionStrategy e) throws PrismLangException {
        for (String player : e.getCoalition().getPlayers()) {
            if (!player.equals("env"))
                index(Integer.valueOf(player.substring(player.lastIndexOf("_") + 1)));
        }
    }

    private void visitVar(String name) throws PrismLangException {
        String[] splitVarName = name.split("_");
        String agentName = splitVarName[splitVarName.length - 1];
        try {
            index(Integer.parseInt(agentName) + 1);
        } catch (NumberFormatException exception) {
            if (!agentName.equals("env"))
                throw new PrismLangException("Invalid identifier in formula");
        }
    }

    private void index(int index) {
        this.index = Math.max(this.index, index);
    }

    void endVisitLabel(String labelName) {
        labelValues.put(labelName, index);
        reset();
    }

    void reset() {
        index = 1;
    }
}
