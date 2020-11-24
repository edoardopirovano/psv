//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit.rewards;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.State;
import parser.Values;
import parser.ast.Expression;
import parser.ast.RewardStruct;
import parser.ast.SRewardStruct;
import parser.ast.SRewardStructItem;
import parser.ast.ModulesFile;
import prism.ModelGenerator;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import explicit.CSG;
import explicit.DTMC;
import explicit.MDP;
import explicit.Model;
import explicit.SMG;
import explicit.STPG;

public class ConstructRewards
{
	protected PrismLog mainLog;
	protected ModulesFile modulesFile;

	/** Allow negative rewards, i.e., weights. Defaults to false. */
	protected boolean allowNegative = false;

	public ConstructRewards(ModulesFile modulesFile)
	{
	        this(new PrismFileLog("stdout"), modulesFile);
	}

	public ConstructRewards(PrismLog mainLog)
	{
	        this(mainLog, null);
	}

	public ConstructRewards(PrismLog mainLog, ModulesFile modulesFile)
	{
		this.mainLog = mainLog;
		this.modulesFile = modulesFile;
	}

	/** Set flag that negative rewards are allowed, i.e., weights */
	public void allowNegativeRewards()
	{
		allowNegative = true;
	}

	/**
	 * Construct rewards from a model and reward structure. 
	 * @param model The model
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public Rewards buildRewardStructure(Model model, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			return buildMCRewardStructure((DTMC) model, rewStr, constantValues);
		case MDP:
			return buildMDPRewardStructure((MDP) model, rewStr, constantValues);
		case STPG:
			return buildSTPGRewardStructure((STPG) model, rewStr, constantValues);
		case SMG:
			return buildSMGRewardStructure((SMG) model, rewStr, constantValues);
		case CSG:
			return buildCSGRewardStructure((CSG) model, rewStr, constantValues);
		default:
			throw new PrismNotSupportedException("Cannot build rewards for " + model.getModelType() + "s");
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a model and reward structure. 
	 * @param mc The DTMC or CTMC
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public MCRewards buildMCRewardStructure(DTMC mc, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		int i, j, n, numStates;

		if (rewStr.getNumTransItems() > 0) {
			// TODO
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}
		// Special case: constant rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			if (Double.isNaN(rew))
				throw new PrismLangException("Reward structure evaluates to NaN (at any state)", rewStr.getReward(0));
			if (!allowNegative && rew < 0)
				throw new PrismLangException("Reward structure evaluates to " + rew + " (at any state), negative rewards not allowed", rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state rewards
		else {
			numStates = mc.getNumStates();
			statesList = mc.getStatesList();
			StateRewardsArray rewSA = new StateRewardsArray(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				for (j = 0; j < numStates; j++) {
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
						if (Double.isNaN(rew))
							throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
						if (!allowNegative && rew < 0)
							throw new PrismLangException("Reward structure evaluates to " + rew + " at state " + statesList.get(j) +", negative rewards not allowed", rewStr.getReward(i));
						rewSA.addToStateReward(j, rew);
					}
				}
			}
			return rewSA;
		}
	}

	/**
	 * Construct the rewards for an MDP from a model and reward structure. 
	 * @param mdp The MDP
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public MDPRewards buildMDPRewardStructure(MDP mdp, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		String action;
		Object mdpAction;
		int i, j, k, n, numStates, numChoices;

		// Special case: constant state rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			if (Double.isNaN(rew))
				throw new PrismLangException("Reward structure evaluates to NaN (at any state)", rewStr.getReward(0));
			if (!allowNegative && rew < 0)
				throw new PrismLangException("Reward structure evaluates to " + rew + " (at any state), negative rewards not allowed", rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state and transition rewards
		else {
			numStates = mdp.getNumStates();
			statesList = mdp.getStatesList();
			MDPRewardsSimple rewSimple = new MDPRewardsSimple(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				action = rewStr.getSynch(i);
				for (j = 0; j < numStates; j++) {
					// Is guard satisfied?
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						// Transition reward
						if (rewStr.getRewardStructItem(i).isTransitionReward()) {
							if (mdp.isDeadlockState(j)) {
								// As state j is a deadlock state, any outgoing transition
								// was added to "fix" the deadlock and thus does not get a reward.
								// Skip to next state
								continue;
							}
							numChoices = mdp.getNumChoices(j);
							for (k = 0; k < numChoices; k++) {
								mdpAction = mdp.getAction(j, k);
								if (mdpAction == null ? (action.isEmpty()) : mdpAction.equals(action)) {
									double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
									if (Double.isNaN(rew))
										throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
									if (!allowNegative && rew < 0)
										throw new PrismLangException("Reward structure evaluates to " + rew + " at state " + statesList.get(j) +", negative rewards not allowed", rewStr.getReward(i));
									rewSimple.addToTransitionReward(j, k, rew);
								}
							}
						}
						// State reward
						else {
							double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
							if (Double.isNaN(rew))
								throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
							if (!allowNegative && rew < 0)
								throw new PrismLangException("Reward structure evaluates to " + rew + " at state " + statesList.get(j) +", negative rewards not allowed", rewStr.getReward(i));
							rewSimple.addToStateReward(j, rew);
						}
					}
				}
			}
			return rewSimple;
		}
	}

	/**
	 * Construct the rewards for an STPG from a model and reward structure. 
	 * @param stpg The STPG
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public STPGRewards buildSTPGRewardStructure(STPG stpg, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		Expression guard;
		String action;
		Object stpgAction;
		int i, s, j, k, numItems, numStates, numChoices, numChoices2;

		// Special case: constant state rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			if (Double.isNaN(rew))
				throw new PrismLangException("Reward structure evaluates to NaN (at any state)", rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state and transition rewards
		else {
			numStates = stpg.getNumStates();
			statesList = stpg.getStatesList();
			STPGRewardsSimple rewSimple = new STPGRewardsSimple(numStates);
			numItems = rewStr.getNumItems();
			for (i = 0; i < numItems; i++) {
				guard = rewStr.getStates(i);
				action = rewStr.getSynch(i);
				for (s = 0; s < numStates; s++) {
					// Is guard satisfied?
					if (guard.evaluateBoolean(constantValues, statesList.get(s))) {
						// Transition reward
						if (rewStr.getRewardStructItem(i).isTransitionReward()) {
							numChoices = stpg.getNumChoices(s);
							for (j = 0; j < numChoices; j++) {
								stpgAction = stpg.getAction(s, j);
								double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(s));
								if (Double.isNaN(rew))
									throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(s), rewStr.getReward(i));
								if (stpgAction == null ? (action.isEmpty()) : stpgAction.equals(action)) {
									rewSimple.addToTransitionReward(s, j, rew);
								}
								numChoices2 = stpg.getNumNestedChoices(s, j);
								for (k = 0; k < numChoices2; k++) {
									stpgAction = stpg.getNestedAction(s, j, k);
									if (stpgAction == null ? (action.isEmpty()) : stpgAction.equals(action)) {
										rewSimple.addToNestedTransitionReward(s, j, k, rew);
									}
								}
							}
						}
						// State reward
						else {
							double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(s));
							if (Double.isNaN(rew))
								throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(s), rewStr.getReward(i));
							rewSimple.addToStateReward(s, rew);
						}
					}
				}
			}
			return rewSimple;
		}
	}

	/**
	 * Construct the rewards for an SMG from a model and reward structure. 
	 * @param smg The SMG
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public SMGRewards buildSMGRewardStructure(SMG smg, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		SMGRewardsSimple rewSimple;
		Expression guard;
		String action;
		Object smgAction;
		int i, j, k, n, numStates, numChoices;

		// Special case: constant state rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			if (Double.isNaN(rew))
				throw new PrismLangException("Reward structure evaluates to NaN (at any state)", rewStr.getReward(0));
			return new StateRewardsConstant(rew);
		}
		// Normal: state and transition rewards
		else {
			numStates = smg.getNumStates();
			statesList = smg.getStatesList();
			rewSimple = new SMGRewardsSimple(numStates);
			n = rewStr.getNumItems();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				action = rewStr.getSynch(i);
				for (j = 0; j < numStates; j++) {
					// Is guard satisfied?
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						// Transition reward
						if (rewStr.getRewardStructItem(i).isTransitionReward()) {
							numChoices = smg.getNumChoices(j);
							for (k = 0; k < numChoices; k++) {
								smgAction = smg.getAction(j, k);
								if (smgAction == null ? (action.isEmpty()) : smgAction.equals(action)) {
									double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
									if (Double.isNaN(rew))
										throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
									rewSimple.addToTransitionReward(j, k, rew);
								}
							}
						}
						// State reward
						else {
							double rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
							if (Double.isNaN(rew))
								throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
							rewSimple.addToStateReward(j, rew);
						}
					}
				}
			}
			return rewSimple;
		}
	}

	/**
	 * Construct the rewards for a CSG from a model and reward structure. 
	 * @param csg The CSG
	 * @param rewStr The reward structure
	 * @param constantValues Values for any undefined constants needed
	 */
	public CSGRewards buildCSGRewardStructure(CSG csg, RewardStruct rewStr, Values constantValues) throws PrismException
	{
		List<State> statesList;
		CSGRewardsSimple rewSimple;
		Expression guard;
		String action;
		String csgAction;
		int i, j, k, m, n, numStates, numChoices;
		// Special case: constant state rewards
		if (rewStr.getNumStateItems() == 1 && Expression.isTrue(rewStr.getStates(0)) && rewStr.getReward(0).isConstant()) {
			double rew = rewStr.getReward(0).evaluateDouble(constantValues);
			if (Double.isNaN(rew))
				throw new PrismLangException("Reward structure evaluates to NaN (at any state)", rewStr.getReward(0));
			return (CSGRewards) new StateRewardsConstant(rew);
		}
		// Normal: state and transition rewards
		else {
			numStates = csg.getNumStates();
			statesList = csg.getStatesList();
			rewSimple = new CSGRewardsSimple(numStates);
			n = rewStr.getNumItems();
			double rew;
			//System.out.println(n + " items");
			String g1;
			String g2;		
			Pattern pattern1 = Pattern.compile("(\\[\\w+(\\,\\w+)*\\])");
			Pattern pattern2 = Pattern.compile("\\w+");
			Matcher matcher1;
			Matcher matcher2;
			Set<String> actions = new HashSet<String>();
			Set<String> csgActions = new HashSet<String>();
			for (i = 0; i < n; i++) {
				guard = rewStr.getStates(i);
				action = rewStr.getSynch(i);
				//System.out.println("-- action " + action);
				//System.out.println(((SRewardStructItem) rewStr.getRewardStructItem(i)).getSynchs());
				for (j = 0; j < numStates; j++) {
					if(rewStr instanceof SRewardStruct) {
						actions.clear();
						actions.addAll(((SRewardStructItem) rewStr.getRewardStructItem(i)).getSynchs());
					}
					//System.out.println("-- actions " + actions);
					if (guard.evaluateBoolean(constantValues, statesList.get(j))) {
						// Transition reward
						if (rewStr.getRewardStructItem(i).isTransitionReward()) {
							//System.out.println("-- transition reward");
							numChoices = csg.getNumChoices(j);
							rew = 0.0;
							for (k = 0; k < numChoices; k++) {									
								if(rewStr instanceof SRewardStruct) {
									csgAction = (String) csg.getAction(j, k);
									csgActions.clear();
									matcher1 = pattern1.matcher(csgAction);
									while(matcher1.find()) {
										g1 = matcher1.group();
										matcher2 = pattern2.matcher(g1);
										while(matcher2.find()) {
											g2 = matcher2.group();
											//System.out.println(g2);
											csgActions.add(g2);
										}
									}	
									// will need to change giving nae "" in order to add state rewards
									//System.out.println("-- csgActions " + csgActions);
									if (!actions.isEmpty() && !csgActions.isEmpty() && actions.containsAll(csgActions)) {
										rew += rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
										if (Double.isNaN(rew))
											throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
										rewSimple.addToTransitionReward(j, k, rew);
									}
								}
								else {
									rew = 0.0;
									for (k = 0; k < numChoices; k++) {
										csgAction = (String) csg.getAction(j, k);
										if (csgAction == null ? (action.isEmpty()) : csgAction.contains(action)) {
											rew += rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
											//System.out.println(csgAction + " " + rew);
											if (Double.isNaN(rew))
												throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
											rewSimple.addToTransitionReward(j, k, rew);
										}
									}
								}
							}
						}
						// State reward
						else {
							rew = rewStr.getReward(i).evaluateDouble(constantValues, statesList.get(j));
							if (Double.isNaN(rew))
								throw new PrismLangException("Reward structure evaluates to NaN at state " + statesList.get(j), rewStr.getReward(i));
							rewSimple.addToStateReward(j, rew);
						}
					}
				}
			}		
		}
		return rewSimple;
	}
	
	
	/**
	 * Construct the rewards for a model from a model generator. 
	 * @param model The model
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public Rewards buildRewardStructure(Model model, ModelGenerator modelGen, int r) throws PrismException
	{
		switch (model.getModelType()) {
		case DTMC:
		case CTMC:
			return buildMCRewardStructure((DTMC) model, modelGen, r);
		case MDP:
			return buildMDPRewardStructure((MDP) model, modelGen, r);
		case STPG:
			return buildSTPGRewardStructure((STPG) model, modelGen, r);
		case SMG:
			return buildSMGRewardStructure((SMG) model, modelGen, r);
		case CSG:
			return buildCSGRewardStructure((CSG) model, modelGen, r);
		default:
			throw new PrismNotSupportedException("Cannot build rewards for " + model.getModelType() + "s");
		}
	}

	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from a model generator. 
	 * @param mc The DTMC or CTMC
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public MCRewards buildMCRewardStructure(DTMC mc, ModelGenerator modelGen, int r) throws PrismException
	{
		if (modelGen == null) {
			throw new PrismException("No model generator to build reward structure");
		}
		
		if (modelGen.rewardStructHasTransitionRewards(r)) {
			// TODO
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}

		int numStates = mc.getNumStates();
		List<State> statesList = mc.getStatesList();
		StateRewardsArray rewSA = new StateRewardsArray(numStates);
		for (int j = 0; j < numStates; j++) {
			State state = statesList.get(j);
			// State rewards
			double rew = modelGen.getStateReward(r, state);
			if (Double.isNaN(rew))
				throw new PrismException("Reward structure evaluates to NaN at state " + state);
			if (!allowNegative && rew < 0)
				throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
			rewSA.addToStateReward(j, rew);
		}
		return rewSA;
	}

	/**
	 * Construct the rewards for an MDP from a model generator. 
	 * @param mdp The MDP
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public MDPRewards buildMDPRewardStructure(MDP mdp, ModelGenerator modelGen, int r) throws PrismException
	{
		int numStates = mdp.getNumStates();
		List<State> statesList = mdp.getStatesList();
		MDPRewardsSimple rewSimple = new MDPRewardsSimple(numStates);
		for (int j = 0; j < numStates; j++) {
			State state = statesList.get(j);
			// State rewards
			double rew = modelGen.getStateReward(r, state);
			if (Double.isNaN(rew))
				throw new PrismException("Reward structure evaluates to NaN at state " + state);
			if (!allowNegative && rew < 0)
				throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
			rewSimple.addToStateReward(j, rew);

			// State-action rewards
			if (mdp.isDeadlockState(j)) {
				// As state j is a deadlock state, any outgoing transition
				// was added to "fix" the deadlock and thus does not get a reward.
				// Skip to next state
				continue;
			}
			int numChoices = mdp.getNumChoices(j);
			for (int k = 0; k < numChoices; k++) {
				rew = modelGen.getStateActionReward(r, state, mdp.getAction(j, k));
				if (Double.isNaN(rew))
					throw new PrismException("Reward structure evaluates to NaN at state " + state);
				if (!allowNegative && rew < 0)
					throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
				rewSimple.addToTransitionReward(j, k, rew);
			}
		}
		return rewSimple;
	}

	/**
	 * Construct the rewards for an STPG from a model generator. 
	 * @param stpg The STPG
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public STPGRewards buildSTPGRewardStructure(STPG stpg, ModelGenerator modelGen, int r) throws PrismException
	{
		int numStates = stpg.getNumStates();
		List<State> statesList = stpg.getStatesList();
		STPGRewardsSimple rewSimple = new STPGRewardsSimple(numStates);
		for (int j = 0; j < numStates; j++) {
			State state = statesList.get(j);
			// State rewards
			double rew = modelGen.getStateReward(r, state);
			if (Double.isNaN(rew))
				throw new PrismException("Reward structure evaluates to NaN at state " + state);
			if (!allowNegative && rew < 0)
				throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
			rewSimple.addToStateReward(j, rew);
			// State-action rewards
			int numChoices = stpg.getNumChoices(j);
			for (int k = 0; k < numChoices; k++) {
				rew = modelGen.getStateActionReward(r, state, stpg.getAction(j, k));
				if (Double.isNaN(rew))
					throw new PrismException("Reward structure evaluates to NaN at state " + state);
				if (!allowNegative && rew < 0)
					throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
				rewSimple.addToTransitionReward(j, k, rew);
				int numChoices2 = stpg.getNumNestedChoices(j, k);
				for (int l = 0; l < numChoices2; l++) {
					rew = modelGen.getStateActionReward(r, state, stpg.getNestedAction(j, k, l));
					if (Double.isNaN(rew))
						throw new PrismException("Reward structure evaluates to NaN at state " + state);
					if (!allowNegative && rew < 0)
						throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
					rewSimple.addToNestedTransitionReward(j, k, l, rew);
				}
			}
		}
		return rewSimple;
	}

	/**
	 * Construct the rewards for an SMG from a model generator. 
	 * @param smg The SMG
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public SMGRewards buildSMGRewardStructure(SMG smg, ModelGenerator modelGen, int r) throws PrismException
	{
		int numStates = smg.getNumStates();
		List<State> statesList = smg.getStatesList();
		SMGRewardsSimple rewSimple = new SMGRewardsSimple(numStates);
		for (int j = 0; j < numStates; j++) {
			State state = statesList.get(j);
			// State rewards
			double rew = modelGen.getStateReward(r, state);
			if (Double.isNaN(rew))
				throw new PrismException("Reward structure evaluates to NaN at state " + state);
			if (!allowNegative && rew < 0)
				throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
			rewSimple.addToStateReward(j, rew);
			// State-action rewards
			int numChoices = smg.getNumChoices(j);
			for (int k = 0; k < numChoices; k++) {
				rew = modelGen.getStateActionReward(r, state, smg.getAction(j, k));
				if (Double.isNaN(rew))
					throw new PrismException("Reward structure evaluates to NaN at state " + state);
				if (!allowNegative && rew < 0)
					throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
				rewSimple.addToTransitionReward(j, k, rew);
			}
		}
		return rewSimple;
	}

	/**
	 * Construct the rewards for a CSG from a model generator. 
	 * @param csg The CSG
	 * @param modelGen The ModelGenerator defining the rewards
	 * @param r The index of the reward structure to build
	 */
	public CSGRewards buildCSGRewardStructure(CSG csg, ModelGenerator modelGen, int r) throws PrismException
	{
		int numStates = csg.getNumStates();
        int a;
		int numChoices;
		double rew;

		List<State> statesList = csg.getStatesList();
		State state;
		
		String csgAction, g1, g2;		
		Pattern pattern1 = Pattern.compile("(\\[\\w+(\\,\\w+)*\\])");
		Pattern pattern2 = Pattern.compile("\\w+");
		Matcher matcher1, matcher2;
		CSGRewardsSimple rewSimple = new CSGRewardsSimple(numStates);
		Set<String> csgActions = new HashSet<String>();
        String[] actions = new String[csg.getNumPlayers()];
		
		for (int j = 0; j < numStates; j++) {
			state = statesList.get(j);
			// State rewards
			rew = modelGen.getStateReward(r, state);
			if (Double.isNaN(rew))
				throw new PrismException("Reward structure evaluates to NaN at state " + state);
			//if (!allowNegative && rew < 0) {
				//throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
			rewSimple.addToStateReward(j, rew);
			// State-action rewards
			numChoices = csg.getNumChoices(j);
			
			for (int k = 0; k < numChoices; k++) {
				if(modelGen.getRewardStruct(r) instanceof SRewardStruct) {
					rew = 0;
					csgAction = (String) csg.getAction(j, k);
					csgActions.clear();
					matcher1 = pattern1.matcher(csgAction);
					while(matcher1.find()) {
						g1 = matcher1.group();
						matcher2 = pattern2.matcher(g1);
						while(matcher2.find()) {
							g2 = matcher2.group();
							csgActions.add(g2);
						}
					}
					rew += modelGen.getStateActionReward(r, state, csgActions);
					if (Double.isNaN(rew))
						throw new PrismException("Reward structure evaluates to NaN at state " + state);
					rewSimple.addToTransitionReward(j, k, rew);
				}
				else {
					a = 0;
					rew = 0;
					matcher2 = pattern2.matcher(csg.getAction(j, k).toString());
					while(matcher2.find()) {
						actions[a] = matcher2.group();
						rew += modelGen.getStateActionReward(r, state, actions[a]);
						a++;
					}	
					if (Double.isNaN(rew))
						throw new PrismException("Reward structure evaluates to NaN at state " + state);
					//if (!allowNegative && rew < 0) {
						//throw new PrismException("Reward structure evaluates to " + rew + " at state " + state +", negative rewards not allowed");
					rewSimple.addToTransitionReward(j, k, rew);
				}
			}
		}
		return rewSimple;
	}
	
