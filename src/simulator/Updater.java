//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package simulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import parser.State;
import parser.VarList;
import parser.ast.ASTElement;
import parser.ast.Command;
import parser.ast.ExpressionProb;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.ast.RewardStruct;
import parser.ast.Update;
import parser.ast.Updates;
import parser.type.Type;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

import parser.ast.SCommand;
import parser.ast.SModule;

import java.util.HashMap;

public class Updater extends PrismComponent
{
	// Settings:
	// Do we check that probabilities sum to 1?
	protected boolean doProbChecks = true;
	// The precision to which we check probabilities sum to 1
	protected double sumRoundOff = 1e-5;
	
	// Info on model being explored
	protected ModulesFile modulesFile;
	protected ModelType modelType;
	protected int numModules;
	protected VarList varList;
	// Synchronising action info
	protected Vector<String> synchs;
	protected int numSynchs;
	protected int synchModuleCounts[];
	// Model info/stats
	protected int numRewardStructs;

	// Temporary storage:

	// Element i,j of updateLists is a list of the updates from module i labelled with action j
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected List<List<List<Updates>>> updateLists;
	// Bit j of enabledSynchs is set iff action j is currently enabled
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected BitSet enabledSynchs;
	// Element j of enabledModules is a BitSet showing modules which enable action j
	// (where j=0 denotes independent, otherwise 1-indexed action label)
	protected BitSet enabledModules[];

	/*** ***/
	protected List<ChoiceListFlexi> choicesUpdates;
	protected List<ChoiceListFlexi> choicesUpdatesProd;
	protected BitSet[] playerChoicesIndex;
	protected BitSet enabledPlayers;
	protected List<String> varIdents;
	protected List<Type> varTypes;
	
	protected BitSet synchedActions; // a single synchronized action
	protected ArrayList<BitSet> synchedGroups; // all synchronized actions
	protected HashMap<BitSet,Updates> synchUpdateMap; // maps the indexes of synchronizes actions to their updates
	protected HashMap<Integer,BitSet> synchMap; // maps an id to the bitsets of product actions
	protected HashMap<Integer,Integer> synchRel; // 
	protected HashMap<Integer,Integer> synchSMap; //
	
	/*** ***/
	
	public Updater(ModulesFile modulesFile, VarList varList)
	{
		this(modulesFile, varList, null);
	}
	
	public Updater(ModulesFile modulesFile, VarList varList, PrismComponent parent)
	{
		// Store some settings
		doProbChecks = parent.getSettings().getBoolean(PrismSettings.PRISM_DO_PROB_CHECKS);
		sumRoundOff = parent.getSettings().getDouble(PrismSettings.PRISM_SUM_ROUND_OFF);
		
		// Get info from model
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		numModules = modulesFile.getNumModules();
		synchs = modulesFile.getSynchs();
		numSynchs = synchs.size();
		numRewardStructs = modulesFile.getNumRewardStructs();
		this.varList = varList;
		
		// Compute count of number of modules using each synch action
		// First, compute and cache the synch actions for each of the modules
		List<HashSet<String>> synchsPerModule = new ArrayList<HashSet<String>>(numModules);
		for (int i = 0; i < numModules; i++) {
			synchsPerModule.add(new HashSet<String>(modulesFile.getModule(i).getAllSynchs()));
		}
		// Second, do the counting
		synchModuleCounts = new int[numSynchs];
		for (int j = 0; j < numSynchs; j++) {
			synchModuleCounts[j] = 0;
			String s = synchs.get(j);
			for (int i = 0; i < numModules; i++) {
				if (synchsPerModule.get(i).contains(s) && !(modulesFile.getModule(i) instanceof SModule))
					synchModuleCounts[j]++;
			}
		}

		// Build lists/bitsets for later use
		updateLists = new ArrayList<List<List<Updates>>>(numModules);
		for (int i = 0; i < numModules; i++) {
			updateLists.add(new ArrayList<List<Updates>>(numSynchs + 1));
			for (int j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).add(new ArrayList<Updates>());
			}
		}
		enabledSynchs = new BitSet(numSynchs + 1);
		enabledModules = new BitSet[numSynchs + 1];
		for (int j = 0; j < numSynchs + 1; j++) {
			enabledModules[j] = new BitSet(numModules);
		}
		
