package parser.ast;

import java.util.ArrayList;
import java.util.List;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class SCommand extends Command {

	List<String> synchs;
	List<Integer> synchsIndexes;
	
	public SCommand() {
		this.synchs = new ArrayList<String>();
	}
	
	public void addSynch(String s) {
		this.synchs.add(s);
	}

	public List<String> getSynchs() {
		return this.synchs;
	}
	
	public int getNumSynchs() {
		return this.synchs.size();
	}
	
	public void setSynchs(List<String> l) {
		this.synchs = l;
	}
	
	public void addIndex(int i) {
		this.synchsIndexes.add(i);
	}

	public List<Integer> getSynchsIndexes() {
		return this.synchsIndexes;
	}
	
	public void setSynchsIndexes(List<Integer> l) {
		this.synchsIndexes = l;
	}
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException {
		return v.visit(this);
	}
	
	@Override
	public String toString() {
		String s = "[";
		s += synchs.get(0);
		for(int i = 1; i < synchs.size(); i++) {
			s+= "," + synchs.get(i); 
		}
		s += "] " + getGuard() + " -> " + getUpdates();
		return s;
	}
	
	@Override
	public ASTElement deepCopy() {
		SCommand ret = new SCommand();
		ret.setSynchs(synchs);
		ret.setGuard(getGuard().deepCopy());
		ret.setUpdates((Updates)getUpdates().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}
