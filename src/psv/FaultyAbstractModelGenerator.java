package psv;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.*;
import parser.ast.Module;
import parser.type.Type;
import parser.visitor.FindAllVars;
import prism.PrismException;
import simulator.ChoiceListFlexi;

import java.util.*;

class FaultyAbstractModelGenerator extends FaultyConcreteModelGenerator {
    private final Module abstractAgent;
    private final FaultFile abstractFaultFile;
    private final VarList abstractVarList = new VarList();

    FaultyAbstractModelGenerator(AsyncSwarmFile sf, FaultFile ff, List<Integer> index) throws PrismException {
        super(sf, ff, index);
        RenamedModule rm = new RenamedModule("agent", "agentAbs");
        for (Declaration decl : swarmFile.getAgents().get(0).getDeclarations()) {
            Declaration declarationCopy = (Declaration) decl.deepCopy();
            declarationCopy.setName(decl.getName() + "_abs");
            abstractVarList.addVar(declarationCopy, 0, new Values());
            rm.addRename(decl.getName(), declarationCopy.getName());
        }
        Module agentCopy = (Module) swarmFile.getAgents().get(0).deepCopy();
        abstractAgent = (Module) agentCopy.rename(rm);
        FaultFile ffCopy = (FaultFile) ff.deepCopy();
        abstractFaultFile = (FaultFile) ffCopy.rename(rm);
        FindAllVars varVisitor = new FindAllVars(getAbstractVarNames(), getAbstractVarTypes());
        abstractAgent.accept(varVisitor);
        abstractFaultFile.accept(varVisitor);
    }

    private List<String> getAbstractVarNames() {
        List<String> varNames = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varNames.add(abstractVarList.getName(i));
        return varNames;
    }

