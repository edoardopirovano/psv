package psv;

import parser.State;
import parser.ast.AsyncSwarmFile;
import parser.ast.Command;
import parser.ast.FaultFile;
import parser.ast.Update;
import prism.PrismException;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;
import java.util.List;

class FaultyAbstractModelGenerator extends AsyncAbstractModelGenerator {
    private final FaultFile faultFile;

    FaultyAbstractModelGenerator(final AsyncSwarmFile sf, final FaultFile ff, final List<Integer> index) throws PrismException {
        super(sf, index);
        this.faultFile = ff;
    }

    @Override
    void buildGrowAndShrinkTransitions(final State state, final Command command, final AsyncAbstractTransition grow, final AsyncAbstractTransition shrink) throws PrismLangException {
        super.buildGrowAndShrinkTransitions(state, command, grow, shrink);
        for (int i = 0; i < abstractFaultFile.getCommands().size(); ++i) {
            if ((abstractFaultFile.getCommands().get(i).equals(command.getSynch()) ||
                    abstractFaultFile.getCommands().get(i).equals("_")) &&
                    abstractFaultFile.getGuards().get(i).evaluateBoolean(state)) {
                final ChoiceListFlexi growChoice = new ChoiceListFlexi();
                final ChoiceListFlexi shrinkChoice = new ChoiceListFlexi();
                for (int j = 0; j < abstractFaultFile.getUpdates().get(i).getNumUpdates(); ++j) {
                    final ArrayList<Update> list1 = new ArrayList<>();
                    list1.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                    final ArrayList<Update> list2 = new ArrayList<>();
                    list2.add((Update) abstractFaultFile.getUpdates().get(i).getUpdate(j).deepCopy());
                    growChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list1);
                    shrinkChoice.add(abstractFaultFile.getUpdates().get(i).getProbabilityInState(j, state), list2);
                }
                grow.productWith(growChoice);
                shrink.productWith(shrinkChoice);
            }
        }
    }
}
