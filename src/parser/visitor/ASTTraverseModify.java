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

import parser.ast.*;
import parser.ast.Module;
import prism.PrismLangException;

import java.util.ArrayList;
import java.util.List;

// Variant of ASTTraverse.
// Performs a depth-first traversal of an asbtract syntax tree (AST),
// replacing each child node with the object returned by the recursive visit call.
// Like ASTTraverse, many traversal-based tasks can be implemented by extending and either:
// (a) overriding defaultVisitPre or defaultVisitPost
// (b) overiding visit for leaf (or other selected) nodes

public class ASTTraverseModify implements ASTVisitor
{
	public void defaultVisitPre(ASTElement e) throws PrismLangException {}
	public void defaultVisitPost(ASTElement e) throws PrismLangException {}
	// -----------------------------------------------------------------------------------
	public void visitPre(ModulesFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ModulesFile e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		if (e.getFormulaList() != null) e.setFormulaList((FormulaList)(e.getFormulaList().accept(this)));
		if (e.getLabelList() != null) e.setLabelList((LabelList)(e.getLabelList().accept(this)));
		if (e.getConstantList() != null) e.setConstantList((ConstantList)(e.getConstantList().accept(this)));
		n = e.getNumGlobals();
		for (i = 0; i < n; i++) {
			if (e.getGlobal(i) != null) e.setGlobal(i, (Declaration)(e.getGlobal(i).accept(this)));
		}
		n = e.getNumModules();
		for (i = 0; i < n; i++) {
			if (e.getModule(i) != null) e.setModule(i, (parser.ast.Module)(e.getModule(i).accept(this)));
		}
		n = e.getNumSystemDefns();
		for (i = 0; i < n; i++) {
			if (e.getSystemDefn(i) != null) e.setSystemDefn(i, (SystemDefn)(e.getSystemDefn(i).accept(this)), e.getSystemDefnName(i));
		}
		n = e.getNumRewardStructs();
		for (i = 0; i < n; i++) {
			if (e.getRewardStruct(i) != null) e.setRewardStruct(i, (RewardStruct)(e.getRewardStruct(i).accept(this)));
		}
		if (e.getInitialStates() != null) e.setInitialStates((Expression)(e.getInitialStates().accept(this)));
		n = e.getNumPlayers();
		for (i = 0; i < n; i++) {
			if (e.getPlayer(i) != null) e.setPlayer(i, (Player)(e.getPlayer(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(ModulesFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(PropertiesFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(PropertiesFile e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		if (e.getLabelList() != null) e.setLabelList((LabelList)(e.getLabelList().accept(this)));
		if (e.getConstantList() != null) e.setConstantList((ConstantList)(e.getConstantList().accept(this)));
		n = e.getNumProperties();
		for (i = 0; i < n; i++) {
			if (e.getPropertyObject(i) != null) e.setPropertyObject(i, (Property)(e.getPropertyObject(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(PropertiesFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------

	public void visitPre(Property e) throws PrismLangException
	{
		defaultVisitPre(e);
	}

	public Object visit(Property e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null)
			e.setExpression((Expression) e.getExpression().accept(this));
		visitPost(e);
		return e;
	}

	public void visitPost(Property e) throws PrismLangException
	{
		defaultVisitPost(e);
	}

	// -----------------------------------------------------------------------------------
	public void visitPre(FormulaList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(FormulaList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getFormula(i) != null) e.setFormula(i, (Expression)(e.getFormula(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(FormulaList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(LabelList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(LabelList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getLabel(i) != null) e.setLabel(i, (Expression)(e.getLabel(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(LabelList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ConstantList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ConstantList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getConstant(i) != null) e.setConstant(i, (Expression)(e.getConstant(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(ConstantList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Declaration e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Declaration e) throws PrismLangException
	{
		visitPre(e);
		if (e.getDeclType() != null) e.setDeclType((DeclarationType)e.getDeclType().accept(this));
		if (e.getStart() != null) e.setStart((Expression)e.getStart().accept(this));
		visitPost(e);
		return e;
	}
	public void visitPost(Declaration e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationInt e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationInt e) throws PrismLangException
	{
		visitPre(e);
		if (e.getLow() != null) e.setLow((Expression)e.getLow().accept(this));
		if (e.getHigh() != null) e.setHigh((Expression)e.getHigh().accept(this));
		visitPost(e);
		return e;
	}
	public void visitPost(DeclarationInt e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationBool e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationBool e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(DeclarationBool e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationArray e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationArray e) throws PrismLangException
	{
		visitPre(e);
		if (e.getLow() != null) e.setLow((Expression)e.getLow().accept(this));
		if (e.getHigh() != null) e.setHigh((Expression)e.getHigh().accept(this));
		if (e.getSubtype() != null) e.setSubtype((DeclarationType)e.getSubtype().accept(this));
		visitPost(e);
		return e;
	}
	public void visitPost(DeclarationArray e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationClock e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationClock e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(DeclarationClock e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationIntUnbounded e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationIntUnbounded e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(DeclarationIntUnbounded e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(parser.ast.Module e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(parser.ast.Module e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumDeclarations();
		for (i = 0; i < n; i++) {
			if (e.getDeclaration(i) != null) e.setDeclaration(i, (Declaration)(e.getDeclaration(i).accept(this)));
		}
		if (e.getInvariant() != null)
			e.setInvariant((Expression)(e.getInvariant().accept(this)));
		n = e.getNumCommands();
		for (i = 0; i < n; i++) {
			if (e.getCommand(i) != null) e.setCommand(i, (Command)(e.getCommand(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(parser.ast.Module e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Command e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Command e) throws PrismLangException
	{
		visitPre(e);
		e.setGuard((Expression)(e.getGuard().accept(this)));
		e.setUpdates((Updates)(e.getUpdates().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(Command e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SCommand e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SCommand e) throws PrismLangException
	{
		visitPre(e);
		e.setGuard((Expression)(e.getGuard().accept(this)));
		e.setUpdates((Updates)(e.getUpdates().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(SCommand e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Updates e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Updates e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumUpdates();
		for (i = 0; i < n; i++) {
			if (e.getProbability(i) != null) e.setProbability(i, (Expression)(e.getProbability(i).accept(this)));
			if (e.getUpdate(i) != null) e.setUpdate(i, (Update)(e.getUpdate(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(Updates e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Update e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Update e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			if (e.getExpression(i) != null) e.setExpression(i, (Expression)(e.getExpression(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(Update e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RenamedModule e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RenamedModule e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(RenamedModule e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RewardStruct e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RewardStruct e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumItems();
		for (i = 0; i < n; i++) {
			if (e.getRewardStructItem(i) != null) e.setRewardStructItem(i, (RewardStructItem)(e.getRewardStructItem(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(RewardStruct e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RewardStructItem e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RewardStructItem e) throws PrismLangException
	{
		visitPre(e);
		e.setStates((Expression)(e.getStates().accept(this)));
		e.setReward((Expression)(e.getReward().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(RewardStructItem e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Player e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Player e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(Player e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemInterleaved e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemInterleaved e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.setOperand(i, (SystemDefn)(e.getOperand(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(SystemInterleaved e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemFullParallel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemFullParallel e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.setOperand(i, (SystemDefn)(e.getOperand(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(SystemFullParallel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemParallel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemParallel e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand1((SystemDefn)(e.getOperand1().accept(this)));
		e.setOperand2((SystemDefn)(e.getOperand2().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(SystemParallel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemHide e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemHide e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand((SystemDefn)(e.getOperand().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(SystemHide e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemRename e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemRename e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand((SystemDefn)(e.getOperand().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(SystemRename e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemModule e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemModule e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(SystemModule e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemBrackets e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemBrackets e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand((SystemDefn)(e.getOperand().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(SystemBrackets e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemReference e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemReference e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(SystemReference e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionTemporal e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionTemporal e) throws PrismLangException
	{
		visitPre(e);
		if (e.getOperand1() != null) e.setOperand1((Expression)(e.getOperand1().accept(this)));
		if (e.getOperand2() != null) e.setOperand2((Expression)(e.getOperand2().accept(this)));
		if (e.getLowerBound() != null) e.setLowerBound((Expression)(e.getLowerBound().accept(this)), e.lowerBoundIsStrict());
		if (e.getUpperBound() != null) e.setUpperBound((Expression)(e.getUpperBound().accept(this)), e.upperBoundIsStrict());
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionTemporal e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionITE e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionITE e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand1((Expression)(e.getOperand1().accept(this)));
		e.setOperand2((Expression)(e.getOperand2().accept(this)));
		e.setOperand3((Expression)(e.getOperand3().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionITE e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionBinaryOp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionBinaryOp e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand1((Expression)(e.getOperand1().accept(this)));
		e.setOperand2((Expression)(e.getOperand2().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionBinaryOp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionUnaryOp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionUnaryOp e) throws PrismLangException
	{
		visitPre(e);
		e.setOperand((Expression)(e.getOperand().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionUnaryOp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFunc e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFunc e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.setOperand(i, (Expression)(e.getOperand(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionFunc e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionIdent e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionIdent e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionLiteral e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionLiteral e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionLiteral e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionConstant e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionConstant e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionConstant e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFormula e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFormula e) throws PrismLangException
	{
		visitPre(e);
		if (e.getDefinition() != null) e.setDefinition((Expression)(e.getDefinition().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionFormula e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionVar e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionVar e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionProb e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		visitPre(e);
		if (e.getProb() != null) e.setProb((Expression)(e.getProb().accept(this)));
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		if (e.getFilter() != null) e.setFilter((Filter)(e.getFilter().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionProb e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionReward e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		visitPre(e);
		if (e.getRewardStructIndex() != null && e.getRewardStructIndex() instanceof Expression)
			e.setRewardStructIndex((Expression)(((Expression)e.getRewardStructIndex()).accept(this)));
		if (e.getRewardStructIndexDiv() != null && e.getRewardStructIndexDiv() instanceof Expression)
			e.setRewardStructIndexDiv((Expression)(((Expression)e.getRewardStructIndexDiv()).accept(this)));
		if (e.getReward() != null) e.setReward((Expression)(e.getReward().accept(this)));
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		if (e.getFilter() != null) e.setFilter((Filter)(e.getFilter().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionReward e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionNash e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionNash e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null)
			e.setExpression((Expression)(((Expression)e.getExpression()).accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionNash e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionNashProb e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionNashProb e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression1() !=null)
			e.setExpression1((Expression)(((Expression)e.getExpression1()).accept(this)));
		if (e.getExpression2() !=null)
			e.setExpression2((Expression)(((Expression)e.getExpression2()).accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionNashProb e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionNashReward e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionNashReward e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression1() !=null)
			e.setExpression1((Expression)(((Expression)e.getExpression1()).accept(this)));
		if (e.getExpression2() !=null)
			e.setExpression2((Expression)(((Expression)e.getExpression2()).accept(this)));		
		if (e.getRewardStructIndex1() != null && e.getRewardStructIndex1() instanceof Expression)
			e.setRewardStructIndex1((Expression)(((Expression)e.getRewardStructIndex1()).accept(this)));
		if (e.getRewardStructIndex2() != null && e.getRewardStructIndex2() instanceof Expression)
			e.setRewardStructIndex2((Expression)(((Expression)e.getRewardStructIndex2()).accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionNashReward e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionSS e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		visitPre(e);
		if (e.getProb() != null) e.setProb((Expression)(e.getProb().accept(this)));
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		if (e.getFilter() != null) e.setFilter((Filter)(e.getFilter().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionSS e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionExists e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionExists e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionExists e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionForAll e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionForAll e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionForAll e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionStrategy e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.setOperand(i, (Expression)(e.getOperand(i).accept(this)));
		}
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionStrategy e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionLabel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionLabel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionProp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionProp e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionProp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFilter e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFilter e) throws PrismLangException
	{
		visitPre(e);
		if (e.getFilter() != null) e.setFilter((Expression)(e.getFilter().accept(this)));
		if (e.getOperand() != null) e.setOperand((Expression)(e.getOperand().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ExpressionFilter e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ForLoop e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ForLoop e) throws PrismLangException
	{
		visitPre(e);
		if (e.getFrom() != null) e.setFrom((Expression)(e.getFrom().accept(this)));
		if (e.getTo() != null) e.setTo((Expression)(e.getTo().accept(this)));
		if (e.getStep() != null) e.setStep((Expression)(e.getStep().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(ForLoop e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Filter e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Filter e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.setExpression((Expression)(e.getExpression().accept(this)));
		visitPost(e);
		return e;
	}
	public void visitPost(Filter e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SyncSwarmFile e) throws PrismLangException { defaultVisitPre(e); }
	@Override
	public Object visit(SyncSwarmFile e) throws PrismLangException {
		visitPre(e);
		e.setAgent((Agent) e.getAgent().accept(this));
		e.setEnvironment((Agent) e.getEnvironment().accept(this));
		e.setLabelList((LabelList) e.getLabelList().accept(this));
		return e;
	}
	public void visitPost(SyncSwarmFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Agent e) throws PrismLangException { defaultVisitPre(e); }
	@Override
	public Object visit(Agent e) throws PrismLangException {
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
	public void visitPost(Agent e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Action e) throws PrismLangException { defaultVisitPre(e); }
	@Override
	public Object visit(Action e) throws PrismLangException {
		visitPre(e);
		e.setCondition((Expression) e.getCondition().accept(this));
		visitPost(e);
		return e;
	}
	public void visitPost(Action e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(AgentUpdate e) throws PrismLangException { defaultVisitPre(e); }
	@Override
	public Object visit(AgentUpdate e) throws PrismLangException {
		visitPre(e);
		e.setStateCondition((Expression) e.getStateCondition().accept(this));
		e.setUpdates((Updates) e.getUpdates().accept(this));
		visitPost(e);
		return e;
	}
	public void visitPost(AgentUpdate e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(FaultFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(FaultFile faultFile) throws PrismLangException {
		visitPre(faultFile);
		ArrayList<Expression> newGuards = new ArrayList<>();
		ArrayList<Updates> newUpdates = new ArrayList<>();
		for (int i = 0; i < faultFile.getGuards().size(); ++i) {
			newGuards.add((Expression) faultFile.getGuards().get(i).accept(this));
			newUpdates.add((Updates) faultFile.getUpdates().get(i).accept(this));
		}
		faultFile.setGuards(newGuards);
		faultFile.setUpdates(newUpdates);
		visitPost(faultFile);
		return faultFile;
	}
	public void visitPost(FaultFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ActionTypes e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ActionTypes actionTypes) throws PrismLangException {
		visitPre(actionTypes);
		visitPost(actionTypes);
		return actionTypes;
	}
	public void visitPost(ActionTypes e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(AsyncSwarmFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(AsyncSwarmFile swarmFile) throws PrismLangException {
		visitPre(swarmFile);
		swarmFile.setActionTypes((ActionTypes) swarmFile.getActionTypes().accept(this));
		List<Module> newAgents = new ArrayList<>();
		for (Module agent : swarmFile.getAgents())
			newAgents.add((Module) agent.accept(this));
		swarmFile.setAgents(newAgents);
		swarmFile.setEnvironment((Module) swarmFile.getEnvironment().accept(this));
		visitPost(swarmFile);
		return swarmFile;
	}
	public void visitPost(AsyncSwarmFile e) throws PrismLangException { defaultVisitPost(e); }
}

