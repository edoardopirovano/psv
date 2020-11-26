package psv;

import parser.State;
import parser.ast.*;
import parser.visitor.FindAllVars;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.List;

public class FaultFileFaultProvider extends FaultProvider {
    FaultFile faultFile;
    List<List<List<Fault>>> concreteFaults = new ArrayList<>();
    List<List<Fault>> abstractFaults = new ArrayList<>();

    FaultFileFaultProvider(final FaultFile faultFile) {
        this.faultFile = faultFile;
    }

    @Override
    void introduceConcrete(final RenamedModule rm, final int type) throws PrismLangException {
        if (concreteFaults.size() == type)
            concreteFaults.add(new ArrayList<>());
        final List<Fault> toAdd = new ArrayList<>();
        for (final Fault fault : faultFile.getFaults().get(type)) {
            Fault copy = (Fault) fault.deepCopy();
            copy = (Fault) copy.rename(rm);
            toAdd.add(copy);
        }
        concreteFaults.get(type).add(toAdd);
    }

    @Override
    public void introduceAbstract(final RenamedModule rm, final FindAllVars varVisitor) throws PrismLangException {
        final List<Fault> toAdd = new ArrayList<>();
        for (final Fault fault : faultFile.getFaults().get(abstractFaults.size())) {
            Fault copy = (Fault) fault.deepCopy();
            copy = (Fault) copy.rename(rm);
            copy.accept(varVisitor);
            toAdd.add(copy);
        }
        abstractFaults.add(toAdd);
    }

    @Override
    public void accept(final FindAllVars replacer) throws PrismLangException {
        for (final List<List<Fault>> fll : concreteFaults) {
            for (final List<Fault> faults : fll) {
                for (final Fault fault : faults)
                    fault.accept(replacer);
            }
        }
    }

    @Override
    protected ChoiceListFlexi getChoice(final Command command, final State exploreState, final int agentType, final int agent) throws PrismLangException {
        final ChoiceListFlexi result = super.getChoice(command, exploreState, agentType, agent);
        if (agentType == -1) return result;
        if (agentType == -2) {
            for (int i = 0; i < concreteFaults.size(); ++i) {
                for (int j = 0; j < concreteFaults.get(i).size(); ++j)
                    productWith(result, command, exploreState, i, j);
            }
            return result;
        }
        productWith(result, command, exploreState, agentType, agent);
        return result;
    }

    private void productWith(final ChoiceListFlexi result, final Command command, final State exploreState, final int agentType, final int agent) throws PrismLangException {
        for (final Fault fault : concreteFaults.get(agentType).get(agent)) {
            if ((fault.getCommand().equals(command.getSynch()) || fault.getCommand().equals("_")) &&
                    fault.getGuard().evaluateBoolean(exploreState)) {
                final ChoiceListFlexi choice = new ChoiceListFlexi();
                for (int j = 0; j < fault.getUpdates().getNumUpdates(); ++j) {
                    final ArrayList<Update> list = new ArrayList<>();
                    list.add(fault.getUpdates().getUpdate(j));
                    choice.add(fault.getUpdates().getProbabilityInState(j, exploreState), list);
                }
                result.productWith(choice);
            }
        }
    }

    @Override
    void buildGrowAndShrinkTransitions(final int agentType, final State state, final Command command,
                                       final AsyncAbstractTransition grow,
                                       final AsyncAbstractTransition shrink) throws PrismLangException {
        super.buildGrowAndShrinkTransitions(agentType, state, command, grow, shrink);
        for (final Fault fault : abstractFaults.get(agentType)) {
            if ((fault.getCommand().equals(command.getSynch()) ||
                    fault.getCommand().equals("_")) &&
                    fault.getGuard().evaluateBoolean(state)) {
                final ChoiceListFlexi growChoice = new ChoiceListFlexi();
                final ChoiceListFlexi shrinkChoice = new ChoiceListFlexi();
                for (int j = 0; j < fault.getUpdates().getNumUpdates(); ++j) {
                    final ArrayList<Update> list1 = new ArrayList<>();
                    list1.add((Update) fault.getUpdates().getUpdate(j).deepCopy());
                    final ArrayList<Update> list2 = new ArrayList<>();
                    list2.add((Update) fault.getUpdates().getUpdate(j).deepCopy());
                    growChoice.add(fault.getUpdates().getProbabilityInState(j, state), list1);
                    shrinkChoice.add(fault.getUpdates().getProbabilityInState(j, state), list2);
                }
                grow.productWith(growChoice);
                shrink.productWith(shrinkChoice);
            }
        }
    }
}
