package explicit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import csg.NashSMT;
import explicit.rewards.CSGRewards;
import explicit.rewards.MDPRewards;
import parser.ast.Coalition;
import prism.PrismException;
import prism.PrismUtils;
import strat.Strategy;

public class CSGModelCheckerAux {

	private CSGModelChecker csg_mc;
	private HashMap<Integer, ArrayList<Distribution>> adv;
	private HashMap<Integer, Distribution> adv1;
	private HashMap<Integer, Distribution> adv2;

	public CSGModelCheckerAux(CSGModelChecker mc) {
		csg_mc = mc;
	}
	
	public static double[] socialWelfareEq(double[][]eqs, ArrayList<ArrayList<Distribution>> lstrat, boolean p2) {
		/*
		System.out.println("-- equilibria");
		for(int e = 0; e < eqs.length; e++) {
			System.out.println("- equilibrium " + e);
			System.out.println("- p1 " + eqs[e][0]);
			System.out.println("- p2 " + eqs[e][1]);
		}
		*/
		ArrayList<Distribution> d = null;
		double r[] = new double[3];
		r[0] = eqs[0][0] + eqs[0][1];
		r[1] = eqs[0][0];
		r[2] = eqs[0][1];
		if(lstrat != null)
			d = lstrat.get(0);
		for(int i = 1; i < eqs.length; i++) {
			if(eqs[i][0] + eqs[i][1] > r[0]) {
				r[0] = eqs[i][0] + eqs[i][1];
				r[1] = eqs[i][0];
				r[2] = eqs[i][1];
				if(lstrat != null)
					d = lstrat.get(i);
			}
			else if(eqs[i][0] + eqs[i][1] == r[0]) {
				// should there be other criteria?
				if(eqs[i][0] == eqs[i][1]) {
					r[1] = eqs[i][0];
					r[2] = eqs[i][1];
					if(lstrat != null)
						d = lstrat.get(i);
				} else if(!p2) {
					if(eqs[i][0] > r[1]) {
						r[1] = eqs[i][0];
						r[2] = eqs[i][1];
						if(lstrat != null)
							d = lstrat.get(i);
					}
				} else {
					if(eqs[i][1] > r[2]) {
						r[1] = eqs[i][0];
						r[2] = eqs[i][1];
						if(lstrat != null)
							d = lstrat.get(i);
					}
				}
			}
		}
		final ArrayList<Distribution> f = d;
		//System.out.println(f);
		if(lstrat != null)
			lstrat.removeIf((ArrayList<Distribution> e) -> !e.equals(f));
		//System.out.println("-- social welfare");
		//System.out.println(Arrays.toString(r));
		return r;
	}
	
	public static double[] maxEq(double[][] eqs, ArrayList<ArrayList<Distribution>> lstrat, boolean p2) {
		ArrayList<Distribution> d = null;
		double r[] = new double[2];
		r[0] = eqs[0][0];
		r[1] = eqs[0][1];
		if(lstrat != null)
			d = lstrat.get(0);
		for(int i = 1; i < eqs.length; i++) {
			if(!p2) {
				if(eqs[i][0] > r[0]) {
					r[0] = eqs[i][0];
					r[1] = eqs[i][1];
					if(lstrat != null)
						d = lstrat.get(i);
				}
			}
			else {
				if(eqs[i][1] > r[1]) {
					r[0] = eqs[i][0];
					r[1] = eqs[i][1];
					if(lstrat != null)
						d = lstrat.get(i);
				}
			}
		}
		final ArrayList<Distribution> f = d;
		if(lstrat != null)
			lstrat.removeIf((ArrayList<Distribution> e) -> !e.equals(f));
		return r;
	}
	
	public static double[] minEq(double[][] eqs, boolean p2) {
		double r[] = new double[2];
		r[0] = eqs[0][0];
		r[1] = eqs[0][1];
		for(int i = 1; i < eqs.length; i++) {
			if(!p2) {
				if(eqs[i][0] < r[0]) {
					r[0] = eqs[i][0];
					r[1] = eqs[i][1];
				}
			}
			else {
				if(eqs[i][1] < r[1]) {
					r[0] = eqs[i][0];
					r[1] = eqs[i][1];
				}
			}
		}
		return r;
	}
	
