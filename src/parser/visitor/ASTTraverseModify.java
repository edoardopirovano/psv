//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package parser.visitor;

import parser.ast.Module;
import parser.ast.*;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.List;

// Variant of ASTTraverse.
// Performs a depth-first traversal of an asbtract syntax tree (AST),
// replacing each child node with the object returned by the recursive visit call.
// Like ASTTraverse, many traversal-based tasks can be implemented by extending and either:
// (a) overriding defaultVisitPre or defaultVisitPost
// (b) overiding visit for leaf (or other selected) nodes

public class ASTTraverseModify implements ASTVisitor {
    public void defaultVisitPre(final ASTElement e) throws PrismLangException {
    }

    public void defaultVisitPost(final ASTElement e) throws PrismLangException {
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ModulesFile e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ModulesFile e) throws PrismLangException {
        visitPre(e);
        int i, n;
        if (e.getFormulaList() != null) e.setFormulaList((FormulaList) (e.getFormulaList().accept(this)));
        if (e.getLabelList() != null) e.setLabelList((LabelList) (e.getLabelList().accept(this)));
        if (e.getConstantList() != null) e.setConstantList((ConstantList) (e.getConstantList().accept(this)));
        n = e.getNumGlobals();
        for (i = 0; i < n; i++) {
            if (e.getGlobal(i) != null) e.setGlobal(i, (Declaration) (e.getGlobal(i).accept(this)));
        }
        n = e.getNumModules();
        for (i = 0; i < n; i++) {
            if (e.getModule(i) != null) e.setModule(i, (parser.ast.Module) (e.getModule(i).accept(this)));
        }
        n = e.getNumSystemDefns();
        for (i = 0; i < n; i++) {
            if (e.getSystemDefn(i) != null)
                e.setSystemDefn(i, (SystemDefn) (e.getSystemDefn(i).accept(this)), e.getSystemDefnName(i));
        }
        n = e.getNumRewardStructs();
        for (i = 0; i < n; i++) {
            if (e.getRewardStruct(i) != null) e.setRewardStruct(i, (RewardStruct) (e.getRewardStruct(i).accept(this)));
        }
        if (e.getInitialStates() != null) e.setInitialStates((Expression) (e.getInitialStates().accept(this)));
        n = e.getNumPlayers();
        for (i = 0; i < n; i++) {
            if (e.getPlayer(i) != null) e.setPlayer(i, (Player) (e.getPlayer(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final ModulesFile e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final PropertiesFile e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final PropertiesFile e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		if (e.getLabelList() != null) e.setLabelList((LabelList) (e.getLabelList().accept(this)));
        if (e.getConstantList() != null) e.setConstantList((ConstantList) (e.getConstantList().accept(this)));
        n = e.getNumProperties();
        for (i = 0; i < n; i++) {
            if (e.getPropertyObject(i) != null)
                e.setPropertyObject(i, (Property) (e.getPropertyObject(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final PropertiesFile e) throws PrismLangException {
        defaultVisitPost(e);
    }
    // -----------------------------------------------------------------------------------

    public void visitPre(final Property e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Property e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression() != null)
            e.setExpression((Expression) e.getExpression().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final Property e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final FormulaList e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final FormulaList e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.size();
        for (i = 0; i < n; i++) {
            if (e.getFormula(i) != null) e.setFormula(i, (Expression) (e.getFormula(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final FormulaList e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final LabelList e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final LabelList e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.size();
        for (i = 0; i < n; i++) {
            if (e.getLabel(i) != null) e.setLabel(i, (Expression) (e.getLabel(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final LabelList e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ConstantList e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ConstantList e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.size();
        for (i = 0; i < n; i++) {
            if (e.getConstant(i) != null) e.setConstant(i, (Expression) (e.getConstant(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final ConstantList e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Declaration e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Declaration e) throws PrismLangException {
        visitPre(e);
        if (e.getDeclType() != null) e.setDeclType((DeclarationType) e.getDeclType().accept(this));
        if (e.getStart() != null) e.setStart((Expression) e.getStart().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final Declaration e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final DeclarationInt e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final DeclarationInt e) throws PrismLangException {
        visitPre(e);
        if (e.getLow() != null) e.setLow((Expression) e.getLow().accept(this));
        if (e.getHigh() != null) e.setHigh((Expression) e.getHigh().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final DeclarationInt e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final DeclarationBool e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final DeclarationBool e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final DeclarationBool e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final DeclarationArray e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final DeclarationArray e) throws PrismLangException {
        visitPre(e);
        if (e.getLow() != null) e.setLow((Expression) e.getLow().accept(this));
        if (e.getHigh() != null) e.setHigh((Expression) e.getHigh().accept(this));
        if (e.getSubtype() != null) e.setSubtype((DeclarationType) e.getSubtype().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final DeclarationArray e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final DeclarationClock e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final DeclarationClock e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final DeclarationClock e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final DeclarationIntUnbounded e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final DeclarationIntUnbounded e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final DeclarationIntUnbounded e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final parser.ast.Module e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final parser.ast.Module e) throws PrismLangException {
        visitPre(e);
        int i, n;
        n = e.getNumDeclarations();
        for (i = 0; i < n; i++) {
            if (e.getDeclaration(i) != null) e.setDeclaration(i, (Declaration) (e.getDeclaration(i).accept(this)));
        }
        if (e.getInvariant() != null)
            e.setInvariant((Expression) (e.getInvariant().accept(this)));
        n = e.getNumCommands();
        for (i = 0; i < n; i++) {
            if (e.getCommand(i) != null) e.setCommand(i, (Command) (e.getCommand(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final parser.ast.Module e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Command e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Command e) throws PrismLangException {
        visitPre(e);
        e.setGuard((Expression) (e.getGuard().accept(this)));
        e.setUpdates((Updates) (e.getUpdates().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final Command e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SCommand e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SCommand e) throws PrismLangException {
        visitPre(e);
        e.setGuard((Expression) (e.getGuard().accept(this)));
        e.setUpdates((Updates) (e.getUpdates().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final SCommand e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Updates e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Updates e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.getNumUpdates();
        for (i = 0; i < n; i++) {
            if (e.getProbability(i) != null) e.setProbability(i, (Expression) (e.getProbability(i).accept(this)));
            if (e.getUpdate(i) != null) e.setUpdate(i, (Update) (e.getUpdate(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final Updates e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Update e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Update e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.getNumElements();
        for (i = 0; i < n; i++) {
            if (e.getExpression(i) != null) e.setExpression(i, (Expression) (e.getExpression(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final Update e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final RenamedModule e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final RenamedModule e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final RenamedModule e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final RewardStruct e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final RewardStruct e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n;
		n = e.getNumItems();
        for (i = 0; i < n; i++) {
            if (e.getRewardStructItem(i) != null)
                e.setRewardStructItem(i, (RewardStructItem) (e.getRewardStructItem(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final RewardStruct e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final RewardStructItem e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final RewardStructItem e) throws PrismLangException {
        visitPre(e);
        e.setStates((Expression) (e.getStates().accept(this)));
        e.setReward((Expression) (e.getReward().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final RewardStructItem e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Player e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Player e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final Player e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemInterleaved e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemInterleaved e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n = e.getNumOperands();
		for (i = 0; i < n; i++) {
            if (e.getOperand(i) != null) e.setOperand(i, (SystemDefn) (e.getOperand(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemInterleaved e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemFullParallel e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemFullParallel e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n = e.getNumOperands();
		for (i = 0; i < n; i++) {
            if (e.getOperand(i) != null) e.setOperand(i, (SystemDefn) (e.getOperand(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemFullParallel e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemParallel e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemParallel e) throws PrismLangException {
        visitPre(e);
        e.setOperand1((SystemDefn) (e.getOperand1().accept(this)));
        e.setOperand2((SystemDefn) (e.getOperand2().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemParallel e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemHide e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemHide e) throws PrismLangException {
        visitPre(e);
        e.setOperand((SystemDefn) (e.getOperand().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemHide e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemRename e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemRename e) throws PrismLangException {
        visitPre(e);
        e.setOperand((SystemDefn) (e.getOperand().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemRename e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemModule e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemModule e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemModule e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemBrackets e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemBrackets e) throws PrismLangException {
        visitPre(e);
        e.setOperand((SystemDefn) (e.getOperand().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemBrackets e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SystemReference e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final SystemReference e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final SystemReference e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionTemporal e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionTemporal e) throws PrismLangException {
        visitPre(e);
        if (e.getOperand1() != null) e.setOperand1((Expression) (e.getOperand1().accept(this)));
        if (e.getOperand2() != null) e.setOperand2((Expression) (e.getOperand2().accept(this)));
        if (e.getLowerBound() != null)
            e.setLowerBound((Expression) (e.getLowerBound().accept(this)), e.lowerBoundIsStrict());
        if (e.getUpperBound() != null)
            e.setUpperBound((Expression) (e.getUpperBound().accept(this)), e.upperBoundIsStrict());
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionTemporal e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionITE e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionITE e) throws PrismLangException {
        visitPre(e);
        e.setOperand1((Expression) (e.getOperand1().accept(this)));
        e.setOperand2((Expression) (e.getOperand2().accept(this)));
        e.setOperand3((Expression) (e.getOperand3().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionITE e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionBinaryOp e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionBinaryOp e) throws PrismLangException {
        visitPre(e);
        e.setOperand1((Expression) (e.getOperand1().accept(this)));
        e.setOperand2((Expression) (e.getOperand2().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionBinaryOp e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionUnaryOp e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionUnaryOp e) throws PrismLangException {
        visitPre(e);
        e.setOperand((Expression) (e.getOperand().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionUnaryOp e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionFunc e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionFunc e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n = e.getNumOperands();
		for (i = 0; i < n; i++) {
            if (e.getOperand(i) != null) e.setOperand(i, (Expression) (e.getOperand(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionFunc e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionIdent e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionIdent e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionIdent e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionLiteral e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionLiteral e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionLiteral e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionConstant e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionConstant e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionConstant e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionFormula e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionFormula e) throws PrismLangException {
        visitPre(e);
        if (e.getDefinition() != null) e.setDefinition((Expression) (e.getDefinition().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionFormula e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionVar e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionVar e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionVar e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionProb e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionProb e) throws PrismLangException {
        visitPre(e);
        if (e.getProb() != null) e.setProb((Expression) (e.getProb().accept(this)));
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        if (e.getFilter() != null) e.setFilter((Filter) (e.getFilter().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionProb e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionReward e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionReward e) throws PrismLangException {
        visitPre(e);
        if (e.getRewardStructIndex() != null && e.getRewardStructIndex() instanceof Expression)
            e.setRewardStructIndex((Expression) (((Expression) e.getRewardStructIndex()).accept(this)));
        if (e.getRewardStructIndexDiv() != null && e.getRewardStructIndexDiv() instanceof Expression)
            e.setRewardStructIndexDiv((Expression) (((Expression) e.getRewardStructIndexDiv()).accept(this)));
        if (e.getReward() != null) e.setReward((Expression) (e.getReward().accept(this)));
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        if (e.getFilter() != null) e.setFilter((Filter) (e.getFilter().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionReward e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionNash e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionNash e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression() != null)
            e.setExpression((Expression) (((Expression) e.getExpression()).accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionNash e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionNashProb e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionNashProb e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression1() != null)
            e.setExpression1((Expression) (((Expression) e.getExpression1()).accept(this)));
        if (e.getExpression2() != null)
            e.setExpression2((Expression) (((Expression) e.getExpression2()).accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionNashProb e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionNashReward e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionNashReward e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression1() != null)
            e.setExpression1((Expression) (((Expression) e.getExpression1()).accept(this)));
        if (e.getExpression2() != null)
            e.setExpression2((Expression) (((Expression) e.getExpression2()).accept(this)));
        if (e.getRewardStructIndex1() != null && e.getRewardStructIndex1() instanceof Expression)
            e.setRewardStructIndex1((Expression) (((Expression) e.getRewardStructIndex1()).accept(this)));
        if (e.getRewardStructIndex2() != null && e.getRewardStructIndex2() instanceof Expression)
            e.setRewardStructIndex2((Expression) (((Expression) e.getRewardStructIndex2()).accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionNashReward e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionSS e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionSS e) throws PrismLangException {
        visitPre(e);
        if (e.getProb() != null) e.setProb((Expression) (e.getProb().accept(this)));
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        if (e.getFilter() != null) e.setFilter((Filter) (e.getFilter().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionSS e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionExists e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionExists e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionExists e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionForAll e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionForAll e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionForAll e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionStrategy e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionStrategy e) throws PrismLangException {
        visitPre(e);
        int i;
		final int n = e.getNumOperands();
		for (i = 0; i < n; i++) {
            if (e.getOperand(i) != null) e.setOperand(i, (Expression) (e.getOperand(i).accept(this)));
        }
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionStrategy e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionLabel e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionLabel e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionLabel e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionProp e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionProp e) throws PrismLangException {
        visitPre(e);
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionProp e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ExpressionFilter e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ExpressionFilter e) throws PrismLangException {
        visitPre(e);
        if (e.getFilter() != null) e.setFilter((Expression) (e.getFilter().accept(this)));
        if (e.getOperand() != null) e.setOperand((Expression) (e.getOperand().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ExpressionFilter e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ForLoop e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ForLoop e) throws PrismLangException {
        visitPre(e);
        if (e.getFrom() != null) e.setFrom((Expression) (e.getFrom().accept(this)));
        if (e.getTo() != null) e.setTo((Expression) (e.getTo().accept(this)));
        if (e.getStep() != null) e.setStep((Expression) (e.getStep().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final ForLoop e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Filter e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final Filter e) throws PrismLangException {
        visitPre(e);
        if (e.getExpression() != null) e.setExpression((Expression) (e.getExpression().accept(this)));
        visitPost(e);
        return e;
    }

    public void visitPost(final Filter e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final SyncSwarmFile e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
    public Object visit(final SyncSwarmFile e) throws PrismLangException {
        visitPre(e);
        final List<Agent> newAgents = new ArrayList<>();
        for (final Agent agent : e.getAgents())
            newAgents.add((Agent) agent.accept(this));
        e.setAgents(newAgents);
        e.setEnvironment((Agent) e.getEnvironment().accept(this));
        e.setLabelList((LabelList) e.getLabelList().accept(this));
        return e;
    }

    public void visitPost(final SyncSwarmFile e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Agent e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
    public Object visit(final Agent e) throws PrismLangException {
        visitPre(e);
        for (int i = 0; i < e.getDecls().size(); i++)
            e.getDecls().set(i, (Declaration) e.getDecls().get(i).accept(this));
        for (int i = 0; i < e.getActions().size(); i++)
            e.getActions().set(i, (Action) e.getActions().get(i).accept(this));
        for (int i = 0; i < e.getUpdates().size(); i++)
            e.getUpdates().set(i, (AgentUpdate) e.getUpdates().get(i).accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final Agent e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final Action e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
    public Object visit(final Action e) throws PrismLangException {
        visitPre(e);
        e.setCondition((Expression) e.getCondition().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final Action e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final AgentUpdate e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
    public Object visit(final AgentUpdate e) throws PrismLangException {
        visitPre(e);
        e.setStateCondition((Expression) e.getStateCondition().accept(this));
        e.setUpdates((Updates) e.getUpdates().accept(this));
        visitPost(e);
        return e;
    }

    public void visitPost(final AgentUpdate e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final FaultFile e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final FaultFile faultFile) throws PrismLangException {
        visitPre(faultFile);
        final ArrayList<Expression> newGuards = new ArrayList<>();
        final ArrayList<Updates> newUpdates = new ArrayList<>();
        for (int i = 0; i < faultFile.getGuards().size(); ++i) {
            newGuards.add((Expression) faultFile.getGuards().get(i).accept(this));
            newUpdates.add((Updates) faultFile.getUpdates().get(i).accept(this));
        }
        faultFile.setGuards(newGuards);
        faultFile.setUpdates(newUpdates);
        visitPost(faultFile);
        return faultFile;
    }

    public void visitPost(final FaultFile e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final ActionTypes e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final ActionTypes actionTypes) throws PrismLangException {
        visitPre(actionTypes);
        visitPost(actionTypes);
        return actionTypes;
    }

    public void visitPost(final ActionTypes e) throws PrismLangException {
        defaultVisitPost(e);
    }

    // -----------------------------------------------------------------------------------
    public void visitPre(final AsyncSwarmFile e) throws PrismLangException {
        defaultVisitPre(e);
    }

    @Override
	public Object visit(final AsyncSwarmFile swarmFile) throws PrismLangException {
        visitPre(swarmFile);
        swarmFile.setActionTypes((ActionTypes) swarmFile.getActionTypes().accept(this));
        final List<Module> newAgents = new ArrayList<>();
        for (final Module agent : swarmFile.getAgents())
            newAgents.add((Module) agent.accept(this));
        swarmFile.setAgents(newAgents);
        swarmFile.setEnvironment((Module) swarmFile.getEnvironment().accept(this));
        visitPost(swarmFile);
        return swarmFile;
    }

    public void visitPost(final AsyncSwarmFile e) throws PrismLangException {
        defaultVisitPost(e);
    }
}