	/**
	 * Construct the rewards for a Markov chain (DTMC or CTMC) from files exported explicitly by PRISM. 
	 * @param mc The DTMC or CTMC
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MCRewards buildMCRewardsFromPrismExplicit(DTMC mc, File rews, File rewt) throws PrismException
	{
		String s, ss[];
		int i, lineNum = 0;
		double reward;
		StateRewardsArray rewSA = new StateRewardsArray(mc.getNumStates());

		if (rews != null) {
			// Open state rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rews))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of state rewards file");
				}
				// Go though list of state rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						reward = Double.parseDouble(ss[1]);
						if (!allowNegative && reward < 0) {
							throw new PrismLangException("Found state reward " + reward + " at state " + i +", negative rewards not allowed");
						}
						rewSA.setStateReward(i, reward);
					}
					s = in.readLine();
					lineNum++;
				}
			} catch (IOException e) {
				throw new PrismException("Could not read state rewards from file \"" + rews + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in state rewards file (line " + lineNum + ") for MDP");
			}
		}

		if (rewt != null) {
			throw new PrismNotSupportedException("Explicit engine does not yet handle transition rewards for D/CTMCs");
		}

		return rewSA;
	}
	
	/**
	 * Construct the rewards for an MDP from files exported explicitly by PRISM.
	 * @param model The MDP
	 * @param rews The file containing state rewards (ignored if null)
	 * @param rewt The file containing transition rewards (ignored if null)
	 */
	public MDPRewards buildMDPRewardsFromPrismExplicit(MDP mdp, File rews, File rewt) throws PrismException
	{
		String s, ss[];
		int i, j, lineNum = 0;
		double reward;
		MDPRewardsSimple rs = new MDPRewardsSimple(mdp.getNumStates());

		if (rews != null) {
			// Open state rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rews))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of state rewards file");
				}
				// Go though list of state rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						reward = Double.parseDouble(ss[1]);
						if (!allowNegative && reward < 0) {
							throw new PrismLangException("Found state reward " + reward + " at state " + i +", negative rewards not allowed");
						}
						rs.setStateReward(i, reward);
					}
					s = in.readLine();
					lineNum++;
				}
			} catch (IOException e) {
				throw new PrismException("Could not read state rewards from file \"" + rews + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in state rewards file (line " + lineNum + ") for MDP");
			}
		}

		if (rewt != null) {
			// Open transition rewards file, automatic close
			try (BufferedReader in = new BufferedReader(new FileReader(rewt))) {
				// Ignore first line
				s = in.readLine();
				lineNum = 1;
				if (s == null) {
					throw new PrismException("Missing first line of transition rewards file");
				}
				// Go though list of transition rewards in file
				s = in.readLine();
				lineNum++;
				while (s != null) {
					s = s.trim();
					if (s.length() > 0) {
						ss = s.split(" ");
						i = Integer.parseInt(ss[0]);
						j = Integer.parseInt(ss[1]);
						reward = Double.parseDouble(ss[3]);
						if (!allowNegative && reward < 0) {
							throw new PrismLangException("Found transition reward " + reward + " at state " + i +", action " + j +", negative rewards not allowed");
						}
						rs.setTransitionReward(i, j, reward);
					}
					s = in.readLine();
					lineNum++;
				}

			} catch (IOException e) {
				throw new PrismException("Could not read transition rewards from file \"" + rewt + "\"" + e);
			} catch (NumberFormatException e) {
				throw new PrismException("Problem in transition rewards file (line " + lineNum + ") for MDP");
			}
		}

		return rs;
	}

}
