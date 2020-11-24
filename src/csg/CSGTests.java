package csg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import explicit.CSG;
import parser.ast.ModulesFile;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismPrintStreamLog;

public class CSGTests {

	static Prism prism;
	static ModulesFile mfile;
	
	public static void main(String[] args) throws PrismException, FileNotFoundException {
		// TODO Auto-generated method stub

		Path currentRelativePath = Paths.get("");
		String path = currentRelativePath.toAbsolutePath().toString();
				
		File file;
		
	    prism = new Prism(new PrismPrintStreamLog(System.out));
		prism.setMainLog(new PrismFileLog("stdout"));
		prism.initialise();
		mfile = new ModulesFile();
		
		//file = new File(path + "/src/csg/models/csgr.prism");
		file = new File(path + "/src/csg/models/csgr2.prism");
		
		mfile = prism.parseModelFile(file);
		prism.loadPRISMModel(mfile);
		prism.setEngine(Prism.EXPLICIT);
		prism.buildModel();
		
		explicit.Model model = prism.getBuiltModelExplicit();
		CSG csg = (CSG) model;
		
		//PrintStream printStream = new PrintStream(new FileOutputStream(path + "/src/csg/models/csgr.dot", false)); 
		PrintStream printStream = new PrintStream(new FileOutputStream(path + "/src/csg/models/csgr2.dot", false)); 
		PrismLog logout = new PrismPrintStreamLog(printStream);
		
		csg.exportToDotFile(logout, null, true);
	
		System.out.println();
		System.out.println(csg.getSynchMap());
		
	}

}
