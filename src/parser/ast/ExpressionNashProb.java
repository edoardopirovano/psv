package parser.ast;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionNashProb extends ExpressionQuant {

	protected Expression expr1;
	protected Expression expr2;
	
	public ExpressionNashProb() {
		
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
		ExpressionNashProb expr = new ExpressionNashProb();
		expr.setExpression1(getExpression1() == null ? null : getExpression1().deepCopy());
		expr.setExpression2(getExpression2() == null ? null : getExpression2().deepCopy());
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
		String s = " P[" + expr1.toString() + "," + expr2.toString() + "] ";
		return s;
	}
	
}