	public double[] computeProbBoundedEquilibria(CSG csg, BitSet target1, BitSet target2, BitSet known, 
												double[][] init, int k1, int k2, boolean genAdv) throws PrismException {
		int ni = 0;
		boolean done = true;
		double[] r = new double[csg.getNumStates()];
		adv = new HashMap<Integer, ArrayList<Distribution>>();
		adv1 = new HashMap<Integer, Distribution>();
		adv2 = new HashMap<Integer, Distribution>();

		//System.out.println("-- New Bounded Nash");
		
		double[][] val = new double[2][csg.getNumStates()];
		double[][] sol = new double[2][csg.getNumStates()];
		double[][] tmp = new double[2][csg.getNumStates()];
		Arrays.fill(val[0], 0);
		Arrays.fill(sol[0], 0);
		Arrays.fill(tmp[0], 0);
		Arrays.fill(val[1], 0);
		Arrays.fill(sol[1], 0);
		Arrays.fill(tmp[1], 0);
		double[][] eq;
		double[] meq;
		
		NashSMT nash = new NashSMT();
		
		if(known == null) {
			known = new BitSet();
		}
		
		BitSet all = new BitSet();
		all.set(0, csg.getNumStates());
		BitSet only1 = (BitSet) target1.clone();
		BitSet only2 = (BitSet) target2.clone();
		BitSet both = (BitSet) target1.clone();
		
		only1.andNot(target2);
		only2.andNot(target1);
		both.and(target2);
		
		known.or(only1);
		known.or(only2);
		known.or(both);
		
		ArrayList<ArrayList<String>> mgame;
		ArrayList<ArrayList<Distribution>> lstrat;
		
		for(int s = 0; s < csg.getNumStates(); s++) {
			sol[0][s] = val[0][s] = target1.get(s)? 1.0 : 0.0;
			sol[1][s] = val[1][s] = target2.get(s)? 1.0 : 0.0;
		}

		mgame = new ArrayList<ArrayList<String>>();
		lstrat = new ArrayList<ArrayList<Distribution>>();

		double[] only1v = new double[csg.getNumStates()];
		double[] only2v = new double[csg.getNumStates()];
		
		long timer = 0, curr;
				
		while(true) {
			//System.out.println("\n################### i " + ni + " ###################");
			//System.out.println(Arrays.toString(sol[0]));
			//System.out.println(Arrays.toString(sol[1]));
			
			curr = System.currentTimeMillis();
			if(ni < k1) {
				only1v = csg_mc.mdp_mc.computeBoundedReachProbs(csg, target2, ni, false).soln;
			}
			if(ni < k2) {
				only2v = csg_mc.mdp_mc.computeBoundedReachProbs(csg, target1, ni, false).soln;
			}	
			curr = System.currentTimeMillis() - curr;
			timer += curr;
			
			for(int s = 0; s < csg.getNumStates(); s++) {
				if(only1.get(s)) {
					sol[0][s] = 1.0;
					sol[1][s] = only1v[s];
				}
				else if(only2.get(s)) {
					sol[0][s] = only2v[s];
					sol[1][s] = 1.0;
				}
				else if(both.get(s)) {
					sol[0][s] = 1.0;
					sol[1][s] = 1.0;
				}	
				
				if(ni >= k1) {
					sol[0][s] = target1.get(s)? 1.0 : 0.0;
				}
				if(ni >= k2) {
					sol[1][s] = target2.get(s)? 1.0 : 0.0;
				}
			}
			
			for(int s = 0; s < csg.getNumStates(); s++) {
				lstrat.clear();
				//System.out.println("val 0 " + Arrays.toString(val[0]));
				//System.out.println("val 1 " + Arrays.toString(val[1]));
				
				if(!known.get(s)) {
					//System.out.println("\n--- s " + s);
					if(genAdv) {		
						eq = stepEquilibria(csg, null, null, nash, mgame, lstrat, sol, s, genAdv);
					}
					else {
						eq = stepEquilibria(csg, null, null, nash, null, null, sol, s, genAdv);
					}
					/*
					if(genAdv) {	
						meq = maxEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
						adv.put(s, lstrat.get(0));
						adv1.put(s, lstrat.get(0).get(0));
						adv2.put(s, lstrat.get(0).get(1));
						}
					else {
						meq = maxEq(eq, null, false);		
					}
					val[0][s] = meq[0];
					val[1][s] = meq[1];
					 */
					if(genAdv) {
						meq = socialWelfareEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
						adv.put(s, lstrat.get(0));
						adv1.put(s, lstrat.get(0).get(0));
						adv2.put(s, lstrat.get(0).get(1));
					}
					else {
						meq = socialWelfareEq(eq, null, false);		
					}
					val[0][s] = meq[1];
					val[1][s] = meq[2];
				}
				r[s] = val[0][s] + val[1][s];
			}
			for(int s = 0; s < csg.getNumStates(); s++) {
				sol[0][s] = val[0][s];	
				sol[1][s] = val[1][s];		
			}
			ni++;
			done = done & PrismUtils.doublesAreClose(sol[0], tmp[0], 1e-6, true);
			done = done & PrismUtils.doublesAreClose(sol[1], tmp[1], 1e-6, true);
			//if(Arrays.equals(sol[0], tmp[0]) && Arrays.equals(sol[1], tmp[1])) {
			//	break;  
			//}
			if(done || ni == Math.max(k1, k2)) {
				break;
			}
			else {
				done = true;
				tmp[0] = Arrays.copyOf(sol[0], sol[0].length);
				tmp[1] = Arrays.copyOf(sol[1], sol[1].length);
			}
		}

		System.out.println("\nMDP Computation (bounded) took " + timer/1000.00 + " seconds.");
		
		System.out.println();
		//System.out.println("-- player 1");
		//System.out.println(Arrays.toString(sol[0]));
		System.out.println("-- Result for player 1: " + sol[0][csg.getFirstInitialState()] + " (value in the intial state).");
		//System.out.println("-- player 2");
		//System.out.println(Arrays.toString(sol[1]));
		System.out.println("-- Result for player 2: " + sol[1][csg.getFirstInitialState()] + " (value in the intial state).");
		
		/*** Uncomment this ***/
		if(genAdv)
			csg_mc.stratProduct(csg, adv1, adv2, null, only1, only2, false, true);
								
		return r;
	}
	