    private List<Type> getAbstractVarTypes() {
        List<Type> varTypes = new ArrayList<>(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            varTypes.add(abstractVarList.getType(i));
        return varTypes;
    }

    @Override
    public State getInitialState() throws PrismException {
        State initialState = super.getInitialState();
        State initialAbstractState = new State(abstractVarList.getNumVars());
        for (int i = 0; i < abstractVarList.getNumVars(); ++i)
            initialAbstractState.setValue(i, abstractVarList.getDeclaration(i).getStartOrDefault().evaluate());
        LinkedHashSet<State> stateSet = new LinkedHashSet<>();
        stateSet.add(initialAbstractState);
        State abstractState = new State(1);
        abstractState.setValue(0, stateSet);
        return new State(initialState, abstractState);
    }

    @Override
    void buildTransitionList() throws PrismException {
        HashMap<String, FaultyAbstractTransition> enabledGs = new HashMap<>();
        for (Map.Entry<String, ChoiceListFlexi> entry : buildTransitionsExceptGlobal().entrySet())
            enabledGs.put(entry.getKey(), new FaultyAbstractTransition(entry.getValue()));

        @SuppressWarnings("unchecked")
        LinkedHashSet<State> stateSet = (LinkedHashSet<State>) getExploreState().varValues[getExploreState().varValues.length - 1];

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
                        FaultyAbstractTransition grow = new FaultyAbstractTransition(state, true);
                        FaultyAbstractTransition shrink = new FaultyAbstractTransition(state, false);
                        for (int i = 0; i < command.getUpdates().getNumUpdates(); ++i) {
                            ArrayList<Update> list1 = new ArrayList<>();
                            list1.add((Update) command.getUpdates().getUpdate(i).deepCopy());
                            ArrayList<Update> list2 = new ArrayList<>();
                            list2.add((Update) command.getUpdates().getUpdate(i).deepCopy());
                            grow.add(command.getUpdates().getProbabilityInState(i, state), list1);
                            shrink.add(command.getUpdates().getProbabilityInState(i, state), list2);
                        }
                        for (int i = 0; i < abstractFaultFile.getCommands().size(); ++i) {
                            if ((abstractFaultFile.getCommands().get(i).equals(command.getSynch()) ||
                                    abstractFaultFile.getCommands().get(i).equals("_")) &&
                                    abstractFaultFile.getGuards().get(i).evaluateBoolean(state)) {
                                ChoiceListFlexi growChoice = new ChoiceListFlexi();
                                ChoiceListFlexi shrinkChoice = new ChoiceListFlexi();
                                for (int j = 0; j < abstractFaultFile.getUpdates().get(i).getNumUpdates(); ++j) {
                                    ArrayList<Update> list1 = new ArrayList<>();
                                    list1.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                                    ArrayList<Update> list2 = new ArrayList<>();
                                    list2.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                                    growChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list1);
                                    shrinkChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list2);
                                }
                                grow.productWith(growChoice);
                                shrink.productWith(shrinkChoice);
                            }
                        }
                        transitionList.add(grow);
                        actionNames.add("(" + act + ", " + state + ", up)");
                        transitionList.add(shrink);
                        actionNames.add("(" + act + ", " + state + ", down)");
                    } else if (swarmFile.getActionTypes().getAe().contains(act)) {
                        FaultyAbstractTransition grow = new FaultyAbstractTransition(state, true);
                        FaultyAbstractTransition shrink = new FaultyAbstractTransition(state, false);
                        for (int i = 0; i < command.getUpdates().getNumUpdates(); ++i) {
                            ArrayList<Update> list1 = new ArrayList<>();
                            list1.add((Update) command.getUpdates().getUpdate(i).deepCopy());
                            ArrayList<Update> list2 = new ArrayList<>();
                            list2.add((Update) command.getUpdates().getUpdate(i).deepCopy());
                            grow.add(command.getUpdates().getProbabilityInState(i, state), list1);
                            shrink.add(command.getUpdates().getProbabilityInState(i, state), list2);
                        }
                        for (int i = 0; i < abstractFaultFile.getCommands().size(); ++i) {
                            if ((abstractFaultFile.getCommands().get(i).equals(command.getSynch()) ||
                                    abstractFaultFile.getCommands().get(i).equals("_")) &&
                                    abstractFaultFile.getGuards().get(i).evaluateBoolean(state)) {
                                ChoiceListFlexi growChoice = new ChoiceListFlexi();
                                ChoiceListFlexi shrinkChoice = new ChoiceListFlexi();
                                for (int j = 0; j < abstractFaultFile.getUpdates().get(i).getNumUpdates(); ++j) {
                                    ArrayList<Update> list1 = new ArrayList<>();
                                    list1.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                                    ArrayList<Update> list2 = new ArrayList<>();
                                    list2.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                                    growChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list1);
                                    shrinkChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list2);
                                }
                                grow.productWith(growChoice);
                                shrink.productWith(shrinkChoice);
                            }
                        }
                        Command matchingEnv = getMatchingEnvironmentCommand(act);
                        if (matchingEnv != null) {
                            ChoiceListFlexi envChoice = new ChoiceListFlexi();
                            for (int i = 0; i < matchingEnv.getUpdates().getNumUpdates(); ++i) {
                                ArrayList<Update> list = new ArrayList<>();
                                list.add(matchingEnv.getUpdates().getUpdate(i));
                                envChoice.add(matchingEnv.getUpdates().getProbabilityInState(i, exploreState), list);
                            }
                            grow.productWith(envChoice);
                            shrink.productWith(envChoice);
                            transitionList.add(grow);
                            actionNames.add("(" + act + ", " + state + ", up)");
                            transitionList.add(shrink);
                            actionNames.add("(" + act + ", " + state + ", down)");
                        }
                    } else {
                        throw new PrismException("Action " + command.getSynch() + " was not declared as A, AE or GS.");
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

        for (String act : enabledGs.keySet()) {
            actionNames.add(act);
            transitionList.add(enabledGs.get(act));
        }
    }
}
