package psv;

import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionVar;
import parser.visitor.ASTTraverse;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IndexCalculator extends ASTTraverse {
    private List<Integer> index = new ArrayList<>();

    private final Map<String, List<Integer>> labelValues = new HashMap<>();

    List<Integer> getIndex() {
        return index;
    }

    @Override
    public void visitPre(final ExpressionVar e) throws PrismLangException {
        visitVar(e.getName());
    }

    @Override
    public void visitPre(final ExpressionIdent e) throws PrismLangException {
        visitVar(e.getName());
    }

    @Override
    public void visitPre(final ExpressionStrategy e) throws PrismLangException {
        for (final String player : e.getCoalitionPlayers())
            visitVar(player);
    }

    @Override
    public void visitPre(final ExpressionLabel e) throws PrismLangException {
        final List<Integer> labelIndex = labelValues.get(e.getName());
        for (int i = 0; i < labelIndex.size(); ++i)
            index(i + 1, labelIndex.get(i));
    }

    private void visitVar(final String name) throws PrismLangException {
        if (name.equals("env"))
            return;
        final String[] splitVarName = name.split("_");
        final String agentName = splitVarName[splitVarName.length - 1];
        if (agentName.equals("E"))
            return;
        final String agentType = splitVarName[splitVarName.length - 2];
        try {
            index(Integer.parseInt(agentType), Integer.parseInt(agentName));
        } catch (final NumberFormatException exception) {
            throw new PrismLangException("Invalid identifier in formula");
        }
    }

    private void index(final int i, final int j) {
        fillTo(i);
        index.set(i - 1, Math.max(index.get(i - 1), j));
    }

    void endVisitLabel(final String labelName) {
        labelValues.put(labelName, index);
        reset();
    }

    void reset() {
        index = new ArrayList<>();
    }

    void fillTo(final int i) {
        while (index.size() < i)
            index.add(0);
    }
}