	public double[] computeProbBoundedEquilibria(CSG csg, BitSet target1, BitSet target2, BitSet known, 
												double[][] init, int limit, boolean genAdv) throws PrismException {
		int ni = 0;
		boolean done = true;
		double[] r = new double[csg.getNumStates()];
		adv = new HashMap<Integer, ArrayList<Distribution>>();
		adv1 = new HashMap<Integer, Distribution>();
		adv2 = new HashMap<Integer, Distribution>();

		double[][] val = new double[2][csg.getNumStates()];
		double[][] sol = new double[2][csg.getNumStates()];
		double[][] tmp = new double[2][csg.getNumStates()];
		Arrays.fill(val[0], 0);
		Arrays.fill(sol[0], 0);
		Arrays.fill(tmp[0], 0);
		Arrays.fill(val[1], 0);
		Arrays.fill(sol[1], 0);
		Arrays.fill(tmp[1], 0);
		double[][] eq;
		double[] meq;
		
		NashSMT nash = new NashSMT();
		
		if(known == null) {
			known = new BitSet();
		}
		
		BitSet all = new BitSet();
		all.set(0, csg.getNumStates());
		BitSet only1 = (BitSet) target1.clone();
		BitSet only2 = (BitSet) target2.clone();
		BitSet both = (BitSet) target1.clone();
		
		only1.andNot(target2);
		only2.andNot(target1);
		both.and(target2);
		
		known.or(only1);
		known.or(only2);
		known.or(both);
		
		ArrayList<ArrayList<String>> mgame;
		ArrayList<ArrayList<Distribution>> lstrat;

		for(int s = 0; s < csg.getNumStates(); s++) {
			sol[0][s] = val[0][s] = target1.get(s)? 1.0 : 0.0;
			sol[1][s] = val[1][s] = target2.get(s)? 1.0 : 0.0;
		}

		mgame = new ArrayList<ArrayList<String>>();
		lstrat = new ArrayList<ArrayList<Distribution>>();

		double[] only1v = new double[csg.getNumStates()];
		double[] only2v = new double[csg.getNumStates()];
		
		long timer = 0, curr;
				
		while(true) {
			//System.out.println("\n################### i " + ni + " ###################");
			//System.out.println(Arrays.toString(sol[0]));
			//System.out.println(Arrays.toString(sol[1]));
			
			curr = System.currentTimeMillis();
			only1v = csg_mc.mdp_mc.computeBoundedReachProbs(csg, target2, ni, false).soln;
			only2v = csg_mc.mdp_mc.computeBoundedReachProbs(csg, target1, ni, false).soln;
			curr = System.currentTimeMillis() - curr;
			timer += curr;
			
			for(int s = 0; s < csg.getNumStates(); s++) {
				if(only1.get(s)) {
					sol[0][s] = 1.0;
					sol[1][s] = only1v[s];
					//System.out.println(only1v[s]);
				}
				else if(only2.get(s)) {
					sol[0][s] = only2v[s];
					sol[1][s] = 1.0;
					//System.out.println(only2v[s]);
				}
				else if(both.get(s)) {
					sol[0][s] = 1.0;
					sol[1][s] = 1.0;
				}	
			}
			
			for(int s = 0; s < csg.getNumStates(); s++) {
				lstrat.clear();
				//System.out.println("val 0 " + Arrays.toString(val[0]));
				//System.out.println("val 1 " + Arrays.toString(val[1]));
				
				if(!known.get(s)) {
					//System.out.println("\n--- s " + s);
					if(genAdv) {		
						eq = stepEquilibria(csg, null, null, nash, mgame, lstrat, sol, s, genAdv);
					}
					else {
						eq = stepEquilibria(csg, null, null, nash, null, null, sol, s, genAdv);
					}
					/*
					if(genAdv) {	
						meq = maxEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
						adv.put(s, lstrat.get(0));
						adv1.put(s, lstrat.get(0).get(0));
						adv2.put(s, lstrat.get(0).get(1));
						}
					else {
						meq = maxEq(eq, null, false);		
					}
					val[0][s] = meq[0];
					val[1][s] = meq[1];
					 */
					if(genAdv) {
						meq = socialWelfareEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
						adv.put(s, lstrat.get(0));
						adv1.put(s, lstrat.get(0).get(0));
						adv2.put(s, lstrat.get(0).get(1));
					}
					else {
						meq = socialWelfareEq(eq, null, false);		
					}
					val[0][s] = meq[1];
					val[1][s] = meq[2];
				}
				r[s] = val[0][s] + val[1][s];
			}
			for(int s = 0; s < csg.getNumStates(); s++) {
				sol[0][s] = val[0][s];	
				sol[1][s] = val[1][s];		
			}
			ni++;
			done = done & PrismUtils.doublesAreClose(sol[0], tmp[0], 1e-6, true);
			done = done & PrismUtils.doublesAreClose(sol[1], tmp[1], 1e-6, true);
			//if(Arrays.equals(sol[0], tmp[0]) && Arrays.equals(sol[1], tmp[1])) {
			//	break;  
			//}
			if(done || ni == limit) {
				break;
			}
			else {
				done = true;
				tmp[0] = Arrays.copyOf(sol[0], sol[0].length);
				tmp[1] = Arrays.copyOf(sol[1], sol[1].length);
			}
		}
		
		System.out.println("\nMDP Computation (bounded) took " + timer/1000.00 + " seconds.");
		
		System.out.println();
		//System.out.println("-- player 1");
		//System.out.println(Arrays.toString(sol[0]));
		System.out.println("-- Result for player 1: " + sol[0][csg.getFirstInitialState()] + " (value in the intial state).");
		//System.out.println("-- player 2");
		//System.out.println(Arrays.toString(sol[1]));
		System.out.println("-- Result for player 2: " + sol[1][csg.getFirstInitialState()] + " (value in the intial state).");
				
		return r;
	}
	
