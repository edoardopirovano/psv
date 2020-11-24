package explicit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.IterableBitSet;
import explicit.CSG;
import explicit.ProbModelChecker.TermCrit;
import explicit.rewards.CSGRewards;
import explicit.rewards.CSGRewardsSimple;
import explicit.rewards.MDPRewards;
import explicit.rewards.MDPRewardsSimple;
import explicit.rewards.StateRewardsConstant;
import ilog.concert.IloException;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
/*
import ilog.cplex.IloCplex;
import ilog.concert.IloException;
import ilog.concert.IloMPModeler;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
*/
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import parser.State;
import parser.ast.Coalition;
import parser.ast.ExpressionTemporal;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismPrintStreamLog;
import prism.PrismUtils;
import prism.Result;
import strat.InvalidStrategyStateException;
import strat.MDStrategyArray;
import strat.Strategy;

public class CSGModelChecker extends ProbModelChecker {

	protected ArrayList<ArrayList<ArrayList<Distribution>>> probMatrices;
	protected ArrayList<ArrayList<ArrayList<Double>>> rewMatrices;
	protected ArrayList<Integer> rowPlayers;
	protected ArrayList<Integer> colPlayers;
	public MDPModelChecker mdp_mc;
	protected HashMap<Integer, Integer> map_state;
	protected List<State> list_state;
	protected int maxRows;
	protected int maxCols;
	public DTMCModelChecker dtmc_mc;
	
	// delete this variable afterwards
	protected CSG csg_model;
	
	long timerBPM;
	long timerBRM;
	long timerVal;	 

	protected HashMap<Integer, Distribution> adv; // probably shouldn't be global
	protected Coalition current_coalition; // shouldn't exist
	
	/**
	 * Used when calling methods computing reachability rewards. Says that the
	 * runs which don't reach the target get reward infinity.
	 */
	public static final int R_INFINITY = 0;
	/**
	 * Used when calling methods computing reachability rewards. Says that the
	 * runs which don't reach the target get the reward cumulated along the run
	 * (i.e. possibly infinite, possibly finite)
	 */
	public static final int R_CUMULATIVE = 1;
	/**
	 * Used when calling methods computing reachability rewards. Says that the
	 * runs which don't reach the target get reward zero.
	 */
	public static final int R_ZERO = 2;
	
	public CSGModelChecker(PrismComponent parent) throws PrismException {
		super(parent);
		mainLog.println("CSG Model Checker");
		mdp_mc = new MDPModelChecker(parent);
		dtmc_mc = new DTMCModelChecker(parent);
		map_state = new HashMap<Integer, Integer>(); //probably does not have to be initialized here
		list_state = new ArrayList<State>(); //probably does not have to be initialized here
		rowPlayers = new ArrayList<Integer>();
		colPlayers  = new ArrayList<Integer>();
		probMatrices = null;
		rewMatrices = null;
		maxRows = 0;
		maxCols = 0;
		timerBPM = 0;
		timerBRM = 0;
		timerVal = 0;
		
		mdp_mc.setVerbosity(0);
		mdp_mc.setSilentPrecomputations(true);
	}

	public void setCoalition(CSG csg, CSGRewards csgRewards, Coalition coalition, boolean min1, boolean min2) throws PrismException {
		current_coalition = coalition;
		ArrayList<Integer> nrowPlayers = new ArrayList<Integer>();
		ArrayList<Integer> ncolPlayers  = new ArrayList<Integer>();
		boolean drp;
		boolean dcp;	
		for(Integer i : csg.getPlayerNames().keySet()) {						
			if(min1) {
				if(coalition.isPlayerIndexInCoalition(i, csg.getPlayerNames())) {
					ncolPlayers.add(i-1);
				}
				else {
					nrowPlayers.add(i-1);
				}
			}
			else {
				if(coalition.isPlayerIndexInCoalition(i, csg.getPlayerNames())) {
					nrowPlayers.add(i-1);
				}
				else {
					ncolPlayers.add(i-1);
				}
			}
		}
		if(!nrowPlayers.equals(rowPlayers)) {
			drp = true;
			rowPlayers = nrowPlayers;
		}
		else {
			drp = false;
		}
		if(!ncolPlayers.equals(colPlayers)) {
			dcp = true;
			colPlayers = ncolPlayers;
		}
		else {
			dcp = false;
		}
		if(probMatrices != null) {
			probMatrices.clear();
		}
		else if(drp && dcp) {
			buildAllProbMatrixGames(csg);
		}
		if(rewMatrices != null) {
			rewMatrices.clear();
		}
		else if (csgRewards != null) {
			buildAllRewMatrixGames(csg, csgRewards);
		}
	}
		
	public void buildAllProbMatrixGames(CSG csg) throws PrismException {
		probMatrices = new ArrayList<ArrayList<ArrayList<Distribution>>>();
		for(int s = 0; s < csg.getNumStates(); s++) {
			probMatrices.add(s, buildProbMatrixGame(csg, null, s));
		}
	}
	
	public void buildAllRewMatrixGames(CSG csg, CSGRewards csgRewards) throws PrismException {
		rewMatrices = new ArrayList<ArrayList<ArrayList<Double>>>();
		for(int s = 0; s < csg.getNumStates(); s++) {
			rewMatrices.add(s, buildRewMatrixGame(csg, csgRewards, s));
		}
	}
	
	public ArrayList<ArrayList<Distribution>> buildProbMatrixGame(CSG csg, ArrayList<ArrayList<String>> msynch, int s)  throws PrismException {
		//System.out.println("\n-- Prob Matrix");
		//System.out.println("-- s " + s);
		if(probMatrices != null && !probMatrices.isEmpty() && (probMatrices.size() > s)) {
			return probMatrices.get(s);
		}		
		Set<String> acts = new HashSet<String>();
        Set<String> ract = new HashSet<String>();
        Set<String> cact = new HashSet<String>();
        Map<String, Map<String, Distribution>> mgame = new HashMap<String, Map<String, Distribution>>();
        ArrayList<ArrayList<Distribution>> mdist = new  ArrayList<ArrayList<Distribution>>();
        Set<String> act1 = new HashSet<String>();
        Set<String> act2 = new HashSet<String>();
        String[] actions = new String[csg.getNumPlayers()];
        Pattern pattern1 = Pattern.compile("(\\[\\w+(\\,\\w+)*\\])");
        Pattern pattern2 = Pattern.compile("\\w+");
        Matcher matcher1;
        Matcher matcher2;
        int a = 0;
        int nrow;
        int ncol;
        String rowact = "";
        String colact = "";
        for(Integer p : rowPlayers) {
    		ract.addAll(csg.getActionsForPlayer(p));
    		ract.add("i" + p);
        }
        for(Integer p : colPlayers) {
        	cact.addAll(csg.getActionsForPlayer(p));
    		cact.add("i" + p);
        }
        String g1;
        String g2;
        //System.out.println("-- ract " + ract);
        //System.out.println("-- cact " + cact);
		for(int t = 0; t < csg.getNumChoices(s); t++) {
			acts.clear();
			act1.clear();
			act2.clear();
			act1.addAll(ract);
			act2.addAll(cact);
			matcher1 = pattern1.matcher(csg.getAction(s, t).toString());
			while(matcher1.find()) {
				g1 = matcher1.group();
				//System.out.println(g1);
				matcher2 = pattern2.matcher(g1);
				while(matcher2.find()) {
					g2 = matcher2.group();
					//System.out.println(g2);
					actions[a] = g2;
					a++;
				}
			}
			//System.out.println("-- actions " + Arrays.toString(actions));
			acts.addAll(Arrays.asList(actions));
			a = 0;
			//System.out.println("-- acts " + acts);
			//System.out.println("-- act1 " + act1);
			//System.out.println("-- act2 " + act2);			
    		act1.retainAll(acts);
    		act2.retainAll(acts);
    		rowact = "";
    		colact = "";
    		for(String act : act1) {
    			rowact = rowact.concat(act);
    		}
    		for(String act : act2) {
    			colact = colact.concat(act);
    		}
    		if(!mgame.containsKey(rowact)) {
    		//System.out.println("-- rowact " + rowact);	
    		//System.out.println("-- colact " + colact);
			mgame.put(rowact, new HashMap<String, Distribution>());
    		}
    		mgame.get(rowact).put(colact, csg.getChoice(s, t));   
		}
		//System.out.println(mgame);
		act1.clear();
		act2.clear();
		nrow = 0;
		ncol = 0;
		for(String row : mgame.keySet()) {
			mdist.add(new ArrayList<Distribution>());
			if(msynch != null)
				msynch.add(new ArrayList<String>());
		}	
		for(String col : mgame.get(rowact).keySet()) {
			for(String row : mgame.keySet()) {
				mdist.get(nrow).add(ncol, mgame.get(row).get(col));
				if(msynch != null)
					msynch.get(nrow).add(row.concat(";").concat(col));
				nrow++;
			}
			nrow = 0;
			ncol++;
		}
		//System.out.println(mgame);
		//System.out.println(mdist);
        return mdist;
	}
	/*
	public ArrayList<ArrayList<Distribution>> buildProbMatrixGame(CSG csg, ArrayList<ArrayList<String>> msynch, int s)  throws PrismException {
		if(probMatrices != null && !probMatrices.isEmpty() && (probMatrices.size() > s)) {
			return probMatrices.get(s);
		}
		long timer = System.currentTimeMillis();
		Set<String> acts = new HashSet<String>();
        Set<String> ract = new HashSet<String>();
        Set<String> cact = new HashSet<String>();
        Map<String, Map<String, Distribution>> mgame = new HashMap<String, Map<String, Distribution>>();
        ArrayList<ArrayList<Distribution>> mdist = new  ArrayList<ArrayList<Distribution>>();
        Set<String> act1 = new HashSet<String>();
        Set<String> act2 = new HashSet<String>();
        String[] actions = new String[csg.getNumPlayers()];
        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher;
        int a = 0;
		int nrow;
		int ncol;
		String rowact = "";
		String colact = "";
        for(Integer p : rowPlayers) {
    		ract.addAll(csg.getActionsForPlayer(p));
    		ract.add("i" + p);
        }
        for(Integer p : colPlayers) {
        	cact.addAll(csg.getActionsForPlayer(p));
    		cact.add("i" + p);
        }
		for(int t = 0; t < csg.getNumChoices(s); t++) {
			acts.clear();
			act1.clear();
			act2.clear();
			act1.addAll(ract);
			act2.addAll(cact);
			matcher = pattern.matcher(csg.getAction(s, t).toString());
			while(matcher.find()) {
				actions[a] = matcher.group();
				a++;
			}
			acts.addAll(Arrays.asList(actions));
			a = 0;
    		act1.retainAll(acts);
    		act2.retainAll(acts);
    		rowact = "";
    		colact = "";
    		for(String act : act1) {
    			rowact = rowact.concat(act);
    		}
    		for(String act : act2) {
    			colact = colact.concat(act);
    		}
    		if(!mgame.containsKey(rowact)) {
			mgame.put(rowact, new HashMap<String, Distribution>());
    		}
    		mgame.get(rowact).put(colact, csg.getChoice(s, t));    			
		}				
		act1.clear();
		act2.clear();
		nrow = 0;
		ncol = 0;
		for(String row : mgame.keySet()) {
			mdist.add(new ArrayList<Distribution>());
			if(msynch != null)
				msynch.add(new ArrayList<String>());
		}	
		for(String col : mgame.get(rowact).keySet()) {
			for(String row : mgame.keySet()) {
				mdist.get(nrow).add(ncol, mgame.get(row).get(col));
				if(msynch != null)
					msynch.get(nrow).add(row.concat(";").concat(col));
				nrow++;
			}
			nrow = 0;
			ncol++;
		}
		//System.out.println("nract " + nract);
		//System.out.println("ncact " + ncact);
		//System.out.println("matrix game");
		//System.out.println(mgame);
		//System.out.println("matrix distribution");
		//System.out.println(mdist);
		//System.out.println("-- msynch");
		//System.out.println(msynch);
		//System.out.println("\n$$ buildProbMatrix check");	
		timer = System.currentTimeMillis() - timer;
		timerBPM += timer;
		return mdist;
	}
	*/
	public ArrayList<ArrayList<Double>> buildRewMatrixGame(CSG csg, CSGRewards csgRewards, int s)  throws PrismException {        
		//System.out.println("\n-- Rew Matrix");
		//System.out.println("-- s " + s);
		if(rewMatrices != null && !rewMatrices.isEmpty() && (rewMatrices.size() > s)) {
			return rewMatrices.get(s);
		}
		Set<String> acts = new HashSet<String>();
        Set<String> ract = new HashSet<String>();
        Set<String> cact = new HashSet<String>();
        Map<String, Map<String, Double>> mgame = new HashMap<String, Map<String, Double>>();
        ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
        Set<String> act1 = new HashSet<String>();
        Set<String> act2 = new HashSet<String>();
        String[] actions = new String[csg.getNumPlayers()];
        Pattern pattern1 = Pattern.compile("(\\[\\w+(\\,\\w+)*\\])");
        Pattern pattern2 = Pattern.compile("\\w+");
        Matcher matcher1;
        Matcher matcher2;
        int a = 0;
        int nrow;
        int ncol;
        String rowact = "";
        String colact = "";
        for(Integer p : rowPlayers) {
    		ract.addAll(csg.getActionsForPlayer(p));
    		ract.add("i" + p);
        }
        for(Integer p : colPlayers) {
        	cact.addAll(csg.getActionsForPlayer(p));
    		cact.add("i" + p);
        }
        String g1;
        String g2;
		for(int t = 0; t < csg.getNumChoices(s); t++) {
			acts.clear();
			act1.clear();
			act2.clear();
			act1.addAll(ract);
			act2.addAll(cact);
			matcher1 = pattern1.matcher(csg.getAction(s, t).toString());
			while(matcher1.find()) {
				g1 = matcher1.group();
				//System.out.println(g1);
				matcher2 = pattern2.matcher(g1);
				while(matcher2.find()) {
					g2 = matcher2.group();
					//System.out.println(g2);
					actions[a] = g2;
					a++;
				}
			}
			acts.addAll(Arrays.asList(actions));
			a = 0;
    		act1.retainAll(acts);
    		act2.retainAll(acts);
    		rowact = "";
    		colact = "";
    		for(String act : act1) {
    			rowact = rowact.concat(act);
    		}
    		for(String act : act2) {
    			colact = colact.concat(act);
    		}
    		if(!mgame.containsKey(rowact)) {
			mgame.put(rowact, new HashMap<String, Double>());
    		}
    		if(csgRewards.getTransitionReward(s, t) != 0) {
    			mgame.get(rowact).put(colact, csgRewards.getTransitionReward(s, t));
    		}
    		else {
    			mgame.get(rowact).put(colact, 0.0);
    		}
		}
		//System.out.println(mgame);
		act1.clear();
		act2.clear();
		nrow = 0;
		ncol = 0;
		for(String row : mgame.keySet()) {
			mrew.add(new ArrayList<Double>());
		}
		for(String col : mgame.get(rowact).keySet()) {
			for(String row : mgame.keySet()) {
				mrew.get(nrow).add(ncol, mgame.get(row).get(col));
				nrow++;
			}
			nrow = 0;
			ncol++;
		}
		//System.out.println(mgame);
		//System.out.println(mrew);
		return mrew;
	}
	/*
	public ArrayList<ArrayList<Double>> buildRewMatrixGame(CSG csg, CSGRewards csgRewards, int s)  throws PrismException {        
		if(rewMatrices != null && !rewMatrices.isEmpty() && (rewMatrices.size() > s)) {
			return rewMatrices.get(s);
		}
		long timer = System.currentTimeMillis();
		Set<String> acts = new HashSet<String>();
        Set<String> ract = new HashSet<String>();
        Set<String> cact = new HashSet<String>();
        Map<String, Map<String, Double>> mgame = new HashMap<String, Map<String, Double>>();
        ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
        Set<String> act1 = new HashSet<String>();
        Set<String> act2 = new HashSet<String>();
		String[] actions = new String[csg.getNumPlayers()];
        Pattern pattern = Pattern.compile("\\w+");
        Matcher matcher;
        int a = 0;
		int nrow;
		int ncol;
		String rowact = "";
		String colact = "";
        for(Integer p : rowPlayers) {
    			ract.addAll(csg.getActionsForPlayer(p));
    			ract.add("i" + p);
        }
        for(Integer p : colPlayers) {
        		cact.addAll(csg.getActionsForPlayer(p));
    			cact.add("i" + p);
        }
		for(int t = 0; t < csg.getNumChoices(s); t++) {
			acts.clear();
			act1.clear();
			act2.clear();
			act1.addAll(ract);
			act2.addAll(cact);
			matcher = pattern.matcher(csg.getAction(s, t).toString());
			while(matcher.find()) {
				actions[a] = matcher.group();
				a++;
			}
			acts.addAll(Arrays.asList(actions));
			a = 0;
    		act1.retainAll(acts);
    		act2.retainAll(acts);
    		rowact = "";
    		colact = "";
    		for(String act : act1) {
    			rowact = rowact.concat(act);
    		}
    		for(String act : act2) {
    			colact = colact.concat(act);
    		}
    		if(!mgame.containsKey(rowact)) {
			mgame.put(rowact, new HashMap<String, Double>());
    		}
    		if(csgRewards.getTransitionReward(s, t) != 0) {
    			mgame.get(rowact).put(colact, csgRewards.getTransitionReward(s, t));
    		}
    		else {
    			mgame.get(rowact).put(colact, 0.0);
    		}
		}
		act1.clear();
		act2.clear();
		nrow = 0;
		ncol = 0;
		for(String row : mgame.keySet()) {
			mrew.add(new ArrayList<Double>());
		}
		for(String col : mgame.get(rowact).keySet()) {
			for(String row : mgame.keySet()) {
				mrew.get(nrow).add(ncol, mgame.get(row).get(col));
				nrow++;
			}
			nrow = 0;
			ncol++;
		}
		//System.out.println("nract " + nract);
		//System.out.println("ncact " + ncact);
		//System.out.println("matrix game");
		//System.out.println(mgame);
		//System.out.println("matrix rewards");
		//System.out.println(mrew);
		//mainLog.println(mrew);
		//System.out.println("\n$$ buildRewMatrix check");	
		timer = System.currentTimeMillis() - timer;
		timerBRM += timer;
        return mrew;
	}
	*/
	//check need of min1, min2
	public double val(LpSolve lp, ArrayList<ArrayList<Distribution>> mdist, ArrayList<ArrayList<Double>> mrew, 
						HashMap<Integer, Distribution> adv, double[] sol, int s, 
						boolean rew, boolean min1, boolean min2, boolean genadv) throws LpSolveException, PrismException {
		long timer = System.currentTimeMillis();
		int nract = mdist.size();
		int ncact = mdist.get(0).size();
		double res = Double.NaN;
		double sum;
		//System.out.println("-- state " + s);
		Distribution d = new Distribution();
		if(nract == 1) {
			//System.out.println("$$ number of row actions = 1");
			res = Double.POSITIVE_INFINITY;
			if(genadv) {
				adv.get(s).clear();
				adv.get(s).set(0, 1.0);
			}
			for(int col = 0; col < mdist.get(0).size(); col++) {
				sum = 0.0;
				for(int t : mdist.get(0).get(col).getSupport()) {
					sum += mdist.get(0).get(col).get(t)*sol[t];
				}
				if(rew) { //action rewards, what about the case of state rewards only??? change this
					sum += mrew.get(0).get(col);	
				}			
				res = (res > sum)? sum : res;
			}		
			return res;
		}
		else if(ncact == 1) {
			//System.out.println("$$ number of col actions = 1"); //problem here with the distributions for the 
			res = Double.NEGATIVE_INFINITY;
			for(int row = 0; row < mdist.size(); row++) {
				sum = 0.0;
				for(int t : mdist.get(row).get(0).getSupport()) {
					sum += mdist.get(row).get(0).get(t)*sol[t];
				}
				if(rew) { //action rewards, what about the case of state rewards only??? change this
					sum += mrew.get(row).get(0);	
				}
				if(res < sum) {
					if(genadv) {
						adv.get(s).clear();
						adv.get(s).set(row, 1.0);
					}
				}
				res = (res < sum)? sum : res;
			}
			return res;
		}
		else {
			//System.out.println("$$ state " + s + " is concurrent");
			buildLPLpsolve(lp, mdist, mrew, sol, rew, min1, min2, false);
			if(lp.solve() == LpSolve.OPTIMAL) {
				res = lp.getObjective();
				double[] v = new double[maxRows + maxCols];
				lp.getVariables(v);
				//System.out.println("-- v " + Arrays.toString(v));
				if(genadv) {
					adv.get(s).clear();
					for(int row = 1; row <= nract; row++) {
						d.set(row-1, v[row]);
						adv.put(s, d);
					}
				}
			}
			else {
				System.out.println(lp.getStatus() == LpSolve.INFEASIBLE);
				throw new PrismException("Problem solving lp for state " + s);
			}
		}
		timer = System.currentTimeMillis() - timer;
		timerVal += timer;
		//System.out.println("$$$ distribution");
		//System.out.println(adv.get(s));
		return res;
	}
		
