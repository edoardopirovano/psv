package parser.ast;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionNash extends ExpressionQuant {

	protected Expression expr;
	
	public ExpressionNash() {
	}

	public void setExpression(Expression e) {
		this.expr = e;
	}
	
	public Expression getExpression() {
		return this.expr;
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
		ExpressionNash expr = new ExpressionNash();
		expr.setExpression(getExpression() == null ? null : getExpression().deepCopy());
		expr.setRelOp(getRelOp());
		expr.setBound(getBound() == null ? null : getBound().deepCopy());
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
		String s = "N";
		s += getRelOp();
		s += (getBound()==null) ? "?" : getBound().toString();
		s += "[" + expr.toString() + " ]";
		return s;
	}

	@Override
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