	public double[] computeRewBoundedEquilibria(CSG csg, CSGRewards csgRewards1, CSGRewards csgRewards2, int limit, boolean genAdv) throws PrismException {
		int ni = 0;
		boolean done = true;
		double[] r = new double[csg.getNumStates()];
		adv = new HashMap<Integer, ArrayList<Distribution>>();
		adv1 = new HashMap<Integer, Distribution>();
		adv2 = new HashMap<Integer, Distribution>();

		double[][] val = new double[2][csg.getNumStates()];
		double[][] sol = new double[2][csg.getNumStates()];
		double[][] tmp = new double[2][csg.getNumStates()];
		Arrays.fill(val[0], 0);
		Arrays.fill(sol[0], 0);
		Arrays.fill(tmp[0], 0);
		Arrays.fill(val[1], 0);
		Arrays.fill(sol[1], 0);
		Arrays.fill(tmp[1], 0);
		double[][] eq;
		double[] meq;

		NashSMT nash = new NashSMT();

		BitSet all = new BitSet();
		all.set(0, csg.getNumStates());

		ArrayList<ArrayList<String>> mgame;
		ArrayList<ArrayList<Distribution>> lstrat;

		mgame = new ArrayList<ArrayList<String>>();
		lstrat = new ArrayList<ArrayList<Distribution>>();

		double[] only1v = new double[csg.getNumStates()];
		double[] only2v = new double[csg.getNumStates()];

		long timer = 0, curr;

		while(true) {
			//System.out.println("\n################### i " + ni + " ###################");
			//System.out.println(Arrays.toString(sol[0]));
			//System.out.println(Arrays.toString(sol[1]));

			curr = System.currentTimeMillis();
			only1v = csg_mc.mdp_mc.computeCumulativeRewards(csg, csgRewards2, ni, false).soln;
			only2v = csg_mc.mdp_mc.computeCumulativeRewards(csg, csgRewards1, ni, false).soln;
			curr = System.currentTimeMillis() - curr;
			timer += curr;

			for(int s = 0; s < csg.getNumStates(); s++) {
				lstrat.clear();
				//System.out.println("val 0 " + Arrays.toString(val[0]));
				//System.out.println("val 1 " + Arrays.toString(val[1]));

				//System.out.println("\n--- s " + s);
				if(genAdv) {		
					eq = stepEquilibria(csg, null, null, nash, mgame, lstrat, sol, s, genAdv);
				}
				else {
					eq = stepEquilibria(csg, null, null, nash, null, null, sol, s, genAdv);
				}
				/*
				if(genAdv) {	
					meq = maxEq(eq, lstrat, false);
					//System.out.println("-- lstrat" + lstrat);
					adv.put(s, lstrat.get(0));
					adv1.put(s, lstrat.get(0).get(0));
					adv2.put(s, lstrat.get(0).get(1));
				}
				else {
					meq = maxEq(eq, null, false);		
				}
				val[0][s] = meq[0];
				val[1][s] = meq[1];
				*/
				if(genAdv) {
					meq = socialWelfareEq(eq, lstrat, false);
					//System.out.println("-- lstrat" + lstrat);
					adv.put(s, lstrat.get(0));
					adv1.put(s, lstrat.get(0).get(0));
					adv2.put(s, lstrat.get(0).get(1));
				}
				else {
					meq = socialWelfareEq(eq, null, false);		
				}
				val[0][s] = meq[1];
				val[1][s] = meq[2];
				r[s] = val[0][s] + val[1][s];
			}
			for(int s = 0; s < csg.getNumStates(); s++) {
				sol[0][s] = val[0][s];	
				sol[1][s] = val[1][s];		
			}
			ni++;
			done = done & PrismUtils.doublesAreClose(sol[0], tmp[0], 1e-6, true);
			done = done & PrismUtils.doublesAreClose(sol[1], tmp[1], 1e-6, true);
			//if(Arrays.equals(sol[0], tmp[0]) && Arrays.equals(sol[1], tmp[1])) {
			//	break;  
			//}
			if(done || ni == limit) {
				break;
			}
			else {
				done = true;
				tmp[0] = Arrays.copyOf(sol[0], sol[0].length);
				tmp[1] = Arrays.copyOf(sol[1], sol[1].length);
			}
		}

		System.out.println("\nMDP Computation (bounded) took " + timer/1000.00 + " seconds.");

		System.out.println();
		//System.out.println("-- player 1");
		//System.out.println(Arrays.toString(sol[0]));
		System.out.println("-- Result for player 1: " + sol[0][csg.getFirstInitialState()] + " (value in the intial state).");
		//System.out.println("-- player 2");
		//System.out.println(Arrays.toString(sol[1]));
		System.out.println("-- Result for player 2: " + sol[1][csg.getFirstInitialState()] + " (value in the intial state).");

		return r;
	}
	
