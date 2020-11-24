package parser.ast;

import java.util.Vector;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class SRewardStruct extends RewardStruct {
	
	public SRewardStruct() {
		super();
	}
	
	public String getGroupSynch(int i) {
		return ((SRewardStructItem) getRewardStructItem(i)).getGroupSynch();
	}
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}
	
	@Override
	public String toString() {
		int i, n;
		String s = "";
		s += "synchrewards";
		if (getName() != null && getName().length() > 0) s += " \"" + getName() + "\"";
		s += " \n\n";
		n = getNumItems();
		for (i = 0; i < n; i++) {
			s += "\t" + getRewardStructItem(i) + "\n";
		}
		s += "\nendsynchrewards\n";
		return s;
	}

	@Override
	public ASTElement deepCopy() {
		int i, n;
		SRewardStruct ret = new SRewardStruct();
		ret.setName(getName());
		n = getNumItems();
		for (i = 0; i < n; i++) {
			ret.addItem((SRewardStructItem)getRewardStructItem(i).deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}
