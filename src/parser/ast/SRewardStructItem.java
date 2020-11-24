package parser.ast;

import java.util.List;
import java.util.ArrayList;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class SRewardStructItem extends RewardStructItem {

	private List<String> synchs = null;
	private String groupSynch;
	
	public SRewardStructItem(List<String> l, Expression s, Expression r) {
		super("", s, r);
		synchs = l;
		setSynch(l);
	}
	
	public void addSych(String s) {
		if(synchs == null) synchs = new ArrayList<String>();
			synchs.add(s);
	}
	
	public List<String> getSynchs() {
		return synchs;
	}
	
	public int getNumSynchs() {
		return synchs.size();
	}
	
	public String getGroupSynch() {
		return groupSynch;
	}
	
	@Override
	public boolean isTransitionReward() {
		return (!synchs.isEmpty());
	}
	
	public void setSynch(List<String> l) {
		String s = "";
		s += "[";
		for(int n = 0; n < synchs.size(); n++) {
			s += synchs.get(n);
			if(n < synchs.size()-1)
				s += ",";
		}
		s += "]";
		groupSynch = s;
	}

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}
	
	@Override
	public String toString() {
		String s = "";
		s += "[";
		for(int n = 0; n < synchs.size(); n++) {
			s += synchs.get(n);
			if(n < synchs.size()-1)
				s += ",";
		}
		s += "]";
		s += getStates() + " : " + getReward() + ";";
		return s;
	}

	@Override
	public ASTElement deepCopy() {
		SRewardStructItem ret = new SRewardStructItem(synchs, getStates().deepCopy(), getReward().deepCopy());
		ret.setSynchIndex(getSynchIndex());
		ret.setPosition(this);
		return ret;
	}
	
}
