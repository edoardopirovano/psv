package parser.ast;

import java.util.ArrayList;
import java.util.List;

public class SModule extends Module {
		
	private ArrayList<Command> commands;
	
	public SModule(String n) {
		super(n);
		commands = new ArrayList<Command>();
	}
	
	@Override
	public void addCommand(Command c) {
		commands.add(c);
		c.setParent(this);
	}
	
	@Override
	public SCommand getCommand(int i) {
		return (SCommand) commands.get(i);
	}
	
	@Override
	public List<Command> getCommands() {
		return commands;
	}
	
	@Override
	public int getNumCommands() {
		return commands.size();
	}
	
	@Override
	public void setCommand(int i, Command c) {
		commands.set(i, c);
		c.setParent(this);
	}
	
	@Override
	public String toString() {
		String s = "";
		int i, n;
		
		s = s + "synch " + getName() + "\n\n";
		n = getNumDeclarations();
		for (i = 0; i < n; i++) {
			s = s + "\t" + getDeclaration(i) + ";\n";
		}
		if (n > 0) s = s + "\n";
		n = getNumCommands();
		for (i = 0; i < n; i++) {
			s = s + "\t" + getCommand(i) + ";\n";
		}
		s = s + "\nendsynch";
		
		return s;
	}
	
	@Override
	public ASTElement deepCopy() {
		int i, n;
		SModule ret = new SModule(getName());
		if (getNameASTElement() != null)
			ret.setNameASTElement((ExpressionIdent)getNameASTElement().deepCopy());
		n = getNumDeclarations();
		for (i = 0; i < n; i++) {
			ret.addDeclaration((Declaration)getDeclaration(i).deepCopy());
		}
		n = getNumCommands();
		for (i = 0; i < n; i++) {
			ret.addCommand((SCommand) getCommand(i).deepCopy());
		}
		if (getInvariant() != null)
			ret.setInvariant(getInvariant().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}
