package psv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.ast.Module;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;
import simulator.TransitionList;

public class AsyncConcreteModelGenerator extends DefaultModelGenerator {
	AsyncSwarmFile swarmFile;

	private VarList varList = new VarList();
	private LabelList labelList;
	private List<String> labelNames;
	private List<List<Module>> renamedAgents;
	private Module environment;

	// Model exploration info

	// State currently being explored
	State exploreState;

	// List of currently available transitions
	List<String> actionNames = new ArrayList<>();
	TransitionList transitionList = new TransitionList();

	// Has the transition list been built?
	private boolean transitionListBuilt;

	AsyncConcreteModelGenerator(AsyncSwarmFile swarmFile, List<Integer> m) throws PrismException {
		this.swarmFile = swarmFile;
		Values constantValues = new Values();

		renamedAgents = new ArrayList<>();
		for (int i = 0; i < m.size(); ++i) {
			List<Module> renamed = new ArrayList<>();
			for (int j = 0; j < m.get(i); ++j) {
				RenamedModule rm = new RenamedModule("agent", "agent_" + i + "_" + j);
				for (Declaration decl : swarmFile.getAgents().get(i).getDeclarations()) {
					Declaration declarationCopy = (Declaration) decl.deepCopy();
					declarationCopy.setName(decl.getName() + "_" + i + "_" + j);
					varList.addVar(declarationCopy, 0, constantValues);
					rm.addRename(decl.getName(), declarationCopy.getName());
				}
				Module agentCopy = (Module) swarmFile.getAgents().get(i).deepCopy();
				agentCopy = (Module) agentCopy.rename(rm);
				renamed.add(agentCopy);
			}
			renamedAgents.add(renamed);
		}
		RenamedModule rm = new RenamedModule("environment", "environment");
		for (Declaration decl : swarmFile.getEnvironment().getDeclarations()) {
			Declaration declarationCopy = (Declaration) decl.deepCopy();
			declarationCopy.setName(decl.getName() + "_E");
			varList.addVar(declarationCopy, 0, constantValues);
			rm.addRename(decl.getName(), declarationCopy.getName());
		}
		environment = (Module) swarmFile.getEnvironment().deepCopy();
		environment = (Module) environment.rename(rm);
		labelList = (LabelList) swarmFile.getLabelList().deepCopy();
		labelNames = labelList.getLabelNames();

		FindAllVars replacer = new FindAllVars(getVarNames(), getVarTypes());
		for (List<Module> agents : renamedAgents) {
			for (Module agent : agents)
				agent.accept(replacer);
		}
		environment.accept(replacer);
		labelList.accept(replacer);

		transitionList = new TransitionList();
		transitionListBuilt = false;
	}

	@Override
	public ModelType getModelType() {
		return ModelType.MDP;
	}

	@Override
	public int getNumVars() {
		return varList.getNumVars();
	}

	@Override
	public List<String> getVarNames() {
		List<String> varNames = new ArrayList<>(varList.getNumVars());
		for (int i = 0; i < varList.getNumVars(); ++i)
			varNames.add(varList.getName(i));
		return varNames;
	}

	@Override
	public List<Type> getVarTypes() {
		List<Type> varTypes = new ArrayList<>(varList.getNumVars());
		for (int i = 0; i < varList.getNumVars(); ++i)
			varTypes.add(varList.getType(i));
		return varTypes;
	}

	@Override
	public int getNumLabels() {
		return labelList.size();
	}

	@Override
	public List<String> getLabelNames() {
		return labelNames;
	}

	@Override
	public String getLabelName(int i) throws PrismException {
		return labelList.getLabelName(i);
	}

	@Override
	public int getLabelIndex(String label) {
		return labelList.getLabelIndex(label);
	}

	@Override
	public boolean hasSingleInitialState() throws PrismException {
		return true;
	}

	@Override
	public State getInitialState() throws PrismException {
		State initialState = new State(getNumVars());
		for (int i = 0; i < varList.getNumVars(); ++i)
			initialState.setValue(i, varList.getDeclaration(i).getStartOrDefault().evaluate());
		return initialState;
	}

	@Override
	public void exploreState(State exploreState) throws PrismException {
		this.exploreState = exploreState;
		transitionListBuilt = false;
	}

	@Override
	public State getExploreState() {
		return exploreState;
	}

	@Override
	public int getNumChoices() throws PrismException {
		return getTransitionList().getNumChoices();
	}

	@Override
	public int getNumTransitions() throws PrismException {
		return getTransitionList().getNumTransitions();
	}

	@Override
	public int getNumTransitions(int index) throws PrismException {
		return getTransitionList().getChoice(index).size();
	}

	@Override
	public Object getTransitionAction(int i) throws PrismException {
		getTransitionList();
		return actionNames.get(i);
	}

	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException {
		return getTransitionAction(i);
	}

	@Override
	public double getTransitionProbability(int index, int offset) throws PrismException {
		TransitionList transitions = getTransitionList();
		return transitions.getChoice(index).getProbability(offset);
	}

	@Override
	public State computeTransitionTarget(int index, int offset) throws PrismException {
		return getTransitionList().getChoice(index).computeTarget(offset, exploreState);
	}