		/*** ***/
		synchedActions = new BitSet();
		synchedGroups = new ArrayList<BitSet>();
		synchUpdateMap = new HashMap<BitSet,Updates>();	
		synchMap = new HashMap<Integer,BitSet>();
		synchSMap = new HashMap<Integer,Integer>();
		synchRel = new HashMap<Integer,Integer>();
		/*** ***/
				
	}

	/**
	 * Set the precision to which we check that probabilities sum to 1.
	 */
	public void setSumRoundOff(double sumRoundOff)
	{
		this.sumRoundOff = sumRoundOff;
	}

	/**
	 * Get the precision to which we check that probabilities sum to 1.
	 */
	public double getSumRoundOff()
	{
		return sumRoundOff;
	}
	
	/**
	 * Determine the set of outgoing transitions from state 'state' and store in 'transitionList'.
	 * @param state State from which to explore
	 * @param transitionList TransitionList object in which to store result
	 */
	public void calculateTransitions(State state, TransitionList transitionList) throws PrismException
	{
		List<ChoiceListFlexi> chs;
		int i, j, k, l, n, count;
		
		// Clear lists/bitsets
		transitionList.clear();
		for (i = 0; i < numModules; i++) {
			for (j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).get(j).clear();
			}
		}
		enabledSynchs.clear();
		for (i = 0; i < numSynchs + 1; i++) {
			enabledModules[i].clear();
		}

		// Calculate the available updates for each module/action
		// (update information in updateLists, enabledSynchs and enabledModules)
		for (i = 0; i < numModules; i++) {
			calculateUpdatesForModule(i, state);
		}

		// Add independent transitions for each (enabled) module to list
		for (i = enabledModules[0].nextSetBit(0); i >= 0; i = enabledModules[0].nextSetBit(i + 1)) {
			for (Updates ups : updateLists.get(i).get(0)) {
				ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(-(i + 1), ups, state);
				if (ch.size() > 0)
					transitionList.add(ch);
			}
		}
		
		// Add synchronous transitions to list
		chs = new ArrayList<ChoiceListFlexi>();
		for (i = enabledSynchs.nextSetBit(1); i >= 0; i = enabledSynchs.nextSetBit(i + 1)) {
			chs.clear();
			// Check counts to see if this action is blocked by some module
			if (enabledModules[i].cardinality() < synchModuleCounts[i - 1])
				continue;
			// If not, proceed...
			for (j = enabledModules[i].nextSetBit(0); j >= 0; j = enabledModules[i].nextSetBit(j + 1)) {
				count = updateLists.get(j).get(i).size();
				// Case where there is only 1 Updates for this module
				if (count == 1) {
					Updates ups = updateLists.get(j).get(i).get(0);
					// Case where this is the first Choice created
					if (chs.size() == 0) {
						ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
						if (ch.size() > 0) {
							chs.add(ch);
						}
					}
					// Case where there are existing Choices
					else {
						// Product with all existing choices
						for (ChoiceListFlexi ch : chs) {
							processUpdatesAndAddToProduct(ups, state, ch);
						}
					}
				}
				// Case where there are multiple Updates (i.e. local nondeterminism)
				else {
					// Case where there are no existing choices
					if (chs.size() == 0) {
						for (Updates ups : updateLists.get(j).get(i)) {
							ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
							if (ch.size() > 0) {
								chs.add(ch);
							}
						}
					}
					// Case where there are existing Choices
					else {
						// Duplicate (count-1 copies of) current Choice list
						n = chs.size();
						for (k = 0; k < count - 1; k++)
							for (l = 0; l < n; l++) {
								chs.add(new ChoiceListFlexi(chs.get(l)));
							}
						// Products with existing choices
						for (k = 0; k < count; k++) {
							Updates ups = updateLists.get(j).get(i).get(k);
							for (l = 0; l < n; l++) {
								processUpdatesAndAddToProduct(ups, state, chs.get(k * n + l));
							}
						}
					}
				}
			}
			// Add all new choices to transition list
			for (ChoiceListFlexi ch : chs) {
				transitionList.add(ch);
			}
		}
		
		// For a DTMC, we need to normalise across all transitions
		// This is partly to handle "local nondeterminism"
		// and also to handle any dubious trickery done by disabling probability checks
		if (modelType == ModelType.DTMC) {
			double probSum = transitionList.getProbabilitySum();
			transitionList.scaleProbabilitiesBy(1.0 / probSum);
		}
	
		// Check validity of the computed transitions
		// (not needed currently)
		//transitionList.checkValid(modelType);
		
		// Check for errors (e.g. overflows) in the computed transitions
		//transitionList.checkForErrors(state, varList);
		
		//System.out.println(transitionList);
	}
	
	public void calculateTransitionsCSG(State state, TransitionList transitionList, Map<Integer, BitSet> mp) throws PrismException
	{
		//System.out.println("\ncalculateTransitionsCSG");
		//System.out.println("\n###################################### state " + state.toString());
		List<ChoiceListFlexi> chs;
		int i, j, k, l, n, count;
		/*** ***/
		boolean synch = false;
		synchSMap.clear();
		/*** ***/
		// CSG
		enabledPlayers = new BitSet();
		int choice_counter = 0;
		playerChoicesIndex = new BitSet[modulesFile.getNumPlayers()];
		choicesUpdates = new ArrayList<ChoiceListFlexi>();
		choicesUpdatesProd = new ArrayList<ChoiceListFlexi>();
		varIdents = new ArrayList<String>();
		varTypes = new ArrayList<Type>();
		for(i = 0; i < modulesFile.getNumPlayers(); i++) {
			playerChoicesIndex[i] = new BitSet();
		}
		//System.out.println("-- vars " + modulesFile.getModule(0).getDeclaration(0));
		for(i = 0; i < modulesFile.getNumModules(); i++) {
			for(j = 0; j < modulesFile.getModule(i).getNumDeclarations(); j++) {
				varIdents.add(modulesFile.getModule(i).getDeclaration(j).getName());
				varTypes.add(modulesFile.getModule(i).getDeclaration(j).getType());
			}
		}
		
		// Clear lists/bitsets
		transitionList.clear();
		for (i = 0; i < numModules; i++) {
			for (j = 0; j < numSynchs + 1; j++) {
				updateLists.get(i).get(j).clear();
			}
		}
		enabledSynchs.clear();
		for (i = 0; i < numSynchs + 1; i++) {
			enabledModules[i].clear();
		}

		// Calculate the available updates for each module/action
		// (update information in updateLists, enabledSynchs and enabledModules)
		for (i = 0; i < numModules; i++) {
			//System.out.println("\n-- Module " + i);
			calculateUpdatesForModule(i, state);
		}

		// Add independent transitions for each (enabled) module to list
		for (i = enabledModules[0].nextSetBit(0); i >= 0; i = enabledModules[0].nextSetBit(i + 1)) {
			for (Updates ups : updateLists.get(i).get(0)) {
				ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(-(i + 1), ups, state);
				if (ch.size() > 0)
					transitionList.add(ch);
			}
		}
		
		// Add synchronous transitions to list
		chs = new ArrayList<ChoiceListFlexi>();
		for (i = enabledSynchs.nextSetBit(1); i >= 0; i = enabledSynchs.nextSetBit(i + 1)) {
			//System.out.println("** enabled synch " + i);
			chs.clear();
			// Check counts to see if this action is blocked by some module
			//System.out.println(enabledModules[i].cardinality());
			//System.out.println(synchModuleCounts[i-1]);
			if (enabledModules[i].cardinality() < synchModuleCounts[i - 1]) {
				//System.out.println("blocked");
				continue;
			}
			// If not, proceed...
			for (j = enabledModules[i].nextSetBit(0); j >= 0; j = enabledModules[i].nextSetBit(j + 1)) {
				//System.out.println("££ j " + j);
				//System.out.println(updateLists.get(j).get(i).get(0));
				count = updateLists.get(j).get(i).size();
				// Case where there is only 1 Updates for this module
				if (count == 1) {
					Updates ups = updateLists.get(j).get(i).get(0);
					// Case where this is the first Choice created
					if (chs.size() == 0) {
						ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
						if (ch.size() > 0) {
							chs.add(ch);
						}
					}
					// Case where there are existing Choices
					else {
						// Product with all existing choices
						for (ChoiceListFlexi ch : chs) {
							processUpdatesAndAddToProduct(ups, state, ch);
						}
					}
				}
				// Case where there are multiple Updates (i.e. local nondeterminism)
				else {
					// Case where there are no existing choices
					if (chs.size() == 0) {
						for (Updates ups : updateLists.get(j).get(i)) {
							ChoiceListFlexi ch = processUpdatesAndCreateNewChoice(i, ups, state);
							if (ch.size() > 0) {
								chs.add(ch);
							}
						}
					}
					// Case where there are existing Choices
					else {
						// Duplicate (count-1 copies of) current Choice list
						n = chs.size();
						for (k = 0; k < count - 1; k++)
							//System.out.println(i + " " + j + " " + k);
							for (l = 0; l < n; l++) {
								chs.add(new ChoiceListFlexi(chs.get(l)));
							}
						// Products with existing choices
						for (k = 0; k < count; k++) {
							Updates ups = updateLists.get(j).get(i).get(k);
							for (l = 0; l < n; l++) {
								processUpdatesAndAddToProduct(ups, state, chs.get(k * n + l));
							}
						}
					}
				}
			}
			// Add all new choices to transition list
			//System.out.println("-- choicelistflexi ");
			for (ChoiceListFlexi ch : chs) {
				//System.out.println("## updates " + choicesUpdates.get(choice_counter));
				//System.out.println("[" + modulesFile.getSynch(i - 1) + "]");
				playerChoicesIndex[modulesFile.getPlayerForAction("[" + modulesFile.getSynch(i - 1) + "]")].set(choice_counter);
				enabledPlayers.set(modulesFile.getPlayerForAction("[" + modulesFile.getSynch(i - 1) + "]"));
				/*** ***/
				//System.out.println("## choicesUpdates " + i);
				//System.out.println(ch);
				synchSMap.put(i, choicesUpdates.size());
				/*** ***/
				choicesUpdates.add(ch);
				choice_counter++;
				transitionList.add(ch);
			}
		}		
		/*** ***/
		//System.out.println("%% synchSMap " + synchSMap);		
		synch = synchedActions.isEmpty();
		/*
		System.out.println("\n-- choicesUpdates");
		for(i = 0; i < choicesUpdates.size(); i++) {
			System.out.println(choicesUpdates.get(i));
		}		
		*/
		/*
		System.out.println("\n-- playerChoicesIndex");
		for(i = 0; i < playerChoicesIndex.length; i++) {
			System.out.println(playerChoicesIndex[i]);
		}
		*/
		/*
		System.out.println("\n-- transitionList");
		for(i = 0; i < transitionList.getNumChoices(); i++) {
			System.out.println("choice " + i + " " + transitionList.getChoice(i));
		}
		System.out.println();
		*/	
		BitSet prodIDs;
		String s = "";
		/*
		System.out.println("\n-- enabledPlayers");
		*/
		//System.out.println(enabledPlayers);
		//System.out.println(enabledPlayers.cardinality());
		
		BitSet idlePLayers = new BitSet();
		
		if(!(enabledPlayers.cardinality() == 0)) { //deadlocks could be fixed here
			for(j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(0); j >= 0; j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(j + 1)) {	
				prodIDs = new BitSet();
				prodIDs.set(transitionList.getChoice(j).getModuleOrActionIndex());
				choicesUpdates.get(j).setProductID(transitionList.getChoice(j).getModuleOrAction());
				choicesUpdates.get(j).setProduct(prodIDs);
				if(enabledPlayers.cardinality() > 1) {
					for(l = 0; l < modulesFile.getNumPlayers(); l++) {
						if(!enabledPlayers.get(l)) {
							if(!idlePLayers.get(l)) {
								//System.out.println("%% Player " + l + " not enabled ");
								s += "[i" + l + "]";
								idlePLayers.set(l);
							}
						}
					}
					if(!synch) {
						preChoiceProduct(prodIDs, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)+1));
					}
					else {
						choicesUpdates.get(j).setProductID(choicesUpdates.get(j).getProductID().concat(s));
						playerChoiceProduct(mp, choicesUpdates.get(j), state, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)+1));
					}
				}
				else {
						playerIdleProduct(choicesUpdates.get(j), state, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)));
				}
			}
			//System.out.println("********* " + s);
			if(!synch)
				preGroupProduct(state, s);
		}
		/*
		if(!(enabledPlayers.cardinality() == 0)) { //deadlocks could be fixed here
			for(j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(0); j >= 0; j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(j + 1)) {	
				//System.out.println("-- player 0, choice " + j);
				//System.out.println(transitionList.getChoice(j));	
				prodIDs = new BitSet();
				prodIDs.set(transitionList.getChoice(j).getModuleOrActionIndex());
				choicesUpdates.get(j).setProductID(transitionList.getChoice(j).getModuleOrAction());
				choicesUpdates.get(j).setProduct(prodIDs);
				if(enabledPlayers.cardinality() > 1) {
					for(l = 0; l < modulesFile.getNumPlayers(); l++) {
						if(!enabledPlayers.get(l)) {
							//System.out.println("Player " + l + " not enabled ");
							choicesUpdates.get(j).setProductID(choicesUpdates.get(j).getProductID().concat("[i" + l + "]"));
						}
					}
					playerChoiceProduct(mp, choicesUpdates.get(j), state, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)+1));
				}
				else {
					//System.out.println(choicesUpdates.get(j).getProductID());
					playerIdleProduct(mp, choicesUpdates.get(j), state, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)));
				}
			//System.out.println();
			}
		}		
		if(!synchedActions.isEmpty()) {
			BitSet preProdIds;
			if(!(enabledPlayers.cardinality() == 0)) { //deadlocks could be fixed here
				for(j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(0); j >= 0; j = playerChoicesIndex[enabledPlayers.nextSetBit(0)].nextSetBit(j + 1)) {
					preProdIds = new BitSet();
					preProdIds.set(transitionList.getChoice(j).getModuleOrActionIndex());
					if(enabledPlayers.cardinality() > 1) {
						preChoiceProduct(preProdIds, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)+1));
					}
					else {
						playerIdleProduct(choicesUpdates.get(j), state, enabledPlayers.nextSetBit(enabledPlayers.nextSetBit(0)));
					}
				}
				for(l = 0; l < modulesFile.getNumPlayers(); l++) {
					if(!enabledPlayers.get(l)) {
						System.out.println("%% Player " + l + " not enabled ");
						s += "[i" + l + "]";
					}
				}
			}		
			System.out.println("********* " + s);
			preGroupProduct(state, s);
		}
		*/
		/*
		System.out.println("-- choicesUpdates");
		for(i = 0; i < choicesUpdates.size(); i++) {
			System.out.println(choicesUpdates.get(i));
		}
		
		System.out.println("\n-- choicesUpdatesProd");
		for(i = 0; i < choicesUpdatesProd.size(); i++) {
			System.out.println(choicesUpdatesProd.get(i));
		}
		*/
		if(modelType == ModelType.CSG) {
			//System.out.println("-- csg");
			transitionList.clear();
			for(ChoiceListFlexi ch : choicesUpdatesProd) {
				//System.out.println("-- final transitions");
				//System.out.println(ch);
				transitionList.add(ch);
			}
			/*
			for(i = 0; i < transitionList.getNumTransitions(); i++) {
				System.out.println(transitionList.getTransitionModuleOrAction(i));
			}
			*/
		}
		/*** ***/
		
		// For a DTMC, we need to normalise across all transitions
		// This is partly to handle "local nondeterminism"
		// and also to handle any dubious trickery done by disabling probability checks
		if (modelType == ModelType.DTMC) {
			double probSum = transitionList.getProbabilitySum();
			transitionList.scaleProbabilitiesBy(1.0 / probSum);
		}
	
		// Check validity of the computed transitions
		// (not needed currently)
		//transitionList.checkValid(modelType);
		
		// Check for errors (e.g. overflows) in the computed transitions
		//transitionList.checkForErrors(state, varList);
		
		//System.out.println(transitionList);
	}
	
	/*** ***/	
	public boolean containsVar(List<Updates> l, Updates u) 
	{
		boolean r = false;
		for(Updates i : l) {
			for(Update j : i.getUpdates()) {
				for(Update k : u.getUpdates()) {
					for(int m = 0; m < j.getNumElements(); m++) {
						for(int n = 0; n < k.getNumElements(); n++) {
							r = r || j.getVarIndex(m) ==  k.getVarIndex(n);
						}
					}
				}
			}
		}
		return r;
	}
	
	public void playerIdleProduct(Map<Integer, BitSet> mp, ChoiceListFlexi clf, State st, int p) 
	{
		//System.out.println("-- idle product");
		String synchId = "";
		for(int i = 0; i < modulesFile.getNumPlayers(); i++) {
			if(i != p) {
				synchId = synchId.concat("[i" + i + "]");
			}	
			else {
				synchId = synchId.concat(clf.getProductID());
			}
		}
		clf.setProductID(synchId);
		modulesFile.getSynchs().addElement(clf.getProductID());
		clf.setModuleOrActionIndex(modulesFile.getSynchs().size());
		mp.put(modulesFile.getSynchs().size(), clf.getProduct());
		//System.out.println(clf);
		choicesUpdatesProd.add(clf);		
	} 
	
	public void playerIdleProduct(ChoiceListFlexi clf, State st, int p) 
	{
		//System.out.println("-- idle product");
		String synchId = "";
		for(int i = 0; i < modulesFile.getNumPlayers(); i++) {
			if(i != p) {
				synchId = synchId.concat("[i" + i + "]");
			}	
			else {
				synchId = synchId.concat(clf.getProductID());
			}
		}
		clf.setProductID(synchId);
		modulesFile.getSynchs().addElement(clf.getProductID());
		clf.setModuleOrActionIndex(modulesFile.getSynchs().size());
		//System.out.println(clf);
		choicesUpdatesProd.add(clf);		
	} 
	
	public void preChoiceProduct(BitSet prod, int p) throws PrismException 
	{ 
		//System.out.println("-- preChoiceProduct");
		//System.out.println(prod);
		BitSet t, players;
		int idx;
		for(int i = playerChoicesIndex[p].nextSetBit(0); i >= 0; i = playerChoicesIndex[p].nextSetBit(i + 1)) {
			//System.out.println("i " + i);
			t = new BitSet();
			t = (BitSet) prod.clone();
			//System.out.println("setting  " + choicesUpdates.get(i).getModuleOrActionIndex());
			//System.out.println(choicesUpdates.get(i));
			t.set(choicesUpdates.get(i).getModuleOrActionIndex());
			if(enabledPlayers.nextSetBit(p + 1) != -1) {
				preChoiceProduct(t, enabledPlayers.nextSetBit(p + 1)); 
			}
			else {
				players = new BitSet();
				for(int s = t.nextSetBit(0); s < t.length() && s !=-1; s = t.nextSetBit(s+1)) {
					if(!players.get(modulesFile.getPlayerForAction(modulesFile.getSynch(s-1)))) {
						players.set(modulesFile.getPlayerForAction(modulesFile.getSynch(s-1)));
					}
					else {
						throw new PrismException("Trying to synchronize different actions from the same player");
					}
				}
				idx = synchMap.keySet().size();
				synchMap.put(idx++, t);
			}
		}
	}
	
	public void preGroupProduct(State st, String preID) throws PrismException 
	{
		//System.out.println("-- preGroupProduct");
		//System.out.println("-- synchMap " + synchMap);
		BitSet and = new BitSet();
		BitSet curr = new BitSet();
		int card;
		for(Integer k : synchMap.keySet()) {
			card = 0;
			//System.out.println("# " + synchMap.get(k));
			for(int i = 0; i < synchedGroups.size(); i++) {
				curr = (BitSet) synchMap.get(k).clone();
				curr.andNot(synchedGroups.get(i));
				and = (BitSet) synchMap.get(k).clone();
				and.and(synchedGroups.get(i));
				if(and.cardinality() > card) {
					card = and.cardinality();
					synchRel.put(k, i);
				}
				else if(card == and.cardinality() && card >= 2) {
					//System.out.println("#### and cardinality ####");
				}
				if(and.cardinality() >= 2) {
					//System.out.println("- " + synchedGroups.get(i));
					//System.out.println("- " + curr);
				}
			}
		}		
		//System.out.println("-- choicesUpdates");
		//System.out.println(choicesUpdates);
		
		//System.out.println("- synchRel");
		//System.out.println(synchRel);
		curr.clear();
		and.clear();
		synchProduct(st, preID);
	}
	
	public void synchProduct(State st, String preID) throws PrismException 
	{
		//System.out.println("-- synchProduct");
		//System.out.println("-- preID " + preID);
		BitSet group, indprod;
		ChoiceListFlexi ch;
		ChoiceListFlexi clf = null;
		Updates l;
		String s;
		int id = 0;
		for(Integer t : synchMap.keySet()) {
			//System.out.println("*");
			//System.out.println("-- synchs " + synchMap.get(t));
			BitSet players = new BitSet();
			group = synchedGroups.get(synchRel.get(t));
			//System.out.println("-- group " + group);
			
			for(int x = group.nextSetBit(0); x < group.length() && x !=-1; x = group.nextSetBit(x+1)) {	
				if(!players.get(modulesFile.getPlayerForAction(modulesFile.getSynch(x-1)))) {
					players.set(modulesFile.getPlayerForAction(modulesFile.getSynch(x-1)));
				}
				else {
					throw new PrismException("Trying to synchronize different actions from the same player");
				}			
			}	
			
			indprod = (BitSet) synchMap.get(t).clone();
			indprod.andNot(group);
			//System.out.println("-- indprod " + indprod);
			
			s = preID;
			s += "[";
			for(int u = group.nextSetBit(0); u < group.size() && u!=-1; u = group.nextSetBit(u+1)) {
				//System.out.println(synchs.get(u-1));
				s += synchs.get(u-1);
				if(group.nextSetBit(u+1) != -1)
					s += ",";
			}
			s += "]";
			if(indprod.cardinality() == 0) {
				//create transition out of the updates of the synchronous one
				l = synchUpdateMap.get(group);
				clf = processUpdatesAndCreateNewChoice(id, l, st); // not too sure about the id (first param)
				clf.setProductID(s);
				id++;
			}
			else {
				int v = indprod.nextSetBit(0);
				//System.out.println(players);
				//System.out.println(modulesFile.getSynch(v-1));
				//System.out.println(modulesFile.getPlayerForAction(modulesFile.getSynch(v-1)));
				
				if(!players.get(modulesFile.getPlayerForAction(modulesFile.getSynch(v-1)))) {
					players.set(modulesFile.getPlayerForAction(modulesFile.getSynch(v-1)));
				}
				else {
					throw new PrismException("Trying to synchronize different actions from the same player");
				}
				
				//clf = new ChoiceListFlexi(choicesUpdates.get(v-1)); // problem here
				clf = new ChoiceListFlexi(choicesUpdates.get(synchSMap.get(v))); // problem here
				clf.setProductID(s);
				//clf.setProductID(clf.getProductID().concat(choicesUpdates.get(v-1).getModuleOrAction()));
				clf.setProductID(clf.getProductID().concat(choicesUpdates.get(synchSMap.get(v)).getModuleOrAction()));
				
				//System.out.println("-- clf productID " + clf.getProductID());
				for(int u = indprod.nextSetBit(v+1); u < indprod.size() && u >= 0; u = indprod.nextSetBit(u+1)) {
					
					if(!players.get(modulesFile.getPlayerForAction(modulesFile.getSynch(u-1)))) {
						players.set(modulesFile.getPlayerForAction(modulesFile.getSynch(u-1)));
					}
					else {
						throw new PrismException("Trying to synchronize different actions from the same player.");
					}
					
					//System.out.println("-- synch " + u);
					//System.out.println("-- choiceUpdates " + choicesUpdates);
					for(List<Update> k : choicesUpdates.get(synchSMap.get(u)).updates) {		
					//for(List<Update> k : choicesUpdates.get(u-1).updates) {		
						for(Update up : k) {
							processUpdatesAndAddToProduct(up.getParent(), st, clf);	
						}
					}
					clf.setProductID(clf.getProductID().concat(choicesUpdates.get(synchSMap.get(u)).getModuleOrAction()));
					//clf.setProductID(clf.getProductID().concat(choicesUpdates.get(u-1).getModuleOrAction()));
				}
				l = synchUpdateMap.get(group);
				//System.out.println("--update " + l.getParent());
				processUpdatesAndAddToProduct(l, st, clf);	
			}
			//System.out.println(clf.getProductID());
			//System.out.println("-- final transition " + clf);
			
			modulesFile.getSynchs().addElement(clf.getProductID());
			clf.setModuleOrActionIndex(modulesFile.getSynchs().size());
			choicesUpdatesProd.add(clf);
			
		}	
		synchMap.clear();
		synchRel.clear();
	}
	
	public void playerChoiceProduct(Map<Integer, BitSet> mp, ChoiceListFlexi clf, State st, int p) throws PrismException 
	{
		ChoiceListFlexi chlf;
		//System.out.println(p);
		//System.out.println(playerChoicesIndex[p]);
		
		for(int i = playerChoicesIndex[p].nextSetBit(0); i >= 0; i = playerChoicesIndex[p].nextSetBit(i + 1)) {
			//System.out.println("\n --- player " + p + ", choice " + i);
			if(enabledPlayers.nextSetBit(p + 1) != -1) {
				//System.out.println(" ---- not last player");
				chlf = playerChoiceProdcut(mp, clf, st, i, false);
				//System.out.println(" ---- product player " + p + " " + 1);
				playerChoiceProduct(mp, chlf, st, enabledPlayers.nextSetBit(p + 1)); 
			}
			else {
				//System.out.println(" ---- last player");
				playerChoiceProdcut(mp, clf, st, i, true);
			}
		}
	} 
	
	public ChoiceListFlexi playerChoiceProdcut(Map<Integer, BitSet> mp, ChoiceListFlexi clf, State st, int i, boolean lt) throws PrismException 
	{
		//System.out.println("- updates " + i);
		List<Updates> lup = new ArrayList<Updates>();
		for(List<Update> k : clf.updates) {
			for(Update l : k) {
				lup.add((Updates) l.getParent().findAllVars(varIdents, varTypes, false));
			}
		}
		for(List<Update> k : choicesUpdates.get(i).updates) {		
			//System.out.println("k " + k);
			for(Update l : k) {
				if(containsVar(lup, (Updates) l.getParent().findAllVars(varIdents, varTypes))) {
					throw(new PrismException("Concurrent update of the same variable"));
				}
			}
		}
		clf = new ChoiceListFlexi(clf);
		for(List<Update> k : choicesUpdates.get(i).updates) {		
			// the product is actually done here, can it be done after the group identification?
			for(Update l : k) {
				//System.out.println("% product " + choicesUpdates.get(i).updates.get(0).get(0).getParent());
				//System.out.println("% n " + l.getParent().getNumUpdates());
				//System.out.println("% update " + l);
				//if(lt && !clf.getProduct().get(choicesUpdates.get(i).getModuleOrActionIndex())) {
				if(!clf.getProduct().get(choicesUpdates.get(i).getModuleOrActionIndex())) {
					processUpdatesAndAddToProduct(l.getParent(), st, clf);	
				}
				//else if(!clf.getProduct().get(choicesUpdates.get(i).getModuleOrActionIndex())) {
				//	processUpdatesAndAddToProduct(l.getParent(), st, clf);	
				//}
			}
			if(lt && !clf.getProduct().get(choicesUpdates.get(i).getModuleOrActionIndex())) {
				clf.setProductID(clf.getProductID().concat(choicesUpdates.get(i).getModuleOrAction()));
				modulesFile.getSynchs().addElement(clf.getProductID());
				//System.out.println("-- clf index " + modulesFile.getSynchs().size());
				clf.setModuleOrActionIndex(modulesFile.getSynchs().size());
				clf.addSynchToProduct(choicesUpdates.get(i).getModuleOrActionIndex());
				mp.put(modulesFile.getSynchs().size(), clf.getProduct());
				choicesUpdatesProd.add(clf);
				//System.out.println("-- product " + clf.getProductID() + " " + clf.getProduct());
				//System.out.println("-- product updates " + clf);
			}
			else if (!clf.getProduct().get(choicesUpdates.get(i).getModuleOrActionIndex())) {
				clf.addSynchToProduct(choicesUpdates.get(i).getModuleOrActionIndex());
				clf.setProductID(clf.getProductID().concat(choicesUpdates.get(i).getModuleOrAction()));
				//System.out.println("-- product " + clf.getProductID() + " " + clf.getProduct());
				//System.out.println("-- product updates " + clf);
			}
		}
		return clf;
	}
	/*** ***/
	
	/**
	 * Calculate the state rewards for a given state.
	 * @param state The state to compute rewards for
	 * @param store An array in which to store the rewards
	 */
	public void calculateStateRewards(State state, double[] store) throws PrismLangException
	{
		int i, j, n;
		double d;
		RewardStruct rw;
		for (i = 0; i < numRewardStructs; i++) {
			rw = modulesFile.getRewardStruct(i);
			n = rw.getNumItems();
			d = 0.0;
			for (j = 0; j < n; j++) {
				if (!rw.getRewardStructItem(j).isTransitionReward())
					if (rw.getStates(j).evaluateBoolean(state))
						d += rw.getReward(j).evaluateDouble(state);
			}
			store[i] = d;
		}
	}

	/**
	 * Calculate the transition rewards for a given state and outgoing choice.
	 * @param state The state to compute rewards for
	 * @param ch The choice from the state to compute rewards for
	 * @param store An array in which to store the rewards
	 */
	public void calculateTransitionRewards(State state, Choice ch, double[] store) throws PrismLangException
	{
		int i, j, n;
		double d;
		RewardStruct rw;
		for (i = 0; i < numRewardStructs; i++) {
			rw = modulesFile.getRewardStruct(i);
			n = rw.getNumItems();
			d = 0.0;
			for (j = 0; j < n; j++) {
				if (rw.getRewardStructItem(j).isTransitionReward())
					if (rw.getRewardStructItem(j).getSynchIndex() == Math.max(0, ch.getModuleOrActionIndex()))
						if (rw.getStates(j).evaluateBoolean(state))
							d += rw.getReward(j).evaluateDouble(state);
			}
			store[i] = d;
		}
	}
	
	// Private helpers
	
	/**
	 * Determine the enabled updates for the 'm'th module from (global) state 'state'.
	 * Update information in updateLists, enabledSynchs and enabledModules.
	 * @param m The module index
	 * @param state State from which to explore
	 */
	protected void calculateUpdatesForModule(int m, State state) throws PrismLangException
	{
		Module module;
		Command command;
		String a;
		BitSet g;
		BitSet h;
		int i, j, k, l, n;
		
		module = modulesFile.getModule(m);
		
		//System.out.println("\n-- State " + state);
		
		//System.out.println("-- Module " + module);
		//System.out.println("-- AllSynchs " + module.getAllSynchs());
		
		if(!(module instanceof SModule)) {
			n = module.getNumCommands();
			for (i = 0; i < n; i++) {
				command = module.getCommand(i);
				if (command.getGuard().evaluateBoolean(state)) {
					j = command.getSynchIndex();
					updateLists.get(m).get(j).add(command.getUpdates());
					enabledSynchs.set(j);
					enabledModules[j].set(m);
				}
			}
		}
		else {
			g = new BitSet();
			//System.out.println("\n" + module.toString());
			n = module.getNumCommands();
			synchedActions.clear();
			synchedGroups.clear();
			synchUpdateMap.clear();
			j = 1;
			for (i = 0; i < n; i++) {
				g.clear();
				command = module.getCommand(i);				
				if (command.getGuard().evaluateBoolean(state)) {
					//System.out.println(modulesFile.getSynchs());
					//System.out.println(command.getSynchIndex()); // have to fix this
					//System.out.println(command); // have to fix this					
					
					/*** PROBABBLY SHOULD BE CHANGED TO THIS WITH OTHER MODIFICATIONS ***/
					/*** THE CURRENT PRODUCT ALSO SERVES TO GET THE ACTIONS FROM EACH PLAYER ***/
					/*
					updateLists.get(m).get(j).add(command.getUpdates());
					enabledModules[j].set(m);
					enabledSynchs.set(j);
					
					for(l = 0; l < ((SCommand) command).getSynchs().size(); l++) {
						a = ((SCommand) command).getSynchs().get(l);
						k = modulesFile.getSynchs().indexOf(a)+1;
						synchedActions.set(k);
						g.set(k);
					}
					*/
					/*** ***/
					
					for(l = 0; l < ((SCommand) command).getSynchs().size(); l++) {
						a = ((SCommand) command).getSynchs().get(l);
						
						// review this
						j = modulesFile.getSynchs().indexOf(a)+1;
						
						updateLists.get(m).get(j).add(command.getUpdates());
						
						//for(int p = 0; p < modulesFile.getNumModules(); p++) {
						//	if(m != p && !(modulesFile.getModule(p) instanceof SModule)) {
						//		for(int q = 0; q < modulesFile.getModule(p).getNumCommands(); q++) {
						//			if(modulesFile.getModule(p).getCommand(q).getSynchIndex() == j) {
						//				if(modulesFile.getModule(p).getCommand(q).getGuard().evaluateBoolean(state)) {
						//					System.out.println("############ j " + j);
						//					updateLists.get(m).get(j).add(modulesFile.getModule(p).getCommand(q).getUpdates());
						//					System.out.println(updateLists.get(m).get(j));
						//				}
						//			}
						//		}
						//	}
						//}
						
						enabledSynchs.set(j);
						enabledModules[j].set(m);
						synchedActions.set(j);
						g.set(j);
					}
					
					h = (BitSet) g.clone();
					synchedGroups.add(h);
					synchUpdateMap.put(h, command.getUpdates());
					j++;
				}
			}
			//System.out.println("-- synchedActions " + synchedActions);
			//System.out.println("-- synchedGroups " + synchedGroups);
			//System.out.println("-- synchUpdateMap " + synchUpdateMap);
		}
	}

	/**
	 * Create a new Choice object (currently ChoiceListFlexi) based on an Updates object
	 * and a (global) state. Check for negative probabilities/rates and, if appropriate,
	 * check probabilities sum to 1 too.
	 * @param moduleOrActionIndex Module/action for the choice, encoded as an integer (see Choice)
	 * @param ups The Updates object 
	 * @param state Global state
	 */
	private ChoiceListFlexi processUpdatesAndCreateNewChoice(int moduleOrActionIndex, Updates ups, State state) throws PrismLangException
	{
		ChoiceListFlexi ch;
		List<Update> list;
		int i, n;
		double p, sum;

		// Create choice and add all info
		ch = new ChoiceListFlexi();
		ch.setModuleOrActionIndex(moduleOrActionIndex);
		n = ups.getNumUpdates();
		sum = 0;
		for (i = 0; i < n; i++) {
			// Compute probability/rate
			p = ups.getProbabilityInState(i, state);
			// Check for negative/NaN probabilities/rates
			if (Double.isNaN(p) || p < 0) {
				String s = modelType.choicesSumToOne() ? "Probability" : "Rate";
				s += " is invalid (" + p + ") in state " + state.toString(modulesFile);
				// Note: we indicate error in whole Updates object because the offending
				// probability expression has probably been simplified from original form.
				throw new PrismLangException(s, ups);
			}
			// Skip transitions with zero probability/rate
			if (p == 0)
				continue;
			sum += p;
			list = new ArrayList<Update>();
			list.add(ups.getUpdate(i));
			ch.add(p, list);
		}
		// For now, PRISM treats empty (all zero probs/rates) distributions as an error.
		// Later, when errors in symbolic model construction are improved, this might be relaxed.
		if (ch.size() == 0) {
			String msg = modelType.probabilityOrRate();
			msg += (ups.getNumUpdates() > 1) ? " values sum to " : " is ";
			msg += "zero for updates in state " + state.toString(modulesFile);
			throw new PrismLangException(msg, ups);
		}
		// Check distribution sums to 1 (if required, and if is non-empty)
		if (doProbChecks && ch.size() > 0 && modelType.choicesSumToOne() && Math.abs(sum - 1) > sumRoundOff) {
			throw new PrismLangException("Probabilities sum to " + sum + " in state " + state.toString(modulesFile), ups);
		}
		return ch;
	}

	/**
	 * Create a new Choice object (currently ChoiceListFlexi) based on the product
	 * of an existing ChoiceListFlexi and an Updates object, for some (global) state.
	 * If appropriate, check probabilities sum to 1 too.
	 * @param ups The Updates object 
	 * @param state Global state
	 * @param ch The existing Choices object
	 */
	private void processUpdatesAndAddToProduct(Updates ups, State state, ChoiceListFlexi ch) throws PrismLangException
	{
		// Create new choice (action index is 0 - not needed)
		ChoiceListFlexi chNew = processUpdatesAndCreateNewChoice(0, ups, state);
		// Build product with existing
		ch.productWith(chNew);
	}
}