	public double[] computeEquilibria(CSG csg, CSGRewards csgRewards1, CSGRewards csgRewards2, 
										ArrayList<Strategy> mdpStrat, BitSet target1, BitSet target2, BitSet known, 
										double[][] init, int limit, boolean genAdv) throws PrismException {
		int ni = 0;
		boolean done = true;
		double[] r = new double[csg.getNumStates()];
		adv = new HashMap<Integer, ArrayList<Distribution>>();
		adv1 = new HashMap<Integer, Distribution>();
		adv2 = new HashMap<Integer, Distribution>();
		
		//System.out.println("\n-- Compute Equilibria");
		
	    //System.loadLibrary("cvc4jni");
	    //List<String> players = new ArrayList<String>();
		//players.add("p1");
		//players.add("s");
		//Coalition coal = new Coalition();
		//coal.setPlayers(players);
		
		//change set coalition
		//csg_mc.setCoalition(csg, null, coal, false, true);
		
		double[][] val = new double[2][csg.getNumStates()];
		double[][] sol = new double[2][csg.getNumStates()];
		double[][] tmp = new double[2][csg.getNumStates()];
		Arrays.fill(val[0], 0);
		Arrays.fill(sol[0], 0);
		Arrays.fill(tmp[0], 0);
		Arrays.fill(val[1], 0);
		Arrays.fill(sol[1], 0);
		Arrays.fill(tmp[1], 0);
		double[][] eq;
		double[] meq;
				
		NashSMT nash = new NashSMT();
		
		if(known == null) {
			known = new BitSet();
		}
		
		BitSet only1 = new BitSet();
		BitSet only2 = new BitSet();
		BitSet both = new BitSet();
		
		if(target1 != null && target2 !=null) {
			only1 = (BitSet) target1.clone();
			only1.andNot(target2);
			
			only2 = (BitSet) target2.clone();
			only2.andNot(target1);
		
			both = (BitSet) target1.clone();
			both.and(target2);
		}
		/*
		System.out.println("-- target1  " + target1);
		System.out.println("-- only1  " + only1);
		System.out.println("-- target2 " + target2);
		System.out.println("-- only2  " + only2);
		System.out.println("-- both " + both);
		*/
		boolean rew = (csgRewards1 != null) && (csgRewards2 != null);
		ArrayList<ArrayList<String>> mgame;
		ArrayList<ArrayList<Distribution>> lstrat;
		
		if(init !=null) {
			//System.out.println("-- init0 " + Arrays.toString(init[0]));
			//System.out.println("-- init1 " + Arrays.toString(init[1]));
			if(!rew) {
				for(int s = 0; s < csg.getNumStates(); s++) {
					sol[0][s] = val[0][s] = target1.get(s)? 1.0 : only2.get(s)? init[0][s] : 0.0;
					sol[1][s] = val[1][s] = target2.get(s)? 1.0 : only1.get(s)? init[1][s] : 0.0;
					r[s] = both.get(s)? 1.0 : 0.0;
				}
			}
			else {
				for(int s = 0; s < csg.getNumStates(); s++) {
					sol[0][s] = val[0][s] = target1.get(s)? 0.0 : only2.get(s)? init[0][s] : 0.0;
					sol[1][s] = val[1][s] = target2.get(s)? 0.0 : only1.get(s)? init[1][s] : 0.0;
				}	
			}
		}
		
		//System.out.println("-- after initialization");
		//System.out.println("-- init0 " + Arrays.toString(sol[0]));
		//System.out.println("-- init1 " + Arrays.toString(sol[1]));
		
		/*
		if(init != null) {
			for(int s = 0; s < csg.getNumStates(); s++) {
				sol[0][s] = val[0][s] = known.get(s) ? target1.get(s) ? 1.0 : init[0][s] : 0.0;
				sol[1][s] = val[1][s] = known.get(s) ? target2.get(s) ? 1.0 : init[1][s] : 0.0;
			}
		}
		*/
		
		mgame = new ArrayList<ArrayList<String>>();
		lstrat = new ArrayList<ArrayList<Distribution>>();
		
	    while(true) {
			//System.out.println("\n################### i " + ni + " ###################");
			//System.out.println(Arrays.toString(sol[0]));
			//System.out.println(Arrays.toString(sol[1]));
	    	for(int s = 0; s < csg.getNumStates(); s++) {
				lstrat.clear();
				//System.out.println("val 0 " + Arrays.toString(val[0]));
				//System.out.println("val 1 " + Arrays.toString(val[1]));
	    		if(!known.get(s)) {
	    			//System.out.println("\n--- s " + s);
	    			if(genAdv) {		
		    			eq = stepEquilibria(csg, csgRewards1, csgRewards2, nash, mgame, lstrat, sol, s, genAdv);
	    			}
	    			else {
	    				eq = stepEquilibria(csg, csgRewards1, csgRewards2, nash, null, null, sol, s, genAdv);
	    			}
	    			/*
	    			if(genAdv) {
						meq = maxEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
		    			adv.put(s, lstrat.get(0));
		    			adv1.put(s, lstrat.get(0).get(0));
		    			adv2.put(s, lstrat.get(0).get(1));
	    			}
	    			else {
 						meq = maxEq(eq, null, false);		
	    			}
					val[0][s] = meq[0];
	    			val[1][s] = meq[1];
	    			*/
	    			
					if(genAdv) {
						meq = socialWelfareEq(eq, lstrat, false);
						//System.out.println("-- lstrat" + lstrat);
		    			adv.put(s, lstrat.get(0));
		    			adv1.put(s, lstrat.get(0).get(0));
		    			adv2.put(s, lstrat.get(0).get(1));
					}
					else {
 						meq = socialWelfareEq(eq, null, false);		
					}
					val[0][s] = meq[1];
	    			val[1][s] = meq[2];
					
	    		}
    			r[s] = val[0][s] + val[1][s];
	    	}
			for(int s = 0; s < csg.getNumStates(); s++) {
				sol[0][s] = val[0][s];	
				sol[1][s] = val[1][s];		
			}
			ni++;
			done = done & PrismUtils.doublesAreClose(sol[0], tmp[0], 1e-6, true);
			done = done & PrismUtils.doublesAreClose(sol[1], tmp[1], 1e-6, true);
			//if(Arrays.equals(sol[0], tmp[0]) && Arrays.equals(sol[1], tmp[1])) {
			//	break;  
			//}
			if(done || ni == limit) {
				break;
			}
			else {
				done = true;
				tmp[0] = Arrays.copyOf(sol[0], sol[0].length);
				tmp[1] = Arrays.copyOf(sol[1], sol[1].length);
			}
	    }
	    
		System.out.println();
		//System.out.println("-- player 1");
		//System.out.println(Arrays.toString(sol[0]));
		System.out.println("-- Result for player 1: " + sol[0][csg.getFirstInitialState()] + " (value in the intial state).");
		//System.out.println("-- player 2");
		//System.out.println(Arrays.toString(sol[1]));
		System.out.println("-- Result for player 2: " + sol[1][csg.getFirstInitialState()] + " (value in the intial state).");
		
	    //System.out.println("-- adv1");
	    //System.out.println(adv1);
	    
	    //System.out.println("-- adv2");
	    //System.out.println(adv2);
		
		/*** Uncomment this ***/	
		if(genAdv)
			csg_mc.stratProduct(csg, adv1, adv2, mdpStrat, only1, only2, false, true);
	  
		/*
		if(!rew) {
			csg_mc.epsilonNE(csg, null, null, adv1, adv2, target1, target2, 
								sol[0][csg.getFirstInitialState()], sol[1][csg.getFirstInitialState()], false, true);
		}
		else {
			csg_mc.epsilonNE(csg, csgRewards1, csgRewards2, adv1, adv2, target1, target2, 
								sol[0][csg.getFirstInitialState()], sol[1][csg.getFirstInitialState()], false, true);
		}
		*/
		
	    //MDPSimple mdp;
	    	    
	    //mdp = csg_mc.stratProduct(csg, adv1, false, true);
	    //csg_mc.exportSrategy(mdp, "adv1_nash");

	    //mdp = csg_mc.stratProduct(csg, adv2, true, false);
	    //csg_mc.exportSrategy(mdp, "adv2_nash");

	    
		/*
		MDPRewards mdp_rewards;
		ModelCheckerResult res;
		
		mdp_rewards = csg_mc.stratRewards(csg, csgRewards2, adv1, false, true);
		res = csg_mc.mdp_mc.computeTotalRewards(mdp, mdp_rewards, false);
		System.out.println("--res2 " + Arrays.toString(res.soln));
				
	    mdp = csg_mc.stratProduct(csg, adv2, true, false);
		
		mdp_rewards = csg_mc.stratRewards(csg, csgRewards1, adv2, true, false);
		res = csg_mc.mdp_mc.computeTotalRewards(mdp, mdp_rewards, false);
		System.out.println("--res1 " + Arrays.toString(res.soln));
		*/
		return r;
	}
	
