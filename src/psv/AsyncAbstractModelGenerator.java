package psv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.ast.Module;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.PrismException;
import simulator.ChoiceListFlexi;

class AsyncAbstractModelGenerator extends AsyncConcreteModelGenerator {
	private List<Module> abstractAgents = new ArrayList<>();
	private List<VarList> abstractVarLists = new ArrayList<>();

	AsyncAbstractModelGenerator(AsyncSwarmFile sf, List<Integer> index) throws PrismException {
		super(sf, index);
		for (int i = 0; i < index.size(); ++i) {
			VarList abstractVarList = new VarList();
			RenamedModule rm = new RenamedModule("agent", "agent" + i + "Abs");
			for (Declaration decl : swarmFile.getAgents().get(i).getDeclarations()) {
				Declaration declarationCopy = (Declaration) decl.deepCopy();
				declarationCopy.setName(decl.getName() + "_" + i + "_abs");
				abstractVarList.addVar(declarationCopy, 0, new Values());
				rm.addRename(decl.getName(), declarationCopy.getName());
			}
			Module agentCopy = (Module) swarmFile.getAgents().get(i).deepCopy();
			Module abstractAgent = (Module) agentCopy.rename(rm);
			abstractAgent.accept(
					new FindAllVars(getAbstractVarNames(abstractVarList), getAbstractVarTypes(abstractVarList)));
			abstractAgents.add(abstractAgent);
			abstractVarLists.add(abstractVarList);
		}
	}

	private List<String> getAbstractVarNames(VarList abstractVarList) {
		List<String> varNames = new ArrayList<>(abstractVarList.getNumVars());
		for (int i = 0; i < abstractVarList.getNumVars(); ++i)
			varNames.add(abstractVarList.getName(i));
		return varNames;
	}

	private List<Type> getAbstractVarTypes(VarList abstractVarList) {
		List<Type> varTypes = new ArrayList<>(abstractVarList.getNumVars());
		for (int i = 0; i < abstractVarList.getNumVars(); ++i)
			varTypes.add(abstractVarList.getType(i));
		return varTypes;
	}

	@Override
	public State getInitialState() throws PrismException {
		List<Set<State>> abstractStates = new ArrayList<>();
		for (int i = 0; i < abstractVarLists.size(); ++i) {
			VarList abstractVarList = abstractVarLists.get(i);
			State initialAbstractState = new State(abstractVarList.getNumVars());
			for (int j = 0; j < abstractVarList.getNumVars(); ++j)
				initialAbstractState.setValue(j, abstractVarList.getDeclaration(j).getStartOrDefault().evaluate());
			LinkedHashSet<State> stateSet = new LinkedHashSet<>();
			stateSet.add(initialAbstractState);
			abstractStates.add(stateSet);
		}
		State abstractState = new State(1);
		abstractState.setValue(0, abstractStates);
		return new State(super.getInitialState(), abstractState);
	}

	@Override
	void buildTransitionList() throws PrismException {
		HashMap<String, AsyncAbstractTransition> enabledGs = new HashMap<>();
		for (Map.Entry<String, ChoiceListFlexi> entry : buildTransitionsExceptGlobal().entrySet())
			enabledGs.put(entry.getKey(), new AsyncAbstractTransition(entry.getValue()));

		@SuppressWarnings("unchecked")
		List<Set<State>> stateSets = (List<Set<State>>) getExploreState().varValues[getExploreState().varValues.length
				- 1];
		for (int i = 0; i < stateSets.size(); ++i) {
			Set<State> stateSet = stateSets.get(i);
			Module abstractAgent = abstractAgents.get(i);
			for (State state : stateSet) {
				Set<String> localEnabledGs = new LinkedHashSet<>();
				for (Command command : abstractAgent.getCommands()) {
					String act = command.getSynch();
					if (command.getGuard().evaluateBoolean(state)) {
						if (swarmFile.getActionTypes().getGs().contains(act)) {
							if (enabledGs.containsKey(act)) {
								localEnabledGs.add(command.getSynch());
								enabledGs.get(act).addUpdates(command.getUpdates());
							}
						} else if (swarmFile.getActionTypes().getA().contains(act)) {
							AsyncAbstractTransition grow = new AsyncAbstractTransition(state, i, true);
							AsyncAbstractTransition shrink = new AsyncAbstractTransition(state, i, false);
							for (int j = 0; j < command.getUpdates().getNumUpdates(); ++j) {
								ArrayList<Update> list = new ArrayList<>();
								list.add(command.getUpdates().getUpdate(j));
								grow.add(command.getUpdates().getProbabilityInState(j, state), list);
								shrink.add(command.getUpdates().getProbabilityInState(j, state), list);
							}
							transitionList.add(grow);
							actionNames.add("(" + act + ", " + i + ", " + state + ", up)");
							transitionList.add(shrink);
							actionNames.add("(" + act + ", " + i + ", " + state + ", down)");
						} else if (swarmFile.getActionTypes().getAe().contains(act)) {
							AsyncAbstractTransition grow = new AsyncAbstractTransition(state, i, true);
							AsyncAbstractTransition shrink = new AsyncAbstractTransition(state, i, false);
							for (int j = 0; j < command.getUpdates().getNumUpdates(); ++j) {
								ArrayList<Update> list = new ArrayList<>();
								list.add(command.getUpdates().getUpdate(j));
								grow.add(command.getUpdates().getProbabilityInState(j, state), list);
								shrink.add(command.getUpdates().getProbabilityInState(j, state), list);
							}
							Command matchingEnv = getMatchingEnvironmentCommand(act);
							if (matchingEnv != null) {
								ChoiceListFlexi envChoice = new ChoiceListFlexi();
								for (int j = 0; j < matchingEnv.getUpdates().getNumUpdates(); ++j) {
									ArrayList<Update> list = new ArrayList<>();
									list.add(matchingEnv.getUpdates().getUpdate(j));
									envChoice.add(matchingEnv.getUpdates().getProbabilityInState(j, exploreState),
											list);
								}
								grow.productWith(envChoice);
								shrink.productWith(envChoice);
								transitionList.add(grow);
								actionNames.add("(" + act + ", " + i + ", " + state + ", up)");
								transitionList.add(shrink);
								actionNames.add("(" + act + ", " + i + ", " + state + ", down)");
							}
						} else {
							throw new PrismException(
									"Action " + command.getSynch() + " was not declared as A, AE or GS.");
						}
					}
				}
				List<String> toRemove = new LinkedList<>();
				for (String act : enabledGs.keySet()) {
					if (!localEnabledGs.contains(act))
						toRemove.add(act);
				}
				for (String act : toRemove)
					enabledGs.remove(act);
			}
		}

		for (String act : enabledGs.keySet()) {
			actionNames.add(act);
			transitionList.add(enabledGs.get(act));
		}
	}
}
