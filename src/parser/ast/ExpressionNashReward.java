package parser.ast;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionNashReward extends ExpressionQuant {

	protected Expression expr1;
	protected Expression expr2;
	protected Object rewardStructIndex1 = null;
	protected Object rewardStructIndex2 = null;
	
	public ExpressionNashReward() {
		
	}
	
	public void setExpression1(Expression e) {
		this.expr1 = e;
	}
	
	public void setExpression2(Expression e) {
		this.expr2 = e;
	}
	
	public Expression getExpression1() {
		return this.expr1;
	}
	
	public Expression getExpression2() {
		return this.expr2;
	}
	
	public Object getRewardStructIndex1() {
		return this.rewardStructIndex1;
	}
	
	public Object getRewardStructIndex2() {
		return this.rewardStructIndex2;
	}
 	
	public void setRewardStructIndex1(Object r) {
		this.rewardStructIndex1 = r;
	}
	
	public void setRewardStructIndex2(Object r) {
		this.rewardStructIndex2 = r;
	}
	
	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConstant() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProposition() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean returnsSingleValue() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Expression deepCopy() {
		ExpressionNashReward expr = new ExpressionNashReward();
		expr.setExpression1(getExpression1() == null ? null : getExpression1().deepCopy());
		expr.setExpression2(getExpression2() == null ? null : getExpression2().deepCopy());
		expr.setRewardStructIndex1(getRewardStructIndex1());
		expr.setRewardStructIndex2(getRewardStructIndex2());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}

	@Override
	public String toString() {
		String s = " R{";
		if (rewardStructIndex1 != null) {
			if (rewardStructIndex1 instanceof Expression) s += rewardStructIndex1;
			else if (rewardStructIndex1 instanceof String) s += "\"" + rewardStructIndex1 + "\"";
		}
		s += ",";
		if (rewardStructIndex2 != null) {
			if (rewardStructIndex2 instanceof Expression) s += rewardStructIndex2;
			else if (rewardStructIndex2 instanceof String) s += "\"" + rewardStructIndex2 + "\"";
		}
		s += "}";
		s += "[" + expr1.toString() + "," + expr2.toString() + "] ";
		return s;
	}

}