	public double[][] stepEquilibria(CSG csg, CSGRewards csgRewards1, CSGRewards csgRewards2, 
									 NashSMT nash, ArrayList<ArrayList<String>> mgame,
									 ArrayList<ArrayList<Distribution>> strat,
									 double[][] val, int s, boolean genAdv) throws PrismException {
		ArrayList<ArrayList<Distribution>> mdist = new ArrayList<ArrayList<Distribution>>();
		ArrayList<ArrayList<Double>> p1matrix = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> p2matrix = new ArrayList<ArrayList<Double>>();
		
		int nrows, ncols;
		Distribution dist;

		/*** NEED TO FINSIH STRATEGY SYNTHESIS ***/
		
		double val1, val2;
		val1 = 0.0;
		val2 = 0.0;
		double[][] val1s, val2s, eqs;

		if(mgame != null) {
			mdist = csg_mc.buildProbMatrixGame(csg, mgame, s);
		}
		else {
			mdist = csg_mc.buildProbMatrixGame(csg, null, s);
		}
		
		nrows = mdist.size();
		ncols = mdist.get(0).size();
		val1s = new double[nrows][ncols];
		val2s = new double[nrows][ncols];

		boolean rew = (csgRewards1 != null) && (csgRewards2 != null);
		
		if(rew) {
			p1matrix = csg_mc.buildRewMatrixGame(csg, csgRewards1, s);
			p2matrix = csg_mc.buildRewMatrixGame(csg, csgRewards2, s);
		}
		
		//System.out.println("val 0 " + Arrays.toString(val[0]));
		//System.out.println("val 1 " + Arrays.toString(val[1]));
		
		/*
		System.out.println("\n-- matrices");
		System.out.println("-- p1matrix");
		System.out.println(p1matrix);
		System.out.println("-- p2matrix");
		System.out.println(p2matrix);
		*/
		
		/*** should this be here? ***/
		/*
		if(rew) {
			val[0][s] += csgRewards1.getStateReward(s);
			val[1][s] += csgRewards2.getStateReward(s);
		}
		*/		
		ArrayList<Distribution> pair;
		
		boolean zeroA;
		boolean zeroB;
		int maxRow[] = new int[ncols];
		Arrays.fill(maxRow, 0);
		int maxCol[] = new int[nrows];
		Arrays.fill(maxCol, 0);

		boolean bMaxRow = false;
		boolean bMaxCol = false;
		boolean uRow = true;
		boolean uCol = true;
		
		int mrow, mcol;
		
		//SortedSet<Double> sRow = new TreeSet<Double>();
		//SortedSet<Double> sCol = new TreeSet<Double>();
		int[] mIndxs;
		
		if(nrows > 1 && ncols> 1 ) { // include tests for degenerate games
			//System.out.println("-- equlibria");
			//System.out.println(mdist);
			zeroA = true;
			zeroB = true;
			for(int r = 0; r < nrows; r++) {
				for(int c = 0; c < ncols; c++) {
					dist = mdist.get(r).get(c);
					if(rew) {
						val1 = p1matrix.get(r).get(c);
						val2 = p2matrix.get(r).get(c);
					}
					else {
						val1 = 0.0;
						val2 = 0.0;
					}
					for(Integer t : dist.getSupport()) {		
						val1 += dist.get(t) * val[0][t];
						val2 += dist.get(t) * val[1][t];
					}
					zeroA = zeroA && val1 == 0.0;
					zeroB = zeroB && val2 == 0.0;
					//sRow.add(val1);
					//sCol.add(val2);
					val1s[r][c] = val1;
					val2s[r][c] = val2;		
				}
			}
			
			if(!zeroA) {
				for(int r = 1; r < nrows; r++) {
					for(int c = 0; c < ncols; c++) {
						maxRow[c] = (val1s[r][c] > val1s[r-1][c])? r : maxRow[c]; 
					}
				}
				bMaxRow = allEqual(maxRow);
				if(bMaxRow) {
					for(int r = 0; r < nrows; r++) {
						if(r != maxRow[0]) 
							uRow = uRow && !Arrays.equals(val1s[r], val1s[maxRow[0]]);
					} 
				}
				//uRow = sRow.size() == nrows * ncols;
				bMaxRow = bMaxRow && uRow;
			}
			else {
				bMaxRow = true;
			}
			
			if(!zeroB) { 
				for(int c = 1; c < ncols; c++) {
					for(int r = 0; r < nrows; r++) {
						maxCol[r] = (val2s[r][c] > val2s[r][c-1])? c : maxCol[r];
					}
				}
				bMaxCol = allEqual(maxCol);
				if(bMaxCol) {
					for(int c = 0; c < ncols; c++) {
						if(c != maxCol[0]) {
							uCol = uCol && !Arrays.equals(getCol(val2s,c),getCol(val2s,maxCol[0]));
						}
					}
				}
				//uCol = sCol.size() == nrows * ncols;
				bMaxCol = bMaxCol && uCol;
			}
			else {
				bMaxCol = true;
			}	
			
			/*
			if(bMaxRow) {
				System.out.println("-- maxRow");
				System.out.println(Arrays.toString(maxRow));
				mrow = maxRow[0];
				mcol = getMaxIndex(val2s[mrow]);
				
				System.out.println("-- mrow " + mrow);
				System.out.println("-- mcol " + mcol);
				
				System.out.println("-- A");
				for(int r = 0; r < nrows; r++) {
					System.out.println(Arrays.toString(val1s[r]));
				}
				System.out.println("-- B");
				for(int r = 0; r < nrows; r++) {
					System.out.println(Arrays.toString(val2s[r]));
				}
				
				System.out.println("-- eq1 " + val1s[mrow][mcol]);
				System.out.println("-- eq2 " + val2s[mrow][mcol]);
				
				//System.exit(1);
			}
			if(bMaxCol) {
				System.out.println("-- maxCol");
				System.out.println(Arrays.toString(maxCol));
				mcol = maxCol[0];
				mrow = getMaxIndex(getCol(val1s, mcol));
				
				System.out.println("-- mrow " + mrow);
				System.out.println("-- mcol " + mcol);
				
				System.out.println("-- A");
				for(int r = 0; r < nrows; r++) {
					System.out.println(Arrays.toString(val1s[r]));
				}
				System.out.println("-- B");
				for(int r = 0; r < nrows; r++) {
					System.out.println(Arrays.toString(val2s[r]));
				}
				
				System.out.println("-- eq1 " + val1s[mrow][mcol]);
				System.out.println("-- eq2 " + val2s[mrow][mcol]);
				
				//System.exit(1);
			}
			*/
			
			/*
			System.out.println("-- A");
			for(int r = 0; r < nrows; r++) {
				System.out.println(Arrays.toString(val1s[r]));
			}
			System.out.println("-- B");
			for(int r = 0; r < nrows; r++) {
				System.out.println(Arrays.toString(val2s[r]));
			}
			*/
			
			if(!(zeroA && zeroB)) {
				eqs = new double[1][2];
				if(bMaxRow || bMaxCol) {
					if(zeroA) {
						mIndxs = findMaxIndexes(val2s);
						mrow = mIndxs[0];
						mcol = mIndxs[1];
					}
					else if(zeroB) {
						mIndxs = findMaxIndexes(val1s);
						mrow = mIndxs[0];
						mcol = mIndxs[1];
					}
					else if(bMaxRow && bMaxCol) {
						mrow = maxRow[0];
						mcol = maxCol[0];
					} 
					else if(bMaxRow) {
						mrow = maxRow[0];
						mcol = getMaxIndex(val2s[mrow]);
					}
					else {
						mcol = maxCol[0];
						mrow = getMaxIndex(getCol(val1s, mcol));
					}
					eqs[0][0] = val1s[mrow][mcol];
					eqs[0][1] = val2s[mrow][mcol];
					if(genAdv) {
						pair = new ArrayList<Distribution>();
						Distribution d = new Distribution();
						d.set(mrow, 1.0);
						pair.add(0, copyDistribution(d));
						d.clear();
						d.set(mcol, 1.0);
						pair.add(1, copyDistribution(d));
						strat.add(0, pair);
					}
					if(rew) {
						eqs[0][0] += csgRewards1.getStateReward(s);
						eqs[0][1] += csgRewards2.getStateReward(s);
					}
				}
				else {
					//System.out.println("nash call");
					nash.update(nrows, ncols, val1s, val2s);
					nash.compEq();
					nash.compPayoffs();
					//nash.print();
					if(nash.getP1p().length > 1) {
						//System.out.println("Multiple equilibria: " + nash.getNeq());
					}
					eqs = new double[nash.getNeq()][2];
					for(int e = 0; e < nash.getNeq(); e++) {
						eqs[e][0] = nash.getP1p()[e];
						eqs[e][1] = nash.getP2p()[e];
						if(genAdv) {
							pair = new ArrayList<Distribution>();
							for(int p = 0; p < 2; p++) {
								//System.out.println(p + " " + nash.getStrat().get(e).get(p));
								pair.add(p, copyDistribution(nash.getStrat().get(e).get(p)));
							} 
							strat.add(e, pair);
						}
						if(rew) {
							eqs[e][0] += csgRewards1.getStateReward(s);
							eqs[e][1] += csgRewards2.getStateReward(s);
						}
					}
				}
			}
			else {
				eqs = new double[1][2];
				eqs[0][0] = 0.0;
				eqs[0][1] = 0.0;
				if(genAdv) {
					pair = new ArrayList<Distribution>();
					Distribution d = new Distribution();
					d.set(0, 1.0);
					pair.add(0, copyDistribution(d));
					pair.add(1, copyDistribution(d));
					strat.add(0, pair);
				}
				if(rew) {
					eqs[0][0] += csgRewards1.getStateReward(s);
					eqs[0][1] += csgRewards2.getStateReward(s);
				}
			}
		}
		else {
			eqs = new double[1][2];
			double vt1, vt2;
			Distribution d1 = new Distribution();
			Distribution d2 = new Distribution();
			val1 = Double.NEGATIVE_INFINITY; // minus infinity?
			val2 = Double.NEGATIVE_INFINITY;  // minus infinity?
			if(nrows > 1 && ncols == 1) {
				/*
				System.out.println("\n-- column matrix");
				System.out.println("-- matrices");
				System.out.println("-- p1matrix");
				System.out.println(p1matrix);
				System.out.println("-- p2matrix");
				System.out.println(p2matrix);
				*/
				for(int r = 0; r < nrows; r++) {
					if(rew) {
						vt1 = p1matrix.get(r).get(0);
						vt2 = p2matrix.get(r).get(0);
					}
					else {
						vt1 = 0.0;
						vt2 = 0.0;
					}
					dist = mdist.get(r).get(0);
					for(int t : dist.getSupport()) {
						vt1 += dist.get(t) * val[0][t];
						vt2 += dist.get(t) * val[1][t];
					}
					if(vt1 > val1) {
						if(genAdv) {
							d1.clear();
							d1.add(r, 1.0);
						}
						val2 = vt2;
						val1 = vt1;
					}
				}
				d2.add(0, 1.0);
			} else if(nrows == 1 && ncols > 1) {
				/*
				System.out.println("\n-- row matrix");
				System.out.println("-- matrices");
				System.out.println("-- p1matrix");
				System.out.println(p1matrix);
				System.out.println("-- p2matrix");
				System.out.println(p2matrix);
				*/
				for(int c = 0; c < ncols; c++) {
					if(rew) {
						vt1 = p1matrix.get(0).get(c);
						vt2 = p2matrix.get(0).get(c);
					}
					else {
						vt1 = 0.0;
						vt2 = 0.0;
					}
					dist = mdist.get(0).get(c);
					for(int t : dist.getSupport()) {
						vt1 += dist.get(t) * val[0][t];
						vt2 += dist.get(t) * val[1][t];
					}
					if(vt2 > val2) {
						if(genAdv) {
							d2.clear();
							d2.add(c, 1.0);
						}
						val2 = vt2;
						val1 = vt1;
					}
				}
				d1.add(0, 1.0);
			} else if(nrows == 1 && ncols == 1) {
				dist = mdist.get(0).get(0);
				if(rew) {
					val1 = p1matrix.get(0).get(0);
					val2 = p2matrix.get(0).get(0);
				}
				else {
					val1 = 0.0;
					val2 = 0.0;
				}
				for(int t : dist.getSupport()) {
					val1 += dist.get(t) * val[0][t];
					val2 += dist.get(t) * val[1][t];
				}
				d1.add(0, 1.0);
				d2.add(0, 1.0);
			} else {
				throw new PrismException("Error with matrix rank");
			}
			if(genAdv) {
				pair = new ArrayList<Distribution>();
				pair.add(0, copyDistribution(d1));
				pair.add(1, copyDistribution(d2));	 
				strat.add(0, pair);
			} 
			eqs[0][0] = val1;
			eqs[0][1] = val2;
			
			if(rew) {
				eqs[0][0] += csgRewards1.getStateReward(s);
				eqs[0][1] += csgRewards2.getStateReward(s);
			}
		}
		
		//System.out.println(csgRewards1.getStateReward(s));
		//System.out.println(csgRewards2.getStateReward(s));
		/*
		System.out.println("-- state " + s);
		System.out.println(eqs[0][0]);
		System.out.println(eqs[0][1]);
		*/
		/*
		System.out.println("-- step equilibrium");
		for(int e = 0; e < eqs.length; e++) {
			System.out.println("- equilibrium " + e);
			System.out.println("- p1 " + eqs[e][0]);
			System.out.println("- p2 " + eqs[e][1]);
		}
		*/
		return eqs;
	}
	
