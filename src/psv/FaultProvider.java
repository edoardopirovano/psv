package psv;

import parser.State;
import parser.ast.Command;
import parser.ast.RenamedModule;
import parser.ast.Update;
import parser.visitor.FindAllVars;
import prism.PrismLangException;
import simulator.ChoiceListFlexi;

import java.util.ArrayList;

public class FaultProvider {
    void introduceConcrete(final RenamedModule rm, final int type) throws PrismLangException {
    }

    public void introduceAbstract(final RenamedModule rm, final FindAllVars varVisitor) throws PrismLangException {
    }

    public void accept(final FindAllVars replacer) throws PrismLangException {
    }

    ChoiceListFlexi getChoice(final Command command, final State exploreState, final int agentType, final int agent) throws PrismLangException {
        final ChoiceListFlexi choice = new ChoiceListFlexi();
        for (int i = 0; i < command.getUpdates().getNumUpdates(); ++i) {
            final ArrayList<Update> list = new ArrayList<>();
            list.add(command.getUpdates().getUpdate(i));
            choice.add(command.getUpdates().getProbabilityInState(i, exploreState), list);
        }
        return choice;
    }

    void buildGrowAndShrinkTransitions(final int agentType, final State state, final Command command,
                                       final AsyncAbstractTransition grow,
                                       final AsyncAbstractTransition shrink) throws PrismLangException {
        for (int j = 0; j < command.getUpdates().getNumUpdates(); ++j) {
            final ArrayList<Update> list = new ArrayList<>();
            list.add(command.getUpdates().getUpdate(j));
            grow.add(command.getUpdates().getProbabilityInState(j, state), list);
            shrink.add(command.getUpdates().getProbabilityInState(j, state), list);
        }
    }
}