	public void buildLPCplex(IloMPModeler cplex, IloNumVar[][] nvar, IloRange[][] nrng, 
								ArrayList<ArrayList<Distribution>> mdist, 
								ArrayList<ArrayList<Double>> actrw, double[] nval, int mode) throws IloException {
		int nrows = mdist.size();
		int ncols = mdist.get(0).size();
		String[] vname = new String[nrows+ncols+2];
		vname[0] = "v";
		for(int i = 1; i <= nrows+1; i++) {
			vname[i] = "p" + i;
		}
		double[] lb = new double[nrows+1];
		double[] ub = new double[nrows+1];
		Arrays.fill(ub, 1.0);
		Arrays.fill(lb, 0.0);
		if(mode == 1) {
			lb[0] = Double.NEGATIVE_INFINITY; 
			ub[0] = Double.POSITIVE_INFINITY;
		}
		IloNumVar[] nvars = cplex.numVarArray(nrows+1, lb, ub, vname);
		IloNumExpr[] nsum = new IloNumExpr[1];
		IloNumExpr nrst;
		nvar[0] = nvars;
		cplex.addMaximize(nvars[0]);
		nrng[0] = new IloRange[((int) Math.max(nrows, ncols))+1]; 
		nsum[0] = cplex.sum(nvars[0], 0.0); //does not need to be an array
		for(int j = 0; j < ncols; j++) {
			for(int i = 0; i < nrows; i++) {
				for(int t : mdist.get(i).get(j).getSupport()){
					nsum[0] = cplex.sum(nsum[0], cplex.prod(-mdist.get(i).get(j).get(t)*nval[t], nvars[i+1]));
				}
				if(mode == 1) {
					nsum[0] = cplex.sum(nsum[0], cplex.prod(-actrw.get(i).get(j), nvars[i+1]));
				}
			}
			nrng[0][j] = cplex.addLe(nsum[0], 0.0);
			nsum[0] = cplex.sum(nvars[0], 0.0);
		}
		nrst = nvars[1];
		for(int p = 2; p < nvars.length; p++) {
			nrst = cplex.sum(nrst, nvars[p]);
		}
		nrng[0][nvars.length-1] = cplex.addEq(nrst, 1.0);
	}