	public double[][] computeNash(NashSMT nash, double[][] a, double[][] b, int nrows, int ncols) {
		nash.update(nrows, ncols, a, b);
		nash.compEq();
		nash.compPayoffs();
		//nash.print();
		if(nash.getP1p().length > 1) {
			System.out.println("Multiple equilibria: " + nash.getNeq());
		}
		double[][] eqs = new double[nash.getNeq()][2];
		for(int e = 0; e < nash.getNeq(); e++) {
			eqs[e][0] = nash.getP1p()[e];
			eqs[e][1] = nash.getP2p()[e];
			System.out.println("p1 " + eqs[e][0]);
			System.out.println("p2 " + eqs[e][1]);
		}
		return eqs;
	}
	
	public Distribution copyDistribution(Distribution d1) {
		Distribution d = new Distribution();
		for(int t : d1.getSupport()) {
			d.add(t, d1.get(t));
		}
		return d;
	}
	
	public boolean allEqual(int[] a) {
		boolean r = true;
		for(int e = 0; e < a.length-1; e++) {
			r = r && (a[e] == a[e+1]);
		}
		return r;
	}
	
	public boolean allEqual(double[][] a) {
		boolean result = true;
		double e = a[0][0];
		for(int r = 0; r < a.length; r++) {
			for(int c = 0; c < a[r].length; c++) {
				if(e != a[r][c]) {
					return false;
				}
				else {
					e = a[r][c];
				}
			}
		}
		return result;
	}
	
