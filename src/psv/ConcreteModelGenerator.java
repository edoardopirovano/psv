package psv;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import simulator.CSGModelGenerator;
import simulator.ChoiceListFlexi;
import simulator.TransitionList;

import java.util.*;

public class ConcreteModelGenerator extends DefaultModelGenerator implements CSGModelGenerator {
    SwarmFile swarmFile;

    private VarList varList = new VarList();
    private LabelList labelList;
    private List<String> labelNames;
    List<Agent> renamedAgents;
    Agent environment;

    // Model exploration info

    // State currently being explored
    protected State exploreState;

    // List of currently available transitions
    List<String> actionNames = new ArrayList<>();
    TransitionList transitionList = new TransitionList();

    // Has the transition list been built?
    private boolean transitionListBuilt;

    ConcreteModelGenerator(SwarmFile swarmFile, int n) throws PrismException {
        this.swarmFile = swarmFile;
        Values constantValues = new Values();

        renamedAgents = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            RenamedModule rm = new RenamedModule("agent", "agent_" + i);
            for (Declaration decl : swarmFile.getAgent().getDecls()) {
                Declaration declarationCopy = (Declaration) decl.deepCopy();
                declarationCopy.setName(decl.getName() + "_" + i);
                varList.addVar(declarationCopy, 0, constantValues);
                rm.addRename(decl.getName(), declarationCopy.getName());
            }
            Agent agentCopy = (Agent) swarmFile.getAgent().deepCopy();
            agentCopy = (Agent) agentCopy.rename(rm);
            renamedAgents.add(agentCopy);
        }
        RenamedModule rm = new RenamedModule("environment", "env");
        for (Declaration decl : swarmFile.getEnvironment().getDecls()) {
            Declaration declarationCopy = (Declaration) decl.deepCopy();
            declarationCopy.setName(decl.getName() + "_env");
            varList.addVar(declarationCopy, 0, constantValues);
            rm.addRename(decl.getName(), declarationCopy.getName());
        }
        environment = (Agent) swarmFile.getEnvironment().deepCopy();
        environment = (Agent) environment.rename(rm);
        labelList = (LabelList) swarmFile.getLabelList().deepCopy();
        labelNames = labelList.getLabelNames();

        FindAllVars replacer = new FindAllVars(getVarNames(), getVarTypes());
        for (Agent agent : renamedAgents)
            agent.accept(replacer);
        environment.accept(replacer);
        labelList.accept(replacer);

        transitionList = new TransitionList();
        transitionListBuilt = false;
    }

    @Override
    public ModelType getModelType()
    {
        return ModelType.CSG;
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
    public int getNumLabels()
    {
        return labelList.size();
    }

    @Override
    public List<String> getLabelNames()
    {
        return labelNames;
    }

    @Override
    public String getLabelName(int i) throws PrismException {
        return labelList.getLabelName(i);
    }

    @Override
    public int getLabelIndex(String label)
    {
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
    public State getExploreState()
    {
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
        return 1;
    }

    @Override
    public boolean isLabelTrue(int i) throws PrismException {
        Expression expr = labelList.getLabel(i);
        return expr.evaluateBoolean(exploreState);
    }


    @Override
    public VarList createVarList()
    {
        return varList;
    }

    /**
     * Returns the current list of available transitions, generating it first if this has not yet been done.
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
        for (List<String> jointAction : getJointActions()) {
            StringBuilder actionString = new StringBuilder();
            for (String action : jointAction)
                actionString.append("[").append(action).append("]");
            actionNames.add(actionString.toString());
            ActionSet<String> actionSet = new ActionSet<>();
            for (String action : jointAction)
                actionSet.add(action.substring(0,action.lastIndexOf("_")));
            ChoiceListFlexi choice = new ChoiceListFlexi();
            choice.add(1.0, new LinkedList<>());
            addUpdates(jointAction.get(0), actionSet, choice, environment);
            int i = 1;
            for (Agent renamedAgent : renamedAgents)
                addUpdates(jointAction.get(i++), actionSet, choice, renamedAgent);
            transitionList.add(choice);
        }
    }

    List<List<String>> getJointActions() throws PrismLangException {
        List<List<String>> jointActions = new LinkedList<>();
        for (Action action : environment.getActions()) {
            if (action.getCondition().evaluateBoolean(exploreState)) {
                LinkedList<String> actions = new LinkedList<>();
                actions.add(action.getName() + "_0");
                jointActions.add(actions);
            }
        }
        for (int i = 1; i <= renamedAgents.size(); ++i) {
            List<List<String>> newJointActions = new LinkedList<>();
            for (Action action : renamedAgents.get(i - 1).getActions()) {
                if (action.getCondition().evaluateBoolean(exploreState)) {
                    for (List<String> jointAction : jointActions) {
                        LinkedList<String> newJointAction = new LinkedList<>(jointAction);
                        newJointAction.add(action.getName() + "_" + i);
                        newJointActions.add(newJointAction);
                    }
                }
            }
            jointActions = newJointActions;
        }
        return jointActions;
    }

    void addUpdates(String action, Set<String> actionSet, ChoiceListFlexi choice, Agent agent) throws PrismLangException {
        for (AgentUpdate update : agent.getUpdates()) {
            if (update.isEnabled(exploreState, actionSet, action)) {
                ChoiceListFlexi newChoice = new ChoiceListFlexi();
                for (int i = 0; i < update.getUpdates().getNumUpdates(); ++i) {
                    ArrayList<Update> list = new ArrayList<>();
                    list.add(update.getUpdates().getUpdate(i));
                    newChoice.add(update.getUpdates().getProbabilityInState(i, exploreState), list);
                }
                if (newChoice.getProbabilitySum() != 1.0)
                    newChoice.add(1.0 - newChoice.getProbabilitySum(), new LinkedList<>());
                choice.productWith(newChoice);
            }
        }
    }

    @Override
    public boolean rewardStructHasTransitionRewards(int i) {
        return false;
    }

    @Override
    public int getNumPlayers() {
        return renamedAgents.size() + 1;
    }

    @Override
    public Player getPlayer(int i) {
        Player player = new Player(i == 0 ? "env" : "agent_" + i);
        Agent agent = (i == 0 ? environment : renamedAgents.get(i - 1));
        for (Action action : agent.getActions())
            player.addAction(action.getName() + "_" + i);
        return player;
    }

    @Override
    public Map<Integer, BitSet> getSynchMap() {
        return new HashMap<>();
    }
}