	@Override
	public int getPlayerNumberForChoice(int i) throws PrismException {
		return -1;
	}

	@Override
	public boolean isLabelTrue(int i) throws PrismException {
		Expression expr = labelList.getLabel(i);
		return expr.evaluateBoolean(exploreState);
	}

	@Override
	public VarList createVarList() {
		return varList;
	}

	/**
	 * Returns the current list of available transitions, generating it first if
	 * this has not yet been done.
	 */
	private TransitionList getTransitionList() throws PrismException {
		if (!transitionListBuilt) {
			actionNames.clear();
			transitionList.clear();
			buildTransitionList();
			transitionListBuilt = true;
		}
		return transitionList;
	}

	void buildTransitionList() throws PrismException {
		HashMap<String, ChoiceListFlexi> enabledGs = buildTransitionsExceptGlobal();
		for (String act : enabledGs.keySet()) {
			actionNames.add(act);
			transitionList.add(enabledGs.get(act));
		}
	}

	HashMap<String, ChoiceListFlexi> buildTransitionsExceptGlobal() throws PrismException {
		HashMap<String, ChoiceListFlexi> enabledGs = new HashMap<>();
		for (Command command : environment.getCommands()) {
			if (command.getGuard().evaluateBoolean(exploreState)) {
				if (swarmFile.getActionTypes().getGs().contains(command.getSynch())) {
					ChoiceListFlexi choice = new ChoiceListFlexi();
					for (int i = 0; i < command.getUpdates().getNumUpdates(); ++i) {
						ArrayList<Update> list = new ArrayList<>();
						list.add(command.getUpdates().getUpdate(i));
						choice.add(command.getUpdates().getProbabilityInState(i, exploreState), list);
					}
					enabledGs.put(command.getSynch(), choice);
				} else if (swarmFile.getActionTypes().getA().contains(command.getSynch())) {
					ChoiceListFlexi choice = new ChoiceListFlexi();
					for (int i = 0; i < command.getUpdates().getNumUpdates(); ++i) {
						ArrayList<Update> list = new ArrayList<>();
						list.add(command.getUpdates().getUpdate(i));
						choice.add(command.getUpdates().getProbabilityInState(i, exploreState), list);
					}
					actionNames.add("(" + command.getSynch() + ", E)");
					transitionList.add(choice);
				} else if (!swarmFile.getActionTypes().getAe().contains(command.getSynch())) {
					throw new PrismException("Action " + command.getSynch() + " was not declared as A, AE or GS.");
				}
			}
		}
		for (int i = 0; i < renamedAgents.size(); ++i) {
			for (int j = 0; j < renamedAgents.get(i).size(); ++j) {
				Module agent = renamedAgents.get(i).get(j);
				HashSet<String> enabledForAgent = new HashSet<>();
				for (Command command : agent.getCommands()) {
					if (command.getGuard().evaluateBoolean(exploreState)) {
						String act = command.getSynch();
						ChoiceListFlexi choice = new ChoiceListFlexi();
						for (int k = 0; k < command.getUpdates().getNumUpdates(); ++k) {
							ArrayList<Update> list = new ArrayList<>();
							list.add(command.getUpdates().getUpdate(k));
							choice.add(command.getUpdates().getProbabilityInState(k, exploreState), list);
						}
						if (swarmFile.getActionTypes().getA().contains(act)) {
							actionNames.add("(" + act + ", (" + i + "," + j + "))");
							transitionList.add(choice);
						} else if (swarmFile.getActionTypes().getAe().contains(act)) {
							Command matchingEnv = getMatchingEnvironmentCommand(act);
							if (matchingEnv != null) {
								ChoiceListFlexi envChoice = new ChoiceListFlexi();
								for (int k = 0; k < matchingEnv.getUpdates().getNumUpdates(); ++k) {
									ArrayList<Update> list = new ArrayList<>();
									list.add(matchingEnv.getUpdates().getUpdate(k));
									envChoice.add(matchingEnv.getUpdates().getProbabilityInState(k, exploreState),
											list);
								}
								choice.productWith(envChoice);
								actionNames.add("(" + act + ", (" + i + "," + j + "))");
								transitionList.add(choice);
							}
						} else if (swarmFile.getActionTypes().getGs().contains(act)) {
							enabledForAgent.add(act);
							if (enabledGs.containsKey(act))
								enabledGs.get(act).productWith(choice);
						} else {
							throw new PrismException("Action " + act + " was not declared as A, AE or GS.");
						}
					}
				}
				List<String> toRemove = new LinkedList<>();
				for (String act : enabledGs.keySet()) {
					if (!enabledForAgent.contains(act))
						toRemove.add(act);
				}
				for (String act : toRemove)
					enabledGs.remove(act);
			}
		}
		return enabledGs;
	}

	Command getMatchingEnvironmentCommand(String actionName) throws PrismLangException {
		for (Command command : environment.getCommands()) {
			if (command.getSynch().equals(actionName) && command.getGuard().evaluateBoolean(exploreState))
				return command;
		}
		return null;
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int i) {
		return false;
	}

	@Override
	public int getNumPlayers() {
		return 0;
	}

	@Override
	public Player getPlayer(int i) {
		return null;
	}
}