	public boolean allDifferent(double[][] a) {
		boolean result;
		Set<Double> s = new HashSet<Double>();
		for(int r = 0; r < a.length; r++) {
			for(int c = 0; c < a[r].length; c++) {
				s.add(a[r][c]);
			}
		}
		result = s.size() == a.length *a[0].length;
		return result;
	}
	
	public double[] getCol(double[][] a, int c) {
		double[] col = new double[a.length];
		for(int r = 0; r < a.length; r++) {
			col[r] = a[r][c];
		}
		return col;
	}
	
	public int[] findMaxIndexes(double[][] a) {
		int result[] = new int[2];
		result[0] = 0;
		result[1] = 0;	
		double value = 0.0;
		for(int r = 0; r < a.length; r++) {
			for(int c = 0; c < a[r].length; c++) {
				if(a[r][c] > value) {
					value = a[r][c];
					result[0] = r;
					result[1] = c;	
				}
			}
		}		
		return result;
	}
	
	public double findMaxValue(double[][] a) {
		double result = 0.0;
		for(int r = 0; r < a.length; r++) {
			for(int c = 0; c < a[r].length; c++) {
				result = (a[r][c] > result)? a[r][c] : result;
			}
		}
		return result;
	}
	
	public int getMaxIndex(double[] a) {
		int r = 0;
		double e = a[0];
		for(int l = 1; l < a.length; l++) {
			if(a[l] > e) {
				r = l;
				e = a[l];
			}
		}
		return r;
	}
}