	public void buildLPLpsolve(LpSolve lp, 
								ArrayList<ArrayList<Distribution>> mdist, 
								ArrayList<ArrayList<Double>> actrw, double[] nval,
								boolean mode, boolean min1, boolean min2, boolean pre) throws LpSolveException {	
		int nrows = mdist.size(); // number of rows
		int ncols = mdist.get(0).size(); // number of columns
		int[] vari = new int[nrows+1]; // indexes of variables, should be m + 1 for a m x n matrix
 		double[] row = new double[nrows+1];
		lp.setColName(1, "v"); // v
		// sets bound for each p variable
		for(int i = 2; i <= nrows+1; i++) {
			lp.setBounds(i, 0.0, 1.0); 
			lp.setColName(i, "p" + (i-1));
		}
		// precomputation mode
		if(pre) {
			lp.setBounds(1, 1.0, 1.0);
		}
		// rewards mode
		if(mode) {
			lp.setBounds(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		}
		lp.setAddRowmode(true); 
		int k = 0;
		// build each column considering the n+1 restrictions as a matrix
		for(int j = 0; j < ncols; j++) {
			vari[k] = 1;
			row[k] = 1;
			for(int i = 0; i < nrows; i++) {
				k++;
				vari[k] = k+1;
				row[k] = 0;
				for(int t : mdist.get(i).get(j).getSupport()){
					row[k] += -mdist.get(i).get(j).get(t)*nval[t];
				}
				if(mode) {
					if(actrw.size() > i) {
						if(actrw.get(i).size() > j) {
							row[k] += -actrw.get(i).get(j);
						}
						else {
							//should throw an exception
						}
					}
					else {
						//should throw an exception
					}
				}
			}
			if(pre) {
				lp.addConstraintex(nrows+1, row, vari, LpSolve.EQ, 0.0);
			}
			else {
				lp.addConstraintex(nrows+1, row, vari, LpSolve.LE, 0.0);
			}

			//if(!min1 && min2) {
			//	lp.addConstraintex(nrows+1, row, coln, LpSolve.LE, 0.0);
			//}
			//if(min1 && !min2) {
			//	lp.addConstraintex(nrows+1, row, coln, LpSolve.GE, 0.0);
			//}
			k = 0;
		}
		k = 0;
		for(k = 0; k < nrows+1; k++) {
			vari[k] = k + 1;
			row[k] = (k > 0)? 1 : 0;
		}
		lp.addConstraintex(k, row, vari, LpSolve.EQ, 1.0);
		k = 0;
		lp.setAddRowmode(false);
		for(k = 0; k < nrows+1; k++) {
			vari[k] = k + 1;
			row[k] = (k == 0)? 1 : 0;
		}
		lp.setObjFnex(k, row, vari);
		if(!pre) {
			lp.setMaxim();
		}
		//lp.printLp();
		//System.out.println("\n$$ buildLPsolve check");	
	}
		
	public double[][] preCompNashReachProb(CSG csg, BitSet target1, BitSet target2, ArrayList<Strategy> strat) throws PrismException {
		int i;
		double[][] init = new double[2][csg.getNumStates()];
		
		ModelCheckerResult res1, res2;
		
		/*** SHOULD IT BE ONLY OR TARGET ? ***/
		//System.out.println("-- preCompNashReachProb");
		
		if(strat != null)
			mdp_mc.setGenStrat(true);
		
		// prob reach only1
		res1 = mdp_mc.computeReachProbs((MDP) csg, target1, false);
		//System.out.println(Arrays.toString(res1.soln));
		
		// prob reaxh only2
		res2 = mdp_mc.computeReachProbs((MDP) csg, target2, false);
		//System.out.println(Arrays.toString(res2.soln));
		
		if(strat != null) {
			strat.add(0, res1.strat);
			strat.add(1, res2.strat);
		}
		
		init[0] = res1.soln;
		init[1] = res2.soln;

		return init;
	}
	
	public double[][] preCompNashReachRew(CSG csg, CSGRewards rewards1, CSGRewards rewards2, BitSet target1, BitSet target2,
											ArrayList<Strategy> strat) throws PrismException {
		double[][] init = new double[2][csg.getNumStates()];

		ModelCheckerResult res1, res2;
		
		/*** SHOULD IT BE ONLY OR TARGET ? ***/
		/*
		System.out.println("-- preCompNashReachRew");
		System.out.println("-- CSG rewards1");
		System.out.println(rewards1);
		System.out.println("-- CSG rewards2");
		System.out.println(rewards2);
		System.out.println("-- MDP rewards1");
		System.out.println((MDPRewards) rewards1);
		System.out.println("-- MDP rewards2");
		System.out.println((MDPRewards) rewards2);
		
		System.out.println("-- target1 " + target1);
		System.out.println("-- target2 " + target2);

		Path currentRelativePath = Paths.get("");
		String path = currentRelativePath.toAbsolutePath().toString();
		PrintStream printStream; 
		PrismLog logout;
		
		try {
			printStream = new PrintStream(new FileOutputStream(path + "/adv.dot", false));
			logout = new PrismPrintStreamLog(printStream);
			((MDP) csg).exportToDotFile(logout, null, true);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		*/
		
		BitSet prob01, prob02;
		prob01 = mdp_mc.prob0((MDP) csg, null, target1, false, null);
		prob02 = mdp_mc.prob0((MDP) csg, null, target2, false, null);
		//System.out.println("-- prob01 " + prob01);		
		//System.out.println("-- prob02 " + prob02);
		
		if(strat != null)
			mdp_mc.setGenStrat(true);
		
		// rew reach only1
		res1 = mdp_mc.computeReachRewards((MDP) csg, (MDPRewards) rewards1, target1, false);
		//System.out.println(Arrays.toString(res1.soln));
		
		// rew reach only2
		res2 = mdp_mc.computeReachRewards((MDP) csg, (MDPRewards) rewards2, target2, false);
		//System.out.println(Arrays.toString(res2.soln));
		
		if(strat != null) {
			strat.add(0, res1.strat);
			strat.add(1, res2.strat);
		}
		
		init[0] = res1.soln;
		init[1] = res2.soln;
		
		//System.out.println("-- res1 " + Arrays.toString(res1.soln));
		//System.out.println("-- res2 " + Arrays.toString(res2.soln));
		
		for(int s = prob01.nextSetBit(0); s < prob01.length() && s > 0; s = prob01.nextSetBit(s+1)) {
			init[0][s] = 0.0;
		}
		
		for(int s = prob02.nextSetBit(0); s < prob02.length() && s > 0; s = prob02.nextSetBit(s+1)) {
			init[1][s] = 0.0;	
		}
		
		return init;
	}
	
	public ModelCheckerResult computeNashReachProb(CSG csg, BitSet target1, BitSet target2, Coalition coalition) throws PrismException {
		long timer = System.currentTimeMillis();
		ModelCheckerResult res = new ModelCheckerResult();

		//HAVE TO ADD MINMAX CHECK
		//HAVE TO REVISE SETCOALITION TO ALLOW FOR BIMAXTRIX
		boolean genAdv = false;
		ArrayList<Strategy> mdpStrat = null;
		
		if(csg.getLabels().contains("adv")) 
			genAdv = true;
		
		setCoalition(csg, null, coalition, false, true);
		
		if(genAdv)
			mdpStrat = new ArrayList<Strategy>();
		
		double init[][] = preCompNashReachProb(csg, target1, target2, mdpStrat);
		
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nMDP computation took: " + timer/1000.00 + " seconds.");
		
		BitSet known = (BitSet) target1.clone();
		known.or(target2);
		
		CSGModelCheckerAux csg_aux = new CSGModelCheckerAux(this);
		res.soln = csg_aux.computeEquilibria(csg, null, null, mdpStrat, target1, target2, known, init, maxIters, genAdv);
		
		//System.out.println(Arrays.toString(res.soln));
		
		return res;
	}
	
	public ModelCheckerResult computeNashReachRew(CSG csg, CSGRewards rewards1, CSGRewards rewards2, BitSet target1, BitSet target2, Coalition coalition) throws PrismException {
		long timer = System.currentTimeMillis();
		ModelCheckerResult res = new ModelCheckerResult();

		//HAVE TO ADD MINMAX CHECK
		//HAVE TO REVISE SETCOALITION TO ALLOW FOR BIMAXTRIX
		boolean genAdv = false;
		ArrayList<Strategy> mdpStrat = null;
		
		if(csg.getLabels().contains("adv")) 
			genAdv = true;
		
		setCoalition(csg, null, coalition, false, true);
		
		if(genAdv)
			mdpStrat = new ArrayList<Strategy>();
		
		double init[][] = preCompNashReachRew(csg, rewards1, rewards2, target1, target2, mdpStrat);
		
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nMDP computation took: " + timer/1000.00 + " seconds.");
		
		BitSet known = (BitSet) target1.clone();
		known.or(target2);
		
		CSGModelCheckerAux csg_aux = new CSGModelCheckerAux(this);
		res.soln = csg_aux.computeEquilibria(csg, rewards1, rewards2, mdpStrat, target1, target2, known, init, maxIters, genAdv);
		
		//System.out.println(Arrays.toString(res.soln));
		
		return res;
	}
	
	public ModelCheckerResult computeNashBoundedCumulRew(CSG csg, CSGRewards rewards1, CSGRewards rewards2, Coalition coalition, int k) throws PrismException {
		ModelCheckerResult res = new ModelCheckerResult();
		
		//HAVE TO ADD MINMAX CHECK
		//HAVE TO REVISE SETCOALITION TO ALLOW FOR BIMAXTRIX
		boolean genAdv = false;
		if(csg.getLabels().contains("adv")) 
			genAdv = true;
		
		setCoalition(csg, null, coalition, false, true);
		
		CSGModelCheckerAux csg_aux = new CSGModelCheckerAux(this);
		res.soln = csg_aux.computeEquilibria(csg, rewards1, rewards2, null, null, null, null, null, k, genAdv);
		//res.soln = csg_aux.computeRewBoundedEquilibria(csg, rewards1, rewards2, k, genAdv);
		
		return res;
	}
	
	public ModelCheckerResult computeBoundedUntilProbs(CSG csg, BitSet remain, BitSet target, int k, boolean min1, boolean min2, Coalition coalition) throws PrismException{
		return computeBoundedReachProbs(csg, remain, target, k, min1, min2, null, null, coalition, false);
	}
		
	public ModelCheckerResult computeUntilProbs(CSG csg, BitSet remain, BitSet target, boolean min1, boolean min2, Coalition coalition) throws PrismException {
		return computeReachProbs(csg, remain, target, min1, min2, null, null, maxIters, coalition, false);
	}
		
	public ModelCheckerResult computeUntilProbs(CSG csg, BitSet remain, BitSet target, boolean min1, boolean min2, int bound, Coalition coalition) throws PrismException {
		return computeReachProbs(csg, remain, target, min1, min2, null, null, bound, coalition, false);
	}	
		
	public ModelCheckerResult computeNextProbs(CSG csg, BitSet target, boolean min1, boolean min2, Coalition col) throws PrismException {		
		mainLog.println("\nCSG Model Checker Next");
		ModelCheckerResult res = null;
		int n;
		double soln[], soln2[];
		long timer;
		timer = System.currentTimeMillis();

		n = csg.getNumStates();
		
		// Create/initialise solution vector(s)
		soln = Utils.bitsetToDoubleArray(target, n);
		soln2 = new double[n];
				
		setCoalition(csg, null, col, min1, min2);
		
		soln2 = computeReachProbsValIter(csg, target, null, null, 1, 1, min1, min2, false);
		
		// Store results/strategy
		res = new ModelCheckerResult();
		res.soln = soln2;
		res.numIters = 1;
		res.timeTaken = timer / 1000.0;
		
		return res;
	}
	
	public ModelCheckerResult computeReachProbs(CSG csg, BitSet remain, BitSet target, boolean min1, boolean min2, 
												double init[], BitSet known, int bound, Coalition coalition, boolean genAdv)
												throws PrismException { // check deal with bound and qualitative verification
		ModelCheckerResult res = null;
		BitSet no, yes;
		int n, numYes, numNo;
		long timer, timerProb0, timerProb1;
		double[] soln;
		
		setCoalition(csg, null, coalition, min1, min2);
		setMaxRowsCols(csg);
		
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("\nStarting probabilistic reachability...");

		// Store num states
		n = csg.getNumStates();

		/*
		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
			}
		}
		target = targetNew;
		}
		 */

		BitSet all = new BitSet();
		all.set(0, csg.getNumStates());

		no = new BitSet();
		yes = new BitSet();

		//System.out.println("-- remain " + remain);
		
		/*** THERE'S A BUG WITH PRECOMPUTATION AND UNBOUDED UNTIL ***/
		/*
		// Precomputation
		timerProb0 = System.currentTimeMillis();
		if (precomp && prob0) {
			try {
				if(!min1) {
					setCoalition(csg, null, coalition, true, false);
					setMaxRowsCols(csg);
					no.or(target);
					no.flip(0, n);
					no = sureSafe(csg, no);
					setCoalition(csg, null, coalition, min1, min2); // restores coalition
				}
				else {
					no.or(target);
					no.flip(0, n);
					no = sureSafe(csg, no);
				} 
			} catch (LpSolveException e) {
				e.printStackTrace();
			}
			//System.out.println("-- no " + no);
			//no = prob0(csg, all, target, min1, min2);
			//no.clear();
		} else {
			no = new BitSet();
		}
		timerProb0 = System.currentTimeMillis() - timerProb0;
		timerProb1 = System.currentTimeMillis();
		if (precomp && prob1) {
			try {				
				if(!min1) { //
					yes = sureReach(csg, target);
				}
				else {
					setCoalition(csg, null, coalition, true, false);
					setMaxRowsCols(csg);
					yes = sureReach(csg, target);
					setCoalition(csg, null, coalition, min1, min2); //restores coalition
				} 
			} catch (LpSolveException e) {
				e.printStackTrace();
			}
			//System.out.println("-- yes " + yes);
			//yes.clear();
		} else {
			yes = (BitSet) target.clone();
		}
		timerProb1 = System.currentTimeMillis() - timerProb1;
		*/
		
		no = (BitSet) all.clone();
		no.andNot(target);
		no.andNot(remain);
		
		yes = (BitSet) target.clone();
		
		setMaxRowsCols(csg);
		
		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();

		if (verbosity >= 1)
			mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));
				
		//if(bound < 1.0 || !(precomp && prob1)) {
			switch (solnMethod) {
			case VALUE_ITERATION:
				soln = computeReachProbsValIter(csg, target, no, yes, 1, bound, min1, min2, genAdv); //change to max iteration value;
				res = new ModelCheckerResult();
				res.soln = soln;
				break;
			default:
				throw new PrismException("Unknown CSG solution method " + solnMethod);
			}
		//}
		//else {
		//	res = new ModelCheckerResult();
		//	res.numIters = 0;
		//	res.soln = new double[n];
			for (int k = 0; k < n; k++)
		//		res.soln[k] = (yes.get(k)) ? 1.0 : 0.0;
		//	mainLog.println("Bound is 1, hence I am skipping the computation of other values than 1.");
		//}
		
		// Finished probabilistic reachability
		timer = System.currentTimeMillis() - timer;
		//if (verbosity >= 1)
		//	mainLog.println("Probabilistic reachability took " + timer / 1000.0 + " seconds.");
		// Update time taken
		res.timeTaken = timer / 1000.0;
		//res.timeProb0 = timerProb0 / 1000.0;
		//res.timePre = (timerProb0 + timerProb1) / 1000.0;
		//mainLog.println("Precomputation took " + (timerProb0 + timerProb1) / 1000.0 + " seconds.");
		return res;
	}	
	
	public ModelCheckerResult computeNashBoundedReachProbs(CSG csg, Coalition coalition, BitSet remain, 
												BitSet target1, BitSet target2, double init[][], int k1, int k2, 
												boolean min1, boolean min2, boolean genAdv) throws PrismException {
		CSGModelCheckerAux csg_aux = new CSGModelCheckerAux(this);
		ModelCheckerResult res = null;
		int i, n;
		double soln[];
		long timer;
		
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("\nStarting (Nash) bounded probabilistic reachability for CSGs...");
		
		n = csg.getNumStates();
		soln = new double[n];
		
		if(csg.getLabels().contains("adv")) 
			genAdv = true;
		
		BitSet all = new BitSet();
		BitSet known = new BitSet();
		all.set(0, n);
		all.andNot(remain);
		
		if(!all.isEmpty()) {
			throw new PrismException("Only F<= k supported at this time");
		}
		
		setCoalition(csg, null, coalition, false, true);
				
		known = (BitSet) target1.clone();
		known.or(target2);
				
		switch (solnMethod) {
		case VALUE_ITERATION:
			soln = csg_aux.computeProbBoundedEquilibria(csg, target1, target2, known, null, k1, k2, genAdv);
			break;
		default:
			throw new PrismException("Unknown CSG solution method " + solnMethod);
		}
		
		timer = System.currentTimeMillis() - timer;

		res = new ModelCheckerResult();
		res.soln = soln;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}
	
	public ModelCheckerResult computeBoundedReachProbs(CSG csg, BitSet remain, BitSet target, int k, boolean min1, boolean min2, double init[],
															double results[], Coalition coalition, boolean genAdv) throws PrismException {
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[];
		long timer;

		// start bounded probabilistic reachability
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("\nStarting bounded probabilistic reachability for CSGs...");

		// store num states
		n = csg.getNumStates();

		// create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// initialise solution vectors. Use passed in initial vector, if present
		if (init != null) {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : init[i];
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 1.0 : 0.0;
		}

		/*** ADD PRECOMPUTATION HERE ***/
		
		BitSet all = new BitSet();
		all.set(0, n);
		BitSet no = new BitSet();
		BitSet yes = new BitSet();
		BitSet known = new BitSet();

		all.andNot(remain);
		
		if(!all.isEmpty()) {
			throw new PrismException("Only true U<=k is suported at this time");
		}
	
		all.andNot(target);
		
		setCoalition(csg, null, coalition, min1, min2); // restores coalition

		yes.or(target);
		//no.or(all);
		known.or(yes);
		known.or(no);

		//System.out.println("-- no " + no);
		//System.out.println("-- yes " + yes);
		//System.out.println("-- known " + known);

		/*** AT SOME POINT MAKE DIFFERENT METHODS FOR BOUNDED AND UNBOUNDED VALUE ITERATION TO SPEED UP CONVERGENCE ***/
		
		/*** ADD PRECOMPUTATION HERE ***/
		
		switch (solnMethod) {
		case VALUE_ITERATION:
			soln = computeReachProbsValIter(csg, target, no, yes, 1, k,  min1, min2, genAdv);
			break;
		default:
			throw new PrismException("Unknown CSG solution method " + solnMethod);
		}

		// finished bounded probabilistic reachability
		timer = System.currentTimeMillis() - timer;

		// store results/strategy
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		//res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}

	public double[] computeReachProbsValIter(CSG csg, BitSet target, BitSet no, BitSet yes, int method, int limit, 
												boolean min1, boolean min2, boolean genAdv) throws PrismException {
		int ni = 0;
		double[] nsol = new double[csg.getNumStates()];
		double[] ntmp = new double[csg.getNumStates()];
		Arrays.fill(nsol, 0.0);
		Arrays.fill(ntmp, 0.0);

		IloCplex cplex;
		IloRange[][] nrng;
		IloNumVar[][] nvar;    

		LpSolve lp;

		for(int i = 0; i < nsol.length; i++) {
			if(target.get(i)) {
				nsol[i] = 1.0;
			}
			if(yes !=null) {
				if(yes.get(i))
					nsol[i] = 1.0;
			}
		}

		boolean done = true;
		//double[] x;

		BitSet uknown = new BitSet();
		uknown.set(0, csg.getNumStates());

		if(no != null)
			uknown.andNot(no);
		if(yes !=null)	
			uknown.andNot(yes);

		adv = new HashMap<Integer,Distribution>();
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		
		if(csg.getLabels().contains("adv")) 
			genAdv = true;

		setMaxRowsCols(csg);
		
		try {
			lp = LpSolve.makeLp(0, maxRows + maxCols);
			lp.setVerbose(LpSolve.IMPORTANT);	
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new PrismException(e.toString());
		}
				
		ntmp = Arrays.copyOf(nsol, nsol.length);
		
		while(true) {     
			//System.out.println("## i: " + ni);
			for(int s = 0; s < csg.getNumStates(); s++) {  
				//System.out.println("-- s " + s);
				if(uknown.get(s)) {
					if(method == 0) {
						try {
							cplex = new IloCplex();
							cplex.setOut(null);
							nrng = new IloRange[1][];
							nvar = new IloNumVar[1][];  
							mdist = buildProbMatrixGame(csg, null, s);
							buildLPCplex(cplex, nvar, nrng, mdist, null, nsol, 0);      
							if (cplex.solve()) {
								nsol[s] = cplex.getObjValue();
								//System.out.println("-- s " + s + " " + nsol[s]);
								//cplex.output().println("Solution status = " + cplex.getStatus());
								//cplex.output().println("Solution value  = " + cplex.getObjValue())	
								//x = cplex.getValues(nvar[0]);
								//for (int j = 0; j < x.length; j++) {
								//	System.out.println("Variable " + j + ": Value = " + x[j]);
								//} 
							}
							cplex.clearModel();
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					if(method == 1) {
						if(genAdv)
							adv.put(s, new Distribution());
						mdist = buildProbMatrixGame(csg, null, s);
						try {
							lp.resizeLp(0, maxRows + maxCols);
							nsol[s] = val(lp, mdist, null, adv, ntmp, s, false, min1, min2, genAdv);
						}
						catch(Exception e) {
							e.printStackTrace();
							throw new PrismException(e.toString());
						}
					}
				}
			}
			ni++;
			
			done = done & PrismUtils.doublesAreClose(nsol, ntmp, termCritParam, termCrit == TermCrit.RELATIVE); // change to Prism's epsilon

			if(done || ni == limit) {
				break;
			}
			else {
				done = true;
				ntmp = Arrays.copyOf(nsol, nsol.length);
			}
		}
		/*
		if(genAdv) {
			System.out.println("-- strategy");
			//System.out.println(adv);
			try {
				strategyProductProb(csg, target, adv);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			System.out.println("-- end strategy");	
		}
		*/		
		return nsol;
	}	
	
	/**
	 * Compute expected reachability rewards, where the runs that don't reach
	 * the final state get infinity. i.e. compute the min/max reward accumulated
	 * to reach a state in {@code target}.
	 * @param csg The CSG
	 * @param rewards The rewards
	 * @param target Target states
	 * @param min1 Min or max probabilities for player 1 (true=min, false=max)
	 * @param min2 Min or max probabilities for player 2 (true=min, false=max)
	 * @param coalition The coalition of players which define player 1
	 */
	public ModelCheckerResult computeReachRewards(CSG csg, CSGRewards rewards, BitSet target, int unreachingSemantics, boolean min1, boolean min2, Coalition coalition) throws PrismException {		
		return computeReachRewards(csg, coalition, rewards, target, min1, min2, null, null, unreachingSemantics);
	}
	
	public ModelCheckerResult computeReachRewards(CSG csg, Coalition coalition, CSGRewards rewards, BitSet target, boolean min1, boolean min2, double init[], BitSet known,
												int unreachingSemantics) throws PrismException {
		switch (unreachingSemantics) {
		case R_INFINITY:
			return computeReachRewardsInfinity(csg, coalition, rewards, target, min1, min2, init, known);
		case R_CUMULATIVE:
			return computeReachRewardsCumulative(csg, coalition, rewards, target, min1, min2, init, known, false);
		case R_ZERO:
			throw new PrismException("F0 is not yet supported for CSGs.");
		default:
			throw new PrismException("Unknown semantics for runs unreaching the target in CSGModelChecker: " + unreachingSemantics);
		}
	}

	public ModelCheckerResult computeTotalRewards(CSG csg, CSGRewards csgRewards, MinMax minMax) throws PrismException {
		return computeTotalRewards(csg, csgRewards, minMax.getCoalition(), minMax.isMin1(), minMax.isMin2());
	}
	
	public ModelCheckerResult computeInstantaneousRewards(CSG csg, CSGRewards csgRewards, int k, boolean min1, boolean min2, Coalition coalition) throws PrismException {
		ModelCheckerResult res = null;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		long timer;

		n = csg.getNumStates();

		timer = System.currentTimeMillis();
		mainLog.println("\nStarting backwards instantaneous rewards computation...");
		
		// Create solution vector(s)
		soln = new double[n];
		soln2 = new double[n];

		// Initialise solution vectors.
		for (i = 0; i < n; i++)
			soln[i] = csgRewards.getStateReward(i);
		
		setCoalition(csg, csgRewards, coalition, min1, min2);
		
		LpSolve lp;
		
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
		
		for (iters = 0; iters < k; iters++) {
			// Val computation			
			for(int s = 0; s < csg.getNumStates(); s++) {
				mdist.clear();
				mrew.clear();
				mdist = buildProbMatrixGame(csg, null, s);
				for(int row = 0; row < mdist.size(); row++) {
					mrew.add(row, new ArrayList<Double>());
					for(int col = 0; col < mdist.get(row).size(); col ++) {
						mrew.get(row).add(col, 0.0);
					}
				}
				try {
					lp = LpSolve.makeLp(0, mdist.size() + 1);
					lp.setVerbose(LpSolve.IMPORTANT);
					soln2[s] = val(lp, mdist, mrew, null, soln, s, true, false, false, false);
					lp.deleteLp();
				}
				catch(Exception e) {
					e.printStackTrace();
					throw new PrismException(e.toString());
				}
			}
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}
		
		timer = System.currentTimeMillis() - timer;
		mainLog.print("Backwards transient instantaneous rewards computation");
		mainLog.println(" took " + iters + " iters and " + timer / 1000.0 + " seconds.");

		// Return results
		res = new ModelCheckerResult();
		res.soln = soln;
		res.lastSoln = soln2;
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		res.timePre = 0.0;
		return res;
	}
		
	/**
	 * Computes the reachability reward under the semantics where nonreaching
	 * runs get infinity.
	 * 
	 * @param csg
	 * @param rewards
	 * @param target
	 * @param min1
	 * @param min2
	 * @param init
	 * @param known
	 * @return
	 * @throws PrismException
	 */
	public ModelCheckerResult computeReachRewardsInfinity(CSG csg, Coalition coalition, CSGRewards rewards, BitSet target, boolean min1, boolean min2, double init[], BitSet known)
														throws PrismException {
		ModelCheckerResult res = new ModelCheckerResult();
		BitSet inf;
		int n, numTarget, numInf;
		double[] sol;
		long timer, timerProb1, timerApprox;
		
		// Start expected reachability
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("\nStarting expected reachability...");

		// Store num states
		n = csg.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()) {
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}
		BitSet all = new BitSet();
		all.set(0, n);
		
		timerProb1 = System.currentTimeMillis();
		
		BitSet arew = new BitSet();	// is this really needed here??
		for(int s = 0; s < n; s++) {
			if(rewards.getStateReward(s) > 0) {
				arew.set(s);
			}
		}
		arew.flip(0, n); // states with zero rewards
		
		//what about negative rewards?
		/*** regular procedure ***/
		try {
			if(!min1) { //if max
				setCoalition(csg, null, coalition, true, false);
			}
			else {
				setCoalition(csg, null, coalition, false, true);
			}
			inf = sureReach(csg, target); 
			inf.flip(0, n);
			//System.out.println("-- inf " + inf);
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		}
		
		/*** globally not reach target ***/
		/*
		BitSet gnt = (BitSet) target.clone();
		gnt.flip(0, n);
		
		try {
			if(min1) {
				setCoalition(csg, coalition, false, true);
			}
			else {
				setCoalition(csg, coalition, true, false);	
			}
			gnt.and(arew);
			gnt = globallyB(csg, gnt);
			System.out.println("zrew=" + arew.cardinality());
			System.out.println("gnt=" + gnt.cardinality());
			//gnt.and(arew);
			//System.out.println("zrew and gnt=" + gnt.cardinality());
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		}
		*/
		/*** not globally eventually arew ***/
		/*
		BitSet gnp;
		
		try {
			if(min1) { //if max
				setCoalition(csg, coalition, false, true);
			}
			else {
				setCoalition(csg, coalition, true, false);
			}
			//arew.flip(0, n); // positive rewards
 			gnp = pre2(csg, arew);
 			//gnp.flip(0, n);
			//System.out.println("prew=" + arew.cardinality());
			System.out.println("gnp=" + gnp.cardinality());
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		}
		*/
					
		timerProb1 = System.currentTimeMillis() - timerProb1;
	
		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		if (verbosity >= 1)
			mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		// compute rewards with epsilon instead of zero. This is used to get the
		// over-approximation
		// of the real result, which deals with the problem of staying in zero
		// components for free
		// when infinity should be gained.

		// first, get the minimum nonzero reward and maximal reward, will be
		// used as a basis for epsilon
		// also, check if by any chance all rewards are nonzero, then we don't
		// need to precompute
		double minimumReward = Double.POSITIVE_INFINITY;
		double maximumReward = 0.0;
		boolean allNonzero = true;
		double r;
		for (int i = 0; i < n; i++) {
			r = rewards.getStateReward(i);
			if (r > 0.0 && r < minimumReward)
				minimumReward = r;
			if (r > maximumReward)
				maximumReward = r;
			allNonzero = allNonzero && r > 0;

			for (int j = 0; j < csg.getNumChoices(i); j++) {
				r = rewards.getTransitionReward(i, j);
				if (r > 0.0 && r < minimumReward)
					minimumReward = r;
				if (r > maximumReward)
					maximumReward = r;
				allNonzero = allNonzero && rewards.getTransitionReward(i, j) > 0;

				for (int k = 0; k < csg.getNumNestedChoices(i, j); k++) {
					r = rewards.getNestedTransitionReward(i, j, k);
					if (r > 0.0 && r < minimumReward)
						minimumReward = r;
					if (r > maximumReward)
						maximumReward = r;
					allNonzero = allNonzero && r > 0;
				}
			}
		}
		
		if(!min1 && !inf.isEmpty()) { // see about this
			for(int s = 0;  s < csg.getNumStates(); s++) {
				if(inf.get(s)) {
					for(int c = 0; c < csg.getNumChoices(s); c++) {
						if(csg.getChoice(s, c).isSubsetOf(inf)) {
							mainLog.println("Action reward of " + csg.getAction(s, c) + " set to zero.");
							((CSGRewardsSimple) rewards).setTransitionReward(s, c, 0.0);
						}
					}
				}
			}
		}
		
		if (!allNonzero && !(rewards instanceof StateRewardsConstant) && min1) {
			timerApprox = System.currentTimeMillis();
			// a simple heuristic that gives small epsilon, but still is
			// hopefully safe floating-point-wise
			double epsilon = Math.min(minimumReward, maximumReward * 0.01);

			if (verbosity >= 1) {
				mainLog.println("Computing the upper bound where " + epsilon + " is used instead of 0.0");
			}

			// modify the rewards
			double origZeroReplacement;
			if (rewards instanceof MDPRewardsSimple) {
				origZeroReplacement = ((MDPRewardsSimple) rewards).getZeroReplacement();
				((MDPRewardsSimple) rewards).setZeroReplacement(epsilon);
			} else {
				throw new PrismException("To compute expected reward I need to modify the reward structure. But I don't know how to modify"
										+ rewards.getClass().getName());
			}
			
			//System.out.println("-- modified rewards ");
			//for(int s = 0; s < csg.getNumStates(); s++) {
			//	System.out.println("-- state " + s);
			//	System.out.println(rewards.getStateReward(s));
			//}
						
			setCoalition(csg, rewards, coalition, min1, min2); // restores coalition
			
			// compute the value when rewards are nonzero
			switch (solnMethod) {
			case VALUE_ITERATION:
				//res = computeReachRewardsValIter(stpg, rewards, target, inf, min1, min2, init, known);
				init = computeReachRewardsValIter(csg, rewards, coalition, target, inf, min1, min2, init, null, null, 1, maxIters, false);
				break;
			default:
				throw new PrismException("Unknown CSG solution method " + solnMethod);
			}
			
			// set the value iteration result to be the initial solution for the
			// next part in which "proper" zero rewards are used
			
			// return the rewards to the original state
			if (rewards instanceof MDPRewardsSimple) {
				((MDPRewardsSimple) rewards).setZeroReplacement(origZeroReplacement);
			}

			timerApprox = System.currentTimeMillis() - timerApprox;

			if (verbosity >= 1) {
				mainLog.println("Computed an over-approximation of the solution (in " + timerApprox / 1000
						+ " seconds), this will now be used to get the solution");
			}
		
			// System.out.println("-- modified rewards ");
			// for(int s = 0; s < csg.getNumStates(); s++) {
			//	System.out.println("-- state " + s);
			//	System.out.println(rewards.getStateReward(s));
			//}	
		}
		
		//inf = (BitSet) gnp.clone();
		/*	 			
		for(int s = 0; s < n; s++) {
			if(inf.get(s)) {
				((CSGRewardsSimple) rewards).setStateReward(s, Double.POSITIVE_INFINITY);
			}
			System.out.println(rewards.getStateReward(s));
		}
		*/
		
		setCoalition(csg, rewards, coalition, min1, min2); // restores coalition
		
		// Compute real rewards
		switch (solnMethod) {
		case VALUE_ITERATION:
			sol = computeReachRewardsValIter(csg, rewards, coalition, target, inf, min1, min2, init, null, null, 1, maxIters, false);
			break;
		default:
			throw new PrismException("Unknown CSG solution method " + solnMethod);
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		if (verbosity >= 1)
			mainLog.println("Expected reachability took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.soln = sol;
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;
		return res;
	}
	
	/**
	 * Computes the reachability reward under the semantics where nonreaching
	 * runs get their total cumulative reward (i.e. anything between 0 and
	 * infinity).
	 * 
	 * @param stpg
	 * @param rewards
	 * @param target
	 * @param min1
	 * @param min2
	 * @param init
	 * @param known
	 * @return
	 * @throws PrismException
	 */
	public ModelCheckerResult computeReachRewardsCumulative(CSG csg, Coalition coalition, CSGRewards rewards, BitSet target, boolean min1, boolean min2, double init[], BitSet known, boolean genAdv) throws PrismException {
		ModelCheckerResult res = new ModelCheckerResult();
		BitSet inf;
		int n;
		double[] sol;
		long timerPrecomp;
				
		// Start expected reachability
		if (verbosity >= 1)
			mainLog.println("\nStarting expected reachability...");

		// Store num states
		n = csg.getNumStates();
		
		BitSet arew = new BitSet();
		BitSet all = new BitSet();
		all.set(0, n);
		
		for(int s = 0; s < n; s++) {
			if(rewards.getStateReward(s) > 0) {
				arew.set(s);
			}
		}
		
		arew.flip(0, n); // states with zero rewards
		
		timerPrecomp = System.currentTimeMillis();
		//what about negative rewards?
		try {
			if(!min1) { //if max
				setCoalition(csg, null, coalition, true, false); // set of states from which player 2 can force zero rewards -- should be for max
			}
			else {
				setCoalition(csg, null, coalition, false, true); // set of states from which player 1 can force zero rewards -- should be for max
			}
			setMaxRowsCols(csg);
			inf = pre1(csg, arew); 
			inf.flip(0, n);
			//System.out.println("-- inf " + inf);
		
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		}
		timerPrecomp = System.currentTimeMillis() - timerPrecomp;
		
		setCoalition(csg, null, coalition, min1, min2); // restores coalition
		setMaxRowsCols(csg);
		
		sol = computeReachRewardsValIter(csg, rewards, coalition, target, inf, min1, min2, null, target, null, 1, maxIters, genAdv);
		
		mainLog.println("Precomputation took " + timerPrecomp/1000.00 + " seconds.");
		res.soln = sol;
		return res;
	}	
	
	public ModelCheckerResult computeCumulativeRewards(CSG csg, CSGRewards csgRewards, Coalition coalition, int k, boolean min1, boolean min2, boolean genAdv) throws PrismException {
		ModelCheckerResult res = new ModelCheckerResult();
		double soln[];
		/*
		System.out.println("-- Cumulative Bounded " + k);
		
		System.out.println("-- csgRewards");
		System.out.println(csgRewards);
		System.out.println("-- coalition");
		System.out.println(coalition);
		System.out.println("-- k " + k);
		System.out.println("-- min1 " + min1);
		System.out.println("-- min2 " + min2);
		*/
		setCoalition(csg, csgRewards, coalition, min1, min2);
		
		soln = computeReachRewardsValIter(csg, csgRewards, coalition, new BitSet(), new BitSet(), min1, min2, null, null, null, 1, k, genAdv);
				
		res.soln = soln;
		
		return res;
	}
	
	public ModelCheckerResult computeTotalRewards(CSG csg, CSGRewards rewards, Coalition coalition, boolean min1, boolean min2) throws PrismException {
		ModelCheckerResult res = new ModelCheckerResult();
		int n;
		long timer, timerPrecomp;
		BitSet inf;
		double[] sol;
		
		// start expected total reward
		timer = System.currentTimeMillis();
		mainLog.println("\nStarting total expected reward...");
		// Store num states
		n = csg.getNumStates();
		
		BitSet arew = new BitSet();
		BitSet all = new BitSet();
		all.set(0, n);
		
		for(int s = 0; s < n; s++) {
			if(rewards.getStateReward(s) > 0) {
				arew.set(s);
			}
		}
		
		//System.out.println("-- arew " + arew);
		
		arew.flip(0, n); // states with zero rewards
		timerPrecomp = System.currentTimeMillis();
		
		//System.out.println("-- arew " + arew);
		
		//what about negative rewards?
		try {
			if(!min1) { //if max
				setCoalition(csg, null, coalition, true, false); // set of states from which player 2 can force zero rewards -- should be for max
			}
			else {
				setCoalition(csg, null, coalition, false, true); // set of states from which player 1 can force zero rewards -- should be for max
			}
			setMaxRowsCols(csg);
			inf = pre1(csg, arew); 
			inf.flip(0, n);
			//System.out.println("-- inf " + inf);
		
		} catch (LpSolveException e) {
			throw new PrismException(e.getMessage());
		}
		timerPrecomp = System.currentTimeMillis() - timerPrecomp;
		
		setCoalition(csg, rewards, coalition, min1, min2); // restores coalition
		setMaxRowsCols(csg);

		// compute rewards
		// do standard max reward calculation, but with empty target set
		switch (solnMethod) {
		case VALUE_ITERATION:
			sol = computeReachRewardsValIter(csg, rewards, coalition, new BitSet(), inf, min1, min2, null, null, null, 1, maxIters, false);
			break;
		default:
			throw new PrismException("Unknown CSG solution method " + solnMethod);
		}

		// finished expected total reward
		timer = System.currentTimeMillis() - timer;
		mainLog.println("Precomputation took " + timerPrecomp / 1000.0 + " seconds.");
		mainLog.println("Expected total reward took " + timer / 1000.0 + " seconds.");

		// update time taken
		res.timeTaken = timer / 1000.0;
		// return results
		res.soln = sol;
		return res;
	}

	public void setMaxRowsCols(CSG csg) throws PrismException {		
		maxRows = 0;
		maxCols = 0;
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		for(int s = 0; s < csg.getNumStates(); s++) {
			mdist = buildProbMatrixGame(csg, null, s);
			maxRows = (mdist.size() > maxRows)? mdist.size() : maxRows;
			maxCols = (mdist.get(0).size() > maxCols)? mdist.get(0).size() : maxCols;	
		}
	}
	
	public double[] computeReachRewardsValIter(CSG csg, CSGRewards csgRewards, Coalition coalition,
												BitSet target, BitSet inf, boolean min1, boolean min2,
												double init[], BitSet known, int strat[],
												int method, int limit,
												boolean genAdv) throws PrismException {
		csg_model = csg;
		
		BitSet unknown, notInf;
		int i, n;
		double soln[], soln2[];
		long timer;
				
		// Start value iteration
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("Starting value iteration (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")...");

		// Store num states
		n = csg.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;
		
		if (init != null) {
			if (known != null) {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = known.get(i) ? init[i] : target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			} else {
				for (i = 0; i < n; i++)
					soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : init[i];
			}
		} else {
			for (i = 0; i < n; i++)
				soln[i] = soln2[i] = target.get(i) ? 0.0 : inf.get(i) ? Double.POSITIVE_INFINITY : 0.0;
		}
		
		// determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		// constructing not infinity set
		notInf = (BitSet) inf.clone(); // is this being used at all?
		notInf.flip(0, n);
		
		//System.out.println("-- soln" + Arrays.toString(soln));
		//System.out.println("-- soln2" + Arrays.toString(soln2));
		
		int ni = 0;
		double[] nval = new double[csg.getNumStates()];
		double[] nsol = new double[csg.getNumStates()];
		double[] ntmp = new double[csg.getNumStates()];
		Arrays.fill(nsol, 0.0);
		Arrays.fill(ntmp, 0.0);
		Arrays.fill(nval, 0.0);

		/*
		IloCplex cplex = new IloCplex();
		cplex.setOut(null);
		IloRange[][] nrng = new IloRange[1][];
		IloNumVar[][] nvar = new IloNumVar[1][];
		*/
		
		LpSolve lp;
		boolean done = true;
 		
		adv = new HashMap<Integer,Distribution>();
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
		
		if(init != null)
			nsol = init;
		
		double rew;
		Iterator<Integer> it;
		int t;
		
		if(csg.getLabels().contains("adv")) 
			genAdv = true;
				
		setMaxRowsCols(csg);
		
		try {
			lp = LpSolve.makeLp(0, maxRows + maxCols);
			lp.setVerbose(LpSolve.IMPORTANT);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new PrismException(e.toString());
		}
		
		timer = System.currentTimeMillis();
		while(true) {     			
			for(int s = 0; s < csg.getNumStates(); s++) {
				if(!target.get(s)) {
					rew = 0;
					//System.out.println("-- s " + s);
					if(method == 0) {
						//buildLP(cplex, nvar, nrng, smdist.get(s), actrew.get(s), nsol, nract[s], ncact[s], nract[s]+1, 1, flags[s]);        		
						//System.out.println("-- s " + s + " " + cplex.solve());
						//if (cplex.solve()) {
							//nsol[s] = cplex.getObjValue();	
							//System.out.println("-- s " + s + " " + nsol[s]);
							//cplex.output().println("Solution status = " + cplex.getStatus());
							//cplex.output().println("Solution value  = " + cplex.getObjValue());
							/*
							double[] x = cplex.getValues(nvar[0]);
							int nvars = x.length;
							for (int j = 0; j < nvars; ++j) {
								cplex.output().println("Variable " + j +
								": Value = " + x[j]);
							} 
							 */
							//if(s == map_states.get(initialstate)) {
								//System.out.println(nsol[s]);
							//}
						//}
						//cplex.clearModel();	
					}
					if(method == 1) {
						if(genAdv)
							adv.put(s, new Distribution());
						mrew = buildRewMatrixGame(csg, csgRewards, s);
						mdist = buildProbMatrixGame(csg, null, s);
						try {
							lp.resizeLp(0, maxRows + maxCols);
							// temporary solution because of problem with positive infinity when solving the lps
							if(soln[s] == Double.POSITIVE_INFINITY) { 
								it = csg.getSuccessorsIterator(s);
								while(it.hasNext()) {
									t = it.next();
									if(t != s)
										rew += soln[t];
								}
								soln[s] = rew;
							}
							soln2[s] = val(lp, mdist, mrew, adv, soln, s, true, false, false, genAdv);
						}
						catch(Exception e) {
							e.printStackTrace();
							throw new PrismException(e.toString());
						}
						/*
						if(inf.get(s)) {
							fr = 0.0;
							for(int r = 0; r < mdist.size(); r++) {
								for(int c = 0; c < mdist.get(r).size(); c++) {
									tr = 0.0;
									for(int t : mdist.get(r).get(c).getSupport()) {
										tr +=  mdist.get(r).get(c).get(t) * soln2[t];
									}
									fr = (fr < tr)? tr : fr;
								}
							}
							soln2[s] = fr + 0.1;
						}
						*/	
						if(unknown.get(s))
							soln2[s] += csgRewards.getStateReward(s); // is this the right place?
					}
				}
			}				
			ni++;
			done = done & PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.RELATIVE);
			//System.out.println("------");
			//System.out.println(ni + "ntmp: " + Arrays.toString(ntmp));
			//System.out.println(ni + "soln: " + Arrays.toString(soln));
			//System.out.println(ni + "soln2" + Arrays.toString(soln2));

			ntmp = soln;
			soln = soln2;
			soln2 = ntmp;
			
			if(!done && ni == maxIters) {
				throw new PrismException("Could not converge after " + maxIters + " iterations");
			}
			if(done || ni == limit) {
				break;
			}
			else {
				done = true;
			}
		}
		timer = System.currentTimeMillis() - timer;
		
		if(genAdv) {
			//System.out.println("-- strategy");
			//System.out.println(adv);
		
			try {
			//	strategyProductRew(csg, csgRewards, adv);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		
			//System.out.println("-- end strategy");	
		}		
		mainLog.println("Value iteration converged after " + ni + " iterations.");
		//mainLog.println("Time taken building prob matrix : " + timerBPM/1000.00 + ".");
		//mainLog.println("Time taken building rew matrix : " + timerBRM/1000.00 + ".");
		return soln;
	}	
	
	public boolean pre1(ArrayList<ArrayList<Distribution>> mdist, BitSet x) { // on actions
		boolean b1; 
		for(int row = 0; row < mdist.size(); row++) {
			b1 = true;
			for(int col = 0; col < mdist.get(0).size(); col++) {
				b1 = b1 && mdist.get(row).get(col).isSubsetOf(x);
			}
			if(b1) { // if there's a row action for which preach(x) = 1, returns true
				return b1;
			}
		}
		return false;
	}
	
	public boolean pre1(LpSolve lp, ArrayList<ArrayList<Distribution>> mdist, BitSet x, int n) throws LpSolveException { // on distributions
		boolean b1;
		if(mdist.size() > 1 && mdist.get(0).size() > 1) {
			lp.resizeLp(0, maxRows + maxCols); //review this
			//System.out.println("-- target set " + x);
			double[] val = new double[n];
			for(int s = 0; s < n; s++) {
				val[s] = (x.get(s))? 1.0 : 0.0;
			}
			buildLPLpsolve(lp, mdist, null, val, false, false, true, true);
			//lp.printLp();
			b1 = lp.solve() == LpSolve.INFEASIBLE;
		}
		else {
			return pre1(mdist, x);
		}
		//System.out.println(b1);
		return !b1; // if not infeasible, preach(x) = 1, returns true
	}
	
	public void pre1(CSG csg, BitSet x, BitSet y) throws PrismException, LpSolveException { // one step pre1
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		LpSolve lp;
		lp = LpSolve.makeLp(0, maxRows + maxCols); //review this
		lp.setVerbose(LpSolve.IMPORTANT);
		for(int s = 0; s < csg.getNumStates(); s++) {
			//System.out.println("-- s " + s);
			mdist = buildProbMatrixGame(csg, null,  s);
			y.set(s, pre1(lp, mdist, x, csg.getNumStates()));
		}
	}
	
	public BitSet pre1(CSG csg, BitSet b) throws PrismException, LpSolveException { // eventually globally b
		int n = csg.getNumStates();
		BitSet x, y, sol1, sol2;
		x = new BitSet(n);
		y = new BitSet(n);
		sol1 = new BitSet();
		sol2 = new BitSet();
		boolean done_x, done_y;
		done_x = false;
		done_y = false;
		while(!done_x) {
			done_y = false;
			y.clear();
			y.or(b);
			while(!done_y) {
				sol1.clear();
				sol2.clear();
				pre1(csg, y, sol1);
				sol1.and(b);
				pre1(csg, x, sol2);
				sol2.andNot(b);
				sol2.or(sol1);
				done_y = sol2.equals(y);
				y.clear();
				y.or(sol2);
			}
			done_x = x.equals(y);
			x.clear();
			x.or(y);
			//System.out.println("-- x " + x);
		}
		return x;
	}
	
	public BitSet pre2(CSG csg, BitSet b) throws PrismException, LpSolveException { // globally eventually b
		int n = csg.getNumStates();
		BitSet x, y, sol1, sol2;
		x = new BitSet(n);
		y = new BitSet(n);
		sol1 = new BitSet();
		sol2 = new BitSet();
		boolean done_x, done_y;
		done_x = false;
		done_y = false;
		y.set(0, n);
		while(!done_y) {
			done_x = false;
			x.clear();
			while(!done_x) {
				sol1.clear();
				sol2.clear();
				pre1(csg, y, sol1);
				sol1.and(b);
				pre1(csg, x, sol2);
				sol2.andNot(b);
				sol2.or(sol1);
				done_x = sol2.equals(x);
				x.clear();
				x.or(sol2);
			}
			done_y = x.equals(y);
			y.clear();
			y.or(x);
		}
		return y;
	}
		
	public BitSet sureReach(CSG csg, BitSet b) throws PrismException, LpSolveException { // eventually b
		BitSet x, t;
		x = new BitSet();
		t = new BitSet();
		boolean done = false; 
		x.or(b);
		while(!done) {
			t.clear();
			pre1(csg, x, t);
			t.or(b);
			done = x.equals(t);
			x.clear();
			x.or(t);
			//System.out.println("-- x " + x);
		}
		return x;
	}
	
	public BitSet sureSafe(CSG csg, BitSet b) throws PrismException, LpSolveException { // globally b
		BitSet x, t;
		x = new BitSet();
		t = new BitSet();
		boolean done = false;
		x.or(b);
		while(!done) {
			t.clear();
			pre1(csg, x, t);
			t.and(b);
			done = x.equals(t);
			x.clear();
			x.or(t);
			//System.out.println("-- x " + x);
		}
		return x;
	}
	
	public void compSelector(IloMPModeler cplex, IloNumVar[][] nvar, IloRange[][] nrng,
								ArrayList<ArrayList<Distribution>> mdist, ArrayList<ArrayList<Double>> actrw, 
								double[] nval, int mode) throws IloException {
		int nrows = mdist.size();
		int ncols = mdist.get(0).size();
		
		String[] vname = new String[nrows+ncols+2];
		vname[0] = "v";
		for(int i = 1; i <= nrows+1; i++) {
			vname[i] = "p" + i;
		}

		double[] lb = new double[nrows+1]; 
		double[] ub = new double[nrows+1];
		Arrays.fill(ub, 1.0);
		Arrays.fill(lb, 0.0);

		if(mode == 1) {
			lb[0] = Double.NEGATIVE_INFINITY; 
			ub[0] = Double.POSITIVE_INFINITY;
		}

		IloNumVar[] nvars = cplex.numVarArray(nrows+1, lb, ub, vname);
		IloNumExpr[] nsum = new IloNumExpr[1];
		IloNumExpr nrst;
		nvar[0] = nvars;

		cplex.addMaximize(nvars[0]);

		nrng[0] = new IloRange[nrows+1];
		nsum[0] = cplex.sum(nvars[0], 0.0); //does not need to be an array

		//System.out.println("##");

		for(int j = 0; j < ncols; j++) {
			for(int i = 0; i < nrows; i++) {
				for(int t : mdist.get(i).get(j).getSupport()){
					nsum[0] = cplex.sum(nsum[0], cplex.prod(-mdist.get(i).get(j).get(t)*nval[t], nvars[i+1]));
					if(mode == 1) {
						nsum[0] = cplex.sum(nsum[0], cplex.prod(-actrw.get(i).get(j), nvars[i+1]));
					}
				}
			}
			//System.out.println(nsum[0]);
			nrng[0][j] = cplex.addLe(nsum[0], 0.0);
			//System.out.println(nrng[0][j]);
			nsum[0] = cplex.sum(nvars[0], 0.0);
		}

		nrst = nvars[1];
		for(int p = 2; p < nvars.length; p++) {
			nrst = cplex.sum(nrst, nvars[p]);
		}
		nrng[0][nvars.length-1] = cplex.addEq(nrst, 1.0);
		//System.out.println(nrng[0][nvars.length-1]);
	}	
	
	public void compVal(IloMPModeler cplex, IloNumVar[][] nvar, IloRange[][] nrng,
						Distribution selec, ArrayList<ArrayList<Distribution>> mdist, 
						double val[], int state) throws Exception {
		ArrayList<Distribution> ndist = new ArrayList<Distribution>();

		IloNumVar[] x = cplex.numVarArray(1, 0.0, 1.0);
		IloNumExpr[] nsum = new IloNumExpr[1];
		nvar[0] = x;
		cplex.addMinimize(x[0]);
		nrng[0] = new IloRange[mdist.get(0).size()];
		double sum; 
		
		//System.out.println("- MDP");
		for(int j = 0; j < mdist.get(0).size(); j++) {
			ndist.add(j, new Distribution());
			for(int i = 0; i < mdist.size(); i++) {
				for(int t : mdist.get(i).get(j).getSupport()) {
					ndist.get(j).add(t, mdist.get(i).get(j).get(t)*selec.get(i));
				}
			}
		}
		
		//System.out.println("- ndist");
		//System.out.println(ndist);

		//System.out.println("-- solving");
		for(int j = 0; j < mdist.get(0).size(); j++) {
			nsum[0] = cplex.sum(x[0], 0.0);
			sum = 0.0;
			for(int t : ndist.get(j).getSupport()) {
				//if(!nlabels.get("s"+state).get(t)) { //here's the problem
				sum += ndist.get(j).get(t)*val[t];
				//}
				//else {
					//nsum[0] = cplex.sum(nsum[0], cplex.prod(-ndist.get(j).get(t), x[0]));
				//}
				//System.out.println("prob " + ndist.get(j).get(t) + " state " + t);
			}
			//System.out.println("sum " + sum);
			nrng[0][j] = cplex.addGe(nsum[0], sum);
			//System.out.println(nrng[0][j]);
		}
	}
	
	public void stratImprovement(CSG csg, CSGRewards csgRewards, BitSet target) throws Exception {

		System.out.println("-- strategy improvement");

		double[] pval = new double[csg.getNumStates()];
		double[] val = new double[csg.getNumStates()]; 
		double[] values;
		double uprob;

		IloCplex cplex = new IloCplex();
		cplex.setOut(null);
		IloRange[][] nrng = new IloRange[1][];
		IloNumVar[][] nvar = new IloNumVar[1][];     

		BitSet iset = new BitSet(csg.getNumStates());
		ArrayList<Distribution> tmp = new ArrayList<Distribution>();
		Distribution selector = new Distribution();

		for(int i = 0; i < target.length(); i++) {
			pval[target.nextSetBit(i)] = 1.0;
			val[target.nextSetBit(i)] = 1.0;
		}
		
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		
		/*** 1 ***/
		System.out.println("-- initial");
		for(int s = 0; s < csg.getNumStates(); s++) {
			System.out.println("-- s " + s);
			mdist = buildProbMatrixGame(csg, null, s);
			tmp.add(s, new Distribution());
			uprob = 1.0/((double) mdist.size());
			for(int a = 0; a < mdist.size(); a++) {
				tmp.get(s).set(a, uprob);
			}
			System.out.println(uprob);
			/*** 2 ***/
			compVal(cplex, nvar, nrng, tmp.get(s), mdist, pval, s);
			if(cplex.solve()) {
				val[s] = cplex.getObjValue();
				cplex.clearModel();
				System.out.println(val[s]);
			}
			else {
				Exception e =  new Exception("Error in valuation.");
				throw e;
			}		
		}

		boolean empty = false;
		int it = 0;

		/*** 3 ***/
		while(!empty){
			/*** 3.1 ***/
			//System.out.println("--- " + it);
			empty = true;
			for(int s = 0; s < csg.getNumStates(); s++) {
				mdist = buildProbMatrixGame(csg, null, s);
				System.out.println("-- s " + s);
				System.out.println(val[s]);
				System.out.println("- selector ");
				System.out.println(tmp.get(s));
				//optimal selector
				compSelector(cplex, nvar, nrng, mdist, null, val, 0);
				if(cplex.solve()) {
					pval[s] = cplex.getObjValue();
					values = cplex.getValues(nvar[0]);
					for (int j = 0; j < mdist.size(); j++) {
						selector.add(j, values[j+1]);
					}	  
					cplex.clearModel();
					System.out.println("pre1 " + pval[s]);
					System.out.println("v " + val[s]);
					/*** 3.2 ***/
					if(pval[s] > val[s]) {// not sure about this
						iset.set(s);
					}
					else {
						iset.clear(s);
					}
					empty = empty && !iset.get(s);
				}
				else {
					Exception e =  new Exception("Error in optimal selector.");
					throw e;
				}
				/*** 3.3 ***/
				if(iset.get(s)) {
					tmp.get(s).clear();
					for(int t : selector.getSupport()) {
						tmp.get(s).add(t, selector.get(t)); 
					} 
				}
				selector.clear();
			}
			/*** 3.4 ***/
			//System.out.println("--- update ");
			for(int s = 0; s < csg.getNumStates(); s++) {
				mdist = buildProbMatrixGame(csg, null, s);
				//System.out.println("-- s " + s);
				compVal(cplex, nvar, nrng, tmp.get(s), mdist, val, s);
				if(cplex.solve()) {
					val[s] = cplex.getObjValue();
					cplex.clearModel();
				}
				else {
					Exception e =  new Exception("Error in valuation.");
					throw e;
				}
				System.out.println(val[s]);
			}
			it++;
		}
	}
				
	public void stratProduct(CSG csg, HashMap<Integer,Distribution> adv1, HashMap<Integer,Distribution> adv2,
								ArrayList<Strategy> mdpStrat, BitSet target1, BitSet target2, boolean min1, boolean min2) throws PrismException {
		setCoalition(csg, null, current_coalition, min1, min2);
		probMatrices.clear();
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		Distribution d;
		/*
		System.out.println("## adv 1 info " + adv1.keySet().size());
		System.out.println(adv1);
		System.out.println("## adv 2 info " + adv2.keySet().size());
		System.out.println(adv2);
		*/
		for(int s = 0; s < csg.getNumStates(); s++) {
			//System.out.println("-- state " + s + " " + csg.getStatesList().get(s));
			msynch.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			String a;
			double prob;
			for (int row = 0; row < mdist.size(); row++) { // goes through each row
				a = "";
				d = new Distribution();
				for(int col = 0; col < mdist.get(row).size(); col++) { // goes through each column
					for(int t : mdist.get(row).get(col).getSupport()) { // for all states in that strategy
						if(adv1.containsKey(s) && adv2.containsKey(s)) { 
							if(adv1.get(s).keySet().contains(row) && adv2.get(s).keySet().contains(col)) {
								d.add(t, mdist.get(row).get(col).get(t)*adv1.get(s).get(row)*adv2.get(s).get(col)); 
							}
						}
					}
					if(adv1.containsKey(s) && adv2.containsKey(s)) {
						if(adv1.get(s).keySet().contains(row) && adv1.get(s).get(row) > 0 && 
								adv2.get(s).keySet().contains(col) && adv2.get(s).get(col) > 0) {
							//System.out.println("1: " + adv1.get(s).get(row));
							//System.out.println("2: " + adv2.get(s).get(col));
							//System.out.println(msynch.get(row).get(col));
							a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
						}
					}
				}
				if(!d.isEmpty()) {
					mdp.addActionLabelledChoice(s, d, a);
					//System.out.println(d);
				}
			}
		}
		
		//System.out.println(mdpStrat.get(0));
		//System.out.println(mdpStrat.get(1));
		
		String action;	
		
		if(mdpStrat !=null) {
			//System.out.println("-- from only 2");
				
			for(int s = target2.nextSetBit(0); s < target2.size() && s > 0; s = target2.nextSetBit(s+1)) {
				//System.out.println("-- state " + s);
				try {
					action = (String) ((MDStrategyArray) mdpStrat.get(0)).getChoiceAction(s);
					d = ((MDStrategyArray) mdpStrat.get(0)).getNextMove(s);
					//System.out.println(action);
					//System.out.println(d);
				
					//System.out.println("- csg check");
					if(d != null) {
						for(int t : d.getSupport()) {
							mdp.addActionLabelledChoice(s, csg.getChoice(s, t), csg.getAction(s, t));
							//System.out.println(csg.getAction(s, t));
						}	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		
			//System.out.println("-- from only 1");
		
			for(int s = target1.nextSetBit(0); s < target1.size() && s > 0; s = target1.nextSetBit(s+1)) {
				//System.out.println("-- state " + s);
				try {
					action = (String) ((MDStrategyArray) mdpStrat.get(1)).getChoiceAction(s);
					d = ((MDStrategyArray) mdpStrat.get(1)).getNextMove(s);
					//System.out.println(action);
					//System.out.println(d);
				
					//System.out.println("- csg check");
					if(d != null) {
						for(int t : d.getSupport()) {
							mdp.addActionLabelledChoice(s, csg.getChoice(s, t), csg.getAction(s, t));
							//System.out.println(csg.getAction(s, t));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		mdp.findDeadlocks(true);
		MDPSimple mdp_rm = new MDPSimple();
		map_state.put(mdp.getFirstInitialState(), mdp_rm.addState());
		mdp_rm.addInitialState(map_state.get(mdp.getFirstInitialState()));
		filterStates(mdp, mdp_rm, mdp.getFirstInitialState());
		mdp_rm.setVarList(csg.getVarList());
		mdp_rm.setStatesList(list_state);
		exportStrategy(mdp_rm,"adv_nash");
		
		//System.out.println("Exported to adv_nash.");
		mainLog.println("-- Exported to adv_nash.");
		
		/*
		DTMCSimple dtmc = new DTMCSimple();
		dtmc.addStates(mdp_rm.getNumStates());
		dtmc.addInitialState(mdp_rm.getFirstInitialState());
		for(int s = 0; s < mdp_rm.getNumStates(); s++) {
			d = new Distribution();
			for(int c = 0; c < mdp_rm.getNumChoices(s); c++) {
				for(int t : mdp_rm.getChoice(s,c).getSupport()) {
					dtmc.addToProbability(s, t, mdp_rm.getChoice(s,c).get(t));
				}
			}
		}
		dtmc.setVarList(mdp_rm.getVarList());
		dtmc.setStatesList(mdp_rm.getStatesList());
		dtmc.exportToDotFile("/Users/Gabriel/Desktop/DPhil/prism-games-csg/prism-games-csg/prism/realdtmc.dot");
		dtmc_mc.setPrecomp(false);
		BitSet newTarget = new BitSet();
		System.out.println(map_state);
		System.out.println(target);
		for(int i = target.nextSetBit(0); i < target.length() && i > 0; i = target.nextSetBit(i+1)) {
			if(map_state.keySet().contains(i))
				newTarget.set(map_state.get(i));
		}
		
		ModelCheckerResult res = dtmc_mc.computeReachProbs(dtmc, newTarget);
		System.out.println(res.soln[dtmc.getFirstInitialState()]);
		*/
	}
	
	public void epsilonNE(CSG csg, CSGRewards csgRewards1, CSGRewards csgRewards2,
							HashMap<Integer,Distribution> adv1, HashMap<Integer,Distribution> adv2,
							BitSet target1, BitSet target2, double rp1, double rp2,
							boolean min1, boolean min2) throws PrismException {
		System.out.println("\n## Epsilon NE");
		ModelCheckerResult res1, res2;

		BitSet target = (BitSet) target1.clone();
		target.or(target2);
		
		MDPSimple mdp;
		MDPRewards mdpRewards;
		boolean prob = (csgRewards1 ==null) && (csgRewards2 == null);
		
		//System.out.println("-- adv 1");
		//System.out.println(adv1);
		
		//System.out.println("-- adv 2");
		//System.out.println(adv2);
		
		//System.out.println("-- target1");
		//System.out.println(target1);
		
		//System.out.println("-- target2");
		//System.out.println(target2);		
		
		System.out.println("-- Running for player 1, fixing s2: ");
		mdp = stratProduct(csg, adv2, target2, !min1, !min2);
		//res1 = mdp_mc.computeBoundedReachProbs(mdp, target1, k, false);
		if(prob) {
 			res1 = mdp_mc.computeReachProbs(mdp, target1, false);
			System.out.println("-- Result from initial state(1): " + res1.soln[mdp.getFirstInitialState()]);
			System.out.println("-- Difference: " + (rp1 - res1.soln[mdp.getFirstInitialState()]));
		}
		else {
			mdpRewards = stratRewards(csg, csgRewards1, adv2, min1, min2);		
			res1 = mdp_mc.computeReachRewards(mdp, mdpRewards, target1, false);
			System.out.println("-- Result from initial state(1): " + res1.soln[mdp.getFirstInitialState()]);
			System.out.println("-- Difference: " + (rp1 - res1.soln[mdp.getFirstInitialState()]));
		}
		
		System.out.println("-- Running for player 2, fixing s1:");
		mdp = stratProduct(csg, adv1, target1, min1, min2);
		//res2 = mdp_mc.computeBoundedReachProbs(mdp, target2, k, false);
		if(prob) {
			res2 = mdp_mc.computeReachProbs(mdp, target2, false);
			System.out.println("-- Result from initial state(2): " + res2.soln[mdp.getFirstInitialState()]);
			System.out.println("-- Difference: " + (rp2 - res2.soln[mdp.getFirstInitialState()]));
		}
		else {
			mdpRewards = stratRewards(csg, csgRewards2, adv1, !min1, !min2);		
			res2 = mdp_mc.computeReachRewards(mdp, mdpRewards, target2, false);
			System.out.println("-- Result from initial state(2): " + res2.soln[mdp.getFirstInitialState()]);
			System.out.println("-- Difference: " + (rp2 - res2.soln[mdp.getFirstInitialState()]));
		}
	}
	
	Set<Integer> exp = new HashSet<Integer>();
	
	public void recProd(CSG csg, MDPSimple mdp, HashMap<Integer,Distribution> adv, int s, int k) throws PrismException {
		if(k > 0) {
			System.out.println(s);
			exp.add(s);
			Distribution d, n;
			ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
			ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
			if(adv.keySet().contains(s)) {
			mdist = buildProbMatrixGame(csg, msynch, s);
			for(int col = 0; col < mdist.get(0).size(); col++) { 
				n = new Distribution();
				for (int row = 0; row < mdist.size(); row++) { // goes through each row
					if(adv.get(s).keySet().contains(row)) {
						d = mdist.get(row).get(col);
						for(int t : d.getSupport()) {
							n.add(t, d.get(t)*adv.get(s).get(row));
							if(!exp.contains(t))
								recProd(csg, mdp, adv, t, k-1);
						}
					}
				}
				if(!n.isEmpty())
					mdp.addActionLabelledChoice(s, n, "");
			}
			}
		}
	}
	
	public MDPSimple stratProductBound(CSG csg, HashMap<Integer,Distribution> adv, boolean min1, boolean min2, int k) throws PrismException {
		setCoalition(csg, null, current_coalition, min1, min2);
		probMatrices.clear();
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
		
		recProd(csg, mdp, adv, csg.getFirstInitialState(), k);
		
		mdp.findDeadlocks(true);
				
		exportStrategy(mdp, "adv_bound");
		
		return mdp;
	}
	
	public void stratProduct(CSG csg, CSGRewards csgRewards, MDPSimple mdp, MDPRewardsSimple mdpRewards, 
								HashMap<Integer,Distribution> adv, BitSet terminal, boolean min1, boolean min2) throws PrismException {
		setCoalition(csg, null, current_coalition, min1, min2);
		probMatrices.clear();
		if(rewMatrices !=null)
			rewMatrices.clear();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		Distribution d;
				
		//exportStrategy(csg, "csg");
		double r;
		for(int s = 0; s < csg.getNumStates(); s++) {
			//System.out.println("-- state s " + s);
			mdpRewards.addToStateReward(s, csgRewards.getStateReward(s));
			msynch.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			mrew = buildRewMatrixGame(csg, csgRewards, s);
			String a;
			if(terminal.get(s)) {
				for(int c = 0; c < csg.getNumChoices(s); c++) {
					mdp.addActionLabelledChoice(s, csg.getChoice(s, c), csg.getAction(s, c));
				}
			}
			else {
				for(int col = 0; col < mdist.get(0).size(); col++) { // goes through each column
					a = "";
					d = new Distribution();
					r = 0.0;
					for (int row = 0; row < mdist.size(); row++) { // goes through each row
						for(int t : mdist.get(row).get(col).getSupport()) { // for all states in that strategy
							if(adv.containsKey(s)) { 
								if(adv.get(s).keySet().contains(row)) {
									d.add(t, mdist.get(row).get(col).get(t)*adv.get(s).get(row)); 
								}
							}
						}
						if(adv.containsKey(s)) {
							if(adv.get(s).keySet().contains(row) && adv.get(s).get(row) > 0) {
								a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
								r += mrew.get(row).get(col)*adv.get(s).get(row); //not too sure about this
							}
						}
					}
					if(!d.isEmpty()) {
						mdp.addActionLabelledChoice(s, d, a);
						mdpRewards.addToTransitionReward(s, mdp.getNumChoices(s)-1, r);
					}
				}
			}
		}
		
		mdp.findDeadlocks(true);
		
		MDPSimple mdp_rm = new MDPSimple();
		MDPRewardsSimple mdp_rew_rm = new MDPRewardsSimple(mdp.getNumStates());		
		
		map_state.put(mdp.getFirstInitialState(), mdp_rm.addState());
		mdp_rm.addInitialState(map_state.get(mdp.getFirstInitialState()));
		filterStates(mdp, mdp_rm, mdp.getFirstInitialState());
		//filterStates(mdp, mdp_rm, mdpRewards, mdp_rew_rm, mdp.getFirstInitialState());
		mdp_rm.setVarList(mdp.getVarList());
		mdp_rm.setStatesList(list_state);

		System.out.println("## mdpRewards");
		System.out.println(mdpRewards);

		for(int s = 0; s < mdp.getNumStates(); s++) {
			//for(int c = 0; c < mdp_rm.getNumChoices(s); c++) {
				
			//}
			if(map_state.keySet().contains(s))
				mdp_rew_rm.addToStateReward(map_state.get(s), mdpRewards.getStateReward(s));
		}
		
		System.out.println("## mdp_rew_rm");
		System.out.println(mdp_rew_rm);
		
		ModelCheckerResult res2 = mdp_mc.computeCumulativeRewards(mdp_rm, mdp_rew_rm, 4, false);
		System.out.println("-- Result from initial state(max): " + res2.soln[mdp_rm.getFirstInitialState()]);

		res2 = mdp_mc.computeCumulativeRewards(mdp_rm, mdp_rew_rm, 4, true);
		System.out.println("-- Result from initial state(min): " + res2.soln[mdp_rm.getFirstInitialState()]);
	}	
	
	public MDPSimple stratProduct(CSG csg, HashMap<Integer,Distribution> adv, BitSet terminal, boolean min1, boolean min2) throws PrismException {
		// 	NEED TO REMEMBER TO PROPAGATE THE COALITION IN ORDER TO BUILD THE MATRIX GAMES CORRECTLY. 
		setCoalition(csg, null, current_coalition, min1, min2);
		probMatrices.clear();
		if(rewMatrices !=null)
			rewMatrices.clear();
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		Distribution d;
		
		/*
		System.out.println("\n## adv info ##");
		System.out.println(adv.keySet().size());
		System.out.println(adv);
		*/
				
		for(int s = 0; s < csg.getNumStates(); s++) {
			//System.out.println("-- state s " + s);
			msynch.clear();
			//mdist.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			//mdist = buildProbMatrixGame(csg, null, s);
			String a;
			if(terminal.get(s)) {
				for(int c = 0; c < csg.getNumChoices(s); c++) {
					mdp.addActionLabelledChoice(s, csg.getChoice(s, c), csg.getAction(s, c));
				}
			}
			else {
				for(int col = 0; col < mdist.get(0).size(); col++) { // goes through each column
					a = "";
					d = new Distribution();
					for (int row = 0; row < mdist.size(); row++) { // goes through each row
						for(int t : mdist.get(row).get(col).getSupport()) { // for all states in that strategy
							if(adv.containsKey(s)) { 
								if(adv.get(s).keySet().contains(row)) {
									d.add(t, mdist.get(row).get(col).get(t)*adv.get(s).get(row)); 
								}
							}
						}
						if(adv.containsKey(s)) {
							if(adv.get(s).keySet().contains(row) && adv.get(s).get(row) > 0) {
							a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
							}
						}
					}
					if(!d.isEmpty()) {
						//mdp.addActionLabelledChoice(s, d, "");
						mdp.addActionLabelledChoice(s, d, a);
					}
				}
			}
		}
		
		mdp.findDeadlocks(true);
				
		/*
		MDPSimple mdp_rm = new MDPSimple();
		map_state.put(mdp.getFirstInitialState(), mdp_rm.addState());
		mdp_rm.addInitialState(map_state.get(mdp.getFirstInitialState()));
		filterStates(mdp, mdp_rm, mdp.getFirstInitialState());
		mdp_rm.setVarList(mdp.getVarList());
		mdp_rm.setStatesList(list_state);
		*/
		
		/*
		System.out.println("-- mdp");
		for(int s = mdp_rm.getFirstInitialState(); s < mdp_rm.getNumStates(); s++) {
			System.out.println("-- state " + s);
			for(int t = 0; t < mdp_rm.getNumChoices(s); t++) {
				System.out.println(mdp_rm.getChoice(s, t));
			}
		}
		*/
				
		//System.out.println("\n-- MDP STATES " + mdp.getNumStates());
		//exportStrategy(mdp, "adv_minimax_bug");
		//System.out.println("\n-- MDP_RM STATES " + mdp_rm.getNumStates());
		//exportStrategy(mdp_rm, "adv_minimax_rm_bug");
		
		return mdp;
	}
	
	public MDPRewards stratRewards(CSG csg, CSGRewards csgRewards, HashMap<Integer,Distribution> adv, boolean min1, boolean min2) throws PrismException {
		setCoalition(csg, csgRewards, current_coalition, min1, min2);
		probMatrices.clear();
		if(rewMatrices !=null)
			rewMatrices.clear();
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		Distribution d;
		MDPRewardsSimple mdpRewards = new MDPRewardsSimple(mdp.getNumStates());		
		double r;
		for(int s = 0; s < csg.getNumStates(); s++) {
			mdpRewards.addToStateReward(s, csgRewards.getStateReward(s));
			msynch.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			mrew = buildRewMatrixGame(csg, csgRewards, s);
			String a;
			for(int col = 0; col < mdist.get(0).size(); col++) {
				a = "";
				d = new Distribution();
				r = 0.0;
				for (int row = 0; row < mdist.size(); row++) {
					for(int t : mdist.get(row).get(col).getSupport()) {					
						if(adv.containsKey(s)) {
							if(adv.get(s).keySet().contains(row)) {
								d.add(t, mdist.get(row).get(col).get(t)*adv.get(s).get(row)); 
							}
						}
					}
					if(adv.containsKey(s)) {
						if(adv.get(s).keySet().contains(row)) {
							a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
							r += mrew.get(row).get(col)*adv.get(s).get(row); //not too sure about this
						}
					}
				}
				if(!d.isEmpty()) {
					mdp.addActionLabelledChoice(s, d, a);
					//mdp.addChoice(s, d);
					mdpRewards.addToTransitionReward(s, mdp.getNumChoices(s)-1, r);
				}
			}
		}
		return mdpRewards;
	}
	
	public void exportStrategy(MDPSimple mdp, String fileName) {
		Path currentRelativePath = Paths.get("");
		String path = currentRelativePath.toAbsolutePath().toString();
		PrintStream printStream; 
		PrismLog logout;
		try {
			printStream = new PrintStream(new FileOutputStream(path + "/" + fileName + ".dot", false));
			logout = new PrismPrintStreamLog(printStream);
			mdp.exportToDotFile(logout, null, true);
			
			printStream = new PrintStream(new FileOutputStream(path + "/" + fileName + ".tra", false)); 
			logout = new PrismPrintStreamLog(printStream);
			mdp.exportToPrismExplicitTra(logout);	
			
			printStream = new PrintStream(new FileOutputStream(path + "/" + fileName + ".sta", false)); 
			logout = new PrismPrintStreamLog(printStream);
			mdp.exportStates(0, mdp.getVarList(), logout);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void minMaxCompare(CSG csg, CSGRewards csgRewards1, CSGRewards csgRewards2, BitSet target1, BitSet target2, Coalition coalition, int k, boolean rew) throws PrismException {
		System.out.println("\n### Minimax Compare ###");
		ModelCheckerResult res1;
		ModelCheckerResult res2;
		double mxmx, mxmn;
		mxmx = 0;
		mxmn = 0;
		
		boolean reach = (target1 !=null) & (target2 != null); 
		
		if(!reach) {
			target1 = new BitSet();
			target2 = new BitSet();
		}
		
		map_state.clear();
				
		// makes the product of the adversary to obtain the MDP
		// the resulting MDP should only have options for player 2
		MDPSimple mdp;
		
		/*
		computeReachProbs(CSG csg, BitSet remain, BitSet target, boolean min1, boolean min2, 
												double init[], BitSet known, int bound, Coalition coalition)
												throws PrismException { 
		*/
		
		/*
		computeBoundedReachProbs(CSG csg, BitSet remain, BitSet target, int k, 
												boolean min1, boolean min2, double init[],
												double results[], Coalition coalition) throws PrismException {
		*/
		
		//double[] init = new double[csg.getNumStates()];
		
		BitSet all = new BitSet();
		all.set(0, csg.getNumStates());
		
		mdp = new MDPSimple();
		
		if(!rew) {
			System.out.println("### Prob ###");
			res1 = new ModelCheckerResult();
			if(reach) {
				if(k==0) {
					//System.out.println("-- F");
					res1 = computeReachProbs(csg, all, target1, false, true, null, new BitSet(), maxIters, coalition, true); 
					mdp = stratProduct(csg, adv, target1, false, true);
				}
				else {
					//System.out.println("-- F<=k");
					res1 = computeBoundedReachProbs(csg, all, target1, k, false, true, null, null, coalition, true);
					mdp = stratProduct(csg, adv, new BitSet(), false, true);
				}
			}
				
			System.out.println("\nResult for player minmax 1:");
			//System.out.println(Arrays.toString(res1.soln));
			mxmx = mxmn = res1.soln[csg.getFirstInitialState()];
			
			System.out.println("-- Result from initial state: " + mxmx);
									
			System.out.println("\nCSG States " + csg.getNumStates());
			System.out.println("\nMDP States " + mdp.getNumStates());
						
			if(reach) {
				if(k==0) {
					res2 = mdp_mc.computeReachProbs(mdp, target2, false);
					//System.out.println(Arrays.toString(res2.soln));
					mxmx += res2.soln[mdp.getFirstInitialState()];
					System.out.println("-- Result from initial state(max): " + res2.soln[mdp.getFirstInitialState()]);
				
					res2 = mdp_mc.computeReachProbs(mdp, target2, true);
					//System.out.println(Arrays.toString(res2.soln));
					mxmn += res2.soln[mdp.getFirstInitialState()];
					System.out.println("-- Result from initial state(min): " + res2.soln[mdp.getFirstInitialState()]);
				}
				else {
					res2 = mdp_mc.computeBoundedReachProbs(mdp, target2, k, false);
					//System.out.println(Arrays.toString(res2.soln));
					mxmx += res2.soln[mdp.getFirstInitialState()];
					System.out.println("-- Result from initial state(max): " + res2.soln[mdp.getFirstInitialState()]);
				
					res2 = mdp_mc.computeBoundedReachProbs(mdp, target2, k, true);
					//System.out.println(Arrays.toString(res2.soln));
					mxmn += res2.soln[mdp.getFirstInitialState()];
					System.out.println("-- Result from initial state(min): " + res2.soln[mdp.getFirstInitialState()]);
				}
			}
			
			System.out.println("-- Result mxmx: " + mxmx);
			System.out.println("-- Result mxmn: " + mxmn);
		}
		else {
			System.out.println("### Rewards ###");
			if(reach) {
				System.out.println("-- F");
				res1 = computeReachRewardsCumulative(csg, coalition, csgRewards1, target1, false, true, new double[0], null, true);
				mdp = stratProduct(csg, adv, target1, false, true);
			}
			else {
				System.out.println("-- F<=k");
				res1 = computeCumulativeRewards(csg, csgRewards1, coalition, k, false, true, true);
				mdp = stratProduct(csg, adv, new BitSet(), false, true);
				
				//System.out.println("\nResult for player minmax 1:");
				//System.out.println("-- Result from initial state: " + res1.soln[csg.getFirstInitialState()]);
				
				//MDPRewards mdpRew = new MDPRewardsSimple(csg.getNumStates());
				//stratProduct(csg, csgRewards2, mdp, (MDPRewardsSimple) mdpRew, adv, new BitSet(), false, true);
				
				//mdp = stratProductBound(csg, adv, false, true, k);
			}
			System.out.println("\nResult for player minmax 1:");
			//System.out.println(Arrays.toString(res1.soln));
			mxmx = mxmn = res1.soln[csg.getFirstInitialState()];
			System.out.println("-- Result from initial state: " + res1.soln[csg.getFirstInitialState()]);
			
			System.out.println("\nCSG States " + csg.getNumStates());
			System.out.println("\nMDP States " + mdp.getNumStates());
		
			MDPRewards mdpRewards = stratRewards(csg, csgRewards2, adv, false, true);		
			
			//System.out.println("-- mdpRewards");
			//System.out.println(mdpRewards);
			
			if(reach) {
				System.out.println("-- F");
				res2 = mdp_mc.computeReachRewards(mdp, mdpRewards, target2, false);
				//System.out.println(Arrays.toString(res2.soln));
				mxmx += res2.soln[mdp.getFirstInitialState()];
				System.out.println("-- Result from initial state(max): " + res2.soln[mdp.getFirstInitialState()]);

				res2 = mdp_mc.computeReachRewards(mdp, mdpRewards, target2, true);
				//System.out.println(Arrays.toString(res2.soln));
				mxmn += res2.soln[mdp.getFirstInitialState()];
				System.out.println("-- Result from initial state(min): " + res2.soln[mdp.getFirstInitialState()]);
			}
			else {
				System.out.println("-- F<=k");
				res2 = mdp_mc.computeCumulativeRewards(mdp, mdpRewards, k, false);
				//System.out.println(Arrays.toString(res2.soln));
				mxmx += res2.soln[mdp.getFirstInitialState()];
				System.out.println("-- Result from initial state(max): " + res2.soln[mdp.getFirstInitialState()]);

				res2 = mdp_mc.computeCumulativeRewards(mdp, mdpRewards, k, true);
				//System.out.println(Arrays.toString(res2.soln));
				mxmn += res2.soln[mdp.getFirstInitialState()];
				System.out.println("-- Result from initial state(min): " + res2.soln[mdp.getFirstInitialState()]);
			}
			
			System.out.println("-- Result mxmx: " + mxmx);
			System.out.println("-- Result mxmn: " + mxmn);
		}
	}
	
	public void filterStates(MDPSimple mdp, MDPSimple mdp_rm, MDPRewards rew, MDPRewards rew_rm, int s) {
		Distribution d;
		list_state.add(mdp.getStatesList().get(s));
   		for(int c = 0; c < mdp.getNumChoices(s); c++) { //gets all choices
   			d = new Distribution();
			for(int t : mdp.getChoice(s, c).getSupport()) { //gets all targets
				if(!map_state.keySet().contains(t)) { //if not yet explored
					map_state.put(t, mdp_rm.addState());
					filterStates(mdp, mdp_rm, rew, rew_rm, t);
				}
				d.add(map_state.get(t), mdp.getChoice(s, c).get(t)); //adds target to distribution
			}
			mdp_rm.addActionLabelledChoice(map_state.get(s), d, mdp.getAction(s, c));
   			((MDPRewardsSimple) rew_rm).addToTransitionReward(map_state.get(s), mdp_rm.getNumChoices(map_state.get(s))-1, rew.getTransitionReward(s, c));
		}
   		((MDPRewardsSimple) rew_rm).addToStateReward(map_state.get(s), rew.getStateReward(s));
	}
	
	//mdpRewards.addToStateReward(s, csgRewards.getStateReward(s));
	//mdpRewards.addToTransitionReward(s, mdp.getNumChoices(s)-1, r);
	
	public void filterStates(MDPSimple mdp, MDPSimple mdp_rm, int s) {
		Distribution d;
		list_state.add(mdp.getStatesList().get(s));
		//System.out.println("-- filter s" + s);
   		for(int c = 0; c < mdp.getNumChoices(s); c++) { //gets all choices
   			d = new Distribution();
			for(int t : mdp.getChoice(s, c).getSupport()) { //gets all targets
				if(!map_state.keySet().contains(t)) { //if not yet explored
					map_state.put(t, mdp_rm.addState());
					filterStates(mdp, mdp_rm, t);
				}
				d.add(map_state.get(t), mdp.getChoice(s, c).get(t)); //adds target to distribution
			}
			mdp_rm.addActionLabelledChoice(map_state.get(s), d, mdp.getAction(s, c));
		}
	}
	
	public void strategyProductProb(CSG csg, BitSet target, HashMap<Integer, Distribution> adv) throws PrismException, FileNotFoundException {
		Path currentRelativePath = Paths.get("");
		String path = currentRelativePath.toAbsolutePath().toString();
		PrintStream printStream; 
		PrismLog logout;
				
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
        		
		//System.out.println("-- initial state");
		//System.out.println(csg.getFirstInitialState());
		
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		
		Distribution d;
		BitSet sbs = new BitSet();
		BitSet term = new BitSet();
				
		/*
		System.out.println("-- csg");
		for(int s = csg.getFirstInitialState(); s < csg.getNumStates(); s++) {
			System.out.println("-- state " + s);
			for(int t = 0; t < csg.getNumChoices(s); t++) {
				System.out.println(csg.getChoice(s, t));
			}
		}
		*/

		//System.out.println(csg.getLabels());
		//System.out.println("-- multiplication");
				
		for(int s = csg.getFirstInitialState(); s < csg.getNumStates(); s++) {
			//System.out.println("-- state " + s);
			//System.out.println("-- csg state rewards " + csgRewards.getStateReward(s));
			msynch.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			
			/*
			for(int t = 0; t < csg.getNumChoices(s); t++) {
				System.out.println("-- csg transition rewards " +  csgRewards.getTransitionReward(s, t));
			}
			*/
			sbs.clear();
			sbs.set(s);
			/*
			System.out.println("--- mdist");
			System.out.println(mdist);
			System.out.println("--- mrew");
			System.out.println(mrew);
			*/
			String a;
		
			//System.out.println("--- msynch");
			//System.out.println(msynch);
						
			for(int col = 0; col < mdist.get(0).size(); col++) {
				a = "";
				d = new Distribution();
				for (int row = 0; row < mdist.size(); row++) {
					for(int t : mdist.get(row).get(col).getSupport()) {	
						if(adv.containsKey(s)) {
							if(adv.get(s).keySet().contains(row)) {
 								d.add(t, mdist.get(row).get(col).get(t)*adv.get(s).get(row)); 
							}
						}
					}
					if(adv.containsKey(s)) {
						if(adv.get(s).keySet().contains(row)) {
							a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
						}
					}
				}
				//System.out.println(d);
				if(!d.isEmpty()) {
					mdp.addActionLabelledChoice(s, d, a);
					//mdp.addChoice(s, d);
				}
			}
			if(mdp.allSuccessorsInSet(s, sbs))
				term.set(s);
		}
		
		/*
		System.out.println(term);
		
		System.out.println("-- mdp");
		for(int s = mdp.getFirstInitialState(); s < mdp.getNumStates(); s++) {
			System.out.println("-- state " + s);
			for(int t = 0; t < mdp.getNumChoices(s); t++) {
				System.out.println(mdp.getChoice(s, t));
			}
		}		
		*/		
				
		MDPSimple mdp_rm = new MDPSimple();
		
		//System.out.println(mdp.getStatesList());
		
		//mdp_rm.addStates(csg.getNumStates());
		//mdp_rm.addInitialState(csg.getFirstInitialState());
		
		map_state.put(mdp.getFirstInitialState(), mdp_rm.addState());
		filterStates(mdp, mdp_rm, mdp.getFirstInitialState());
		//mdp_rm.setStatesList(csg.getStatesList());
		mdp_rm.setVarList(csg.getVarList());

		/*
		System.out.println("-- mdp");
		System.out.println(mdp.getNumStates());
		System.out.println("-- mdp_rm");
		System.out.println(mdp_rm.getNumStates());
		System.out.println(list_state.size());
		*/
		
		mdp_rm.setStatesList(list_state);

		/*
		System.out.println("-- mdpRewards");
		for(int s = mdp.getFirstInitialState(); s < mdp.getNumStates(); s++) {
			System.out.println("-- state " + s);
			System.out.println("-- state rewards " +  mdpRewards.getStateReward(s));
			for(int t = 0; t < mdp.getNumChoices(s); t++) {
				System.out.println("--- transition rewards " +  mdpRewards.getTransitionReward(s, t));
			}
		}	
		*/
		
		printStream = new PrintStream(new FileOutputStream(path + "/adv.dot", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportToDotFile(logout, null, true);
		
		printStream = new PrintStream(new FileOutputStream(path + "/adv.tra", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportToPrismExplicitTra(logout);	
		
		printStream = new PrintStream(new FileOutputStream(path + "/examples-distr/games/csg/adv.sta", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportStates(0, mdp.getVarList(), logout);
			
		BitSet newTarget = new BitSet();
		
		for(int s = 0; s < csg.getNumStates(); s++) {
			if(target.get(s)) {
				newTarget.set(map_state.get(s));
			}
		}
				
		/*
		System.out.println("-- checking strategy ");
		ModelCheckerResult res = mdp_mc.computeReachProbs((MDP) mdp, newTarget, true);
		*/
		
		/*
		ExpressionVar vs = new ExpressionVar("r", parser.type.TypeInt.getInstance());
		ExpressionLiteral ns = new ExpressionLiteral(parser.type.TypeInt.getInstance(), null);
		vs.setIndex(modulesFile.getVarIndex("r"));
		ExpressionBinaryOp eq = new ExpressionBinaryOp(5, null, null);
		eq.setOperand1(vs);	
				
		//System.out.println("\n" + Arrays.toString(res.soln));
		System.out.println("\n-- result should be: " + res.soln[mdp.getFirstInitialState()]);
		
		Result states = new Result();
		
		for(int i = 1; i <= 9; i++) {
			ns.setValue(i);
			eq.setOperand2(ns);
			states = mdp_mc.check(mdp, vs);
			System.out.println(states);
		}
		*/

		System.out.println("-- adversary exported to " + path + "/adv.dot");
		System.out.println("-- adversary exported to " + path + "/adv.tra");
		
		System.out.println("-- map_states");
		System.out.println(map_state);
	}
	
	public void strategyProductRew(CSG csg, CSGRewards csgRewards, HashMap<Integer, Distribution> adv) throws PrismException, FileNotFoundException {	
		Path currentRelativePath = Paths.get("");
		String path = currentRelativePath.toAbsolutePath().toString();
		PrintStream printStream; 
		PrismLog logout;
				
		MDPSimple mdp = new MDPSimple();
		mdp.addStates(csg.getNumStates());
		mdp.setStatesList(csg.getStatesList());
		mdp.setVarList(csg.getVarList());
		mdp.addInitialState(csg.getFirstInitialState());
        		
		//System.out.println("-- initial state");
		//System.out.println(csg.getFirstInitialState());
		
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> mrew = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<String>> msynch = new ArrayList<ArrayList<String>>();
		
		Distribution d;
		BitSet sbs = new BitSet();
		BitSet term = new BitSet();
		
		MDPRewardsSimple mdpRewards = new MDPRewardsSimple(mdp.getNumStates());		
		double r;
		
		/*
		System.out.println("-- csg");
		for(int s = csg.getFirstInitialState(); s < csg.getNumStates(); s++) {
			System.out.println("-- state " + s);
			for(int t = 0; t < csg.getNumChoices(s); t++) {
				System.out.println(csg.getChoice(s, t));
			}
		}
		*/

		//System.out.println(csg.getLabels());
		//System.out.println("-- multiplication");
		
		for(int s = csg.getFirstInitialState(); s < csg.getNumStates(); s++) {
			//System.out.println("-- state " + s);
			//System.out.println("-- csg state rewards " + csgRewards.getStateReward(s));
			mdpRewards.addToStateReward(s, csgRewards.getStateReward(s));
			msynch.clear();
			mdist = buildProbMatrixGame(csg, msynch, s);
			mrew = buildRewMatrixGame(csg, csgRewards, s);
			
			/*
			for(int t = 0; t < csg.getNumChoices(s); t++) {
				System.out.println("-- csg transition rewards " +  csgRewards.getTransitionReward(s, t));
			}
			*/
			sbs.clear();
			sbs.set(s);
			/*
			System.out.println("--- mdist");
			System.out.println(mdist);
			System.out.println("--- mrew");
			System.out.println(mrew);
			*/
			String a;
		
			//System.out.println("--- msynch");
			//System.out.println(msynch);
			
			for(int col = 0; col < mdist.get(0).size(); col++) {
				a = "";
				d = new Distribution();
				r = 0.0;
				for (int row = 0; row < mdist.size(); row++) {
					for(int t : mdist.get(row).get(col).getSupport()) {					
						if(adv.containsKey(s)) {
							if(adv.get(s).keySet().contains(row)) {
								d.add(t, mdist.get(row).get(col).get(t)*adv.get(s).get(row)); 
							}
						}
					}
					if(adv.containsKey(s)) {
						if(adv.get(s).keySet().contains(row)) {
							a = a.concat("-").concat(msynch.get(row).get(col)); //not too sure about this
							r += mrew.get(row).get(col)*adv.get(s).get(row); //not too sure about this
						}
					}
				}
				//System.out.println(d);
				if(!d.isEmpty()) {
					mdp.addActionLabelledChoice(s, d, a);
					//mdp.addChoice(s, d);
					mdpRewards.addToTransitionReward(s, mdp.getNumChoices(s)-1, r);
				}
			}
			if(mdp.allSuccessorsInSet(s, sbs))
				term.set(s);
		}
		
		/*
		System.out.println(term);
		
		System.out.println("-- mdp");
		for(int s = mdp.getFirstInitialState(); s < mdp.getNumStates(); s++) {
			System.out.println("-- state " + s);
			for(int t = 0; t < mdp.getNumChoices(s); t++) {
				System.out.println(mdp.getChoice(s, t));
			}
		}		
		*/		
				
		MDPSimple mdp_rm = new MDPSimple();
		
		//System.out.println(mdp.getStatesList());
		
		//mdp_rm.addStates(csg.getNumStates());
		//mdp_rm.addInitialState(csg.getFirstInitialState());
		
		map_state.put(mdp.getFirstInitialState(), mdp_rm.addState());
		filterStates(mdp, mdp_rm, mdp.getFirstInitialState());
		//mdp_rm.setStatesList(csg.getStatesList());
		mdp_rm.setVarList(csg.getVarList());

		/*
		System.out.println("-- mdp");
		System.out.println(mdp.getNumStates());
		System.out.println("-- mdp_rm");
		System.out.println(mdp_rm.getNumStates());
		System.out.println(list_state.size());
		*/
		
		mdp_rm.setStatesList(list_state);

		/*
		System.out.println("-- mdpRewards");
		for(int s = mdp.getFirstInitialState(); s < mdp.getNumStates(); s++) {
			System.out.println("-- state " + s);
			System.out.println("-- state rewards " +  mdpRewards.getStateReward(s));
			for(int t = 0; t < mdp.getNumChoices(s); t++) {
				System.out.println("--- transition rewards " +  mdpRewards.getTransitionReward(s, t));
			}
		}	
		*/
		
		printStream = new PrintStream(new FileOutputStream(path + "/adv.dot", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportToDotFile(logout, null, true);
		
		printStream = new PrintStream(new FileOutputStream(path + "/adv.tra", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportToPrismExplicitTra(logout);	
		
		printStream = new PrintStream(new FileOutputStream(path + "/examples-distr/games/csg/adv.sta", false)); 
		logout = new PrismPrintStreamLog(printStream);
		
		mdp_rm.exportStates(0, mdp.getVarList(), logout);
		
		ExpressionTemporal expt = new ExpressionTemporal();
		expt.setOperator(ExpressionTemporal.R_C);
						
		/*
		System.out.println("-- checking strategy ");
		ModelCheckerResult res = mdp_mc.computeReachRewards((MDP) mdp, mdpRewards, term, true);
		*/
		
		/*
		ExpressionVar vs = new ExpressionVar("r", parser.type.TypeInt.getInstance());
		ExpressionLiteral ns = new ExpressionLiteral(parser.type.TypeInt.getInstance(), null);
		vs.setIndex(modulesFile.getVarIndex("r"));
		ExpressionBinaryOp eq = new ExpressionBinaryOp(5, null, null);
		eq.setOperand1(vs);	
				
		//System.out.println("\n" + Arrays.toString(res.soln));
		System.out.println("\n-- result should be: " + res.soln[mdp.getFirstInitialState()]);
		
		Result states = new Result();
		
		for(int i = 1; i <= 9; i++) {
			ns.setValue(i);
			eq.setOperand2(ns);
			states = mdp_mc.check(mdp, vs);
			System.out.println(states);
		}
		*/

		//System.out.println("-- adversary exported to " + path + "/adv.dot");
		//System.out.println("-- adversary exported to " + path + "/adv.tra");
		
		System.out.println("-- map_states");
		System.out.println(map_state);
	}
	
}
