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

public class FaultyConcreteModelGenerator extends AsyncConcreteModelGenerator {
    FaultFile faultFile;

    FaultyConcreteModelGenerator(final AsyncSwarmFile swarmFile, final FaultFile faultFile, final List<Integer> numAgents) throws PrismException {
        super(swarmFile, numAgents);
        this.faultFile = faultFile;
    }

    @Override
    protected ChoiceListFlexi getChoice(final Command command, final State exploreState, final int agentType, final int agent) throws PrismLangException {
        final ChoiceListFlexi result = super.getChoice(command, exploreState, agentType, agent);
        if (agent == -1) return result;
        if (agent == -2) {
            for (int i = 0; i < renamedAgents.size(); ++i)
                productWith(result, command, exploreState, i);
            return result;
        }
        productWith(result, command, exploreState, agent);
        return result;
    }

    private void productWith(final ChoiceListFlexi result, final Command command, final State exploreState, final int agent) throws PrismLangException {
        final FaultFile ff = renamedFaultFiles.get(agent);
        for (int i = 0; i < ff.getCommands().size(); ++i) {
            if ((ff.getCommands().get(i).equals(command.getSynch()) || ff.getCommands().get(i).equals("_")) &&
                    ff.getGuards().get(i).evaluateBoolean(exploreState)) {
                final ChoiceListFlexi choice = new ChoiceListFlexi();
                for (int j = 0; j < ff.getUpdates().get(i).getNumUpdates(); ++j) {
                    final ArrayList<Update> list = new ArrayList<>();
                    list.add(ff.getUpdates().get(i).getUpdate(j));
                    choice.add(ff.getUpdates().get(i).getProbabilityInState(j, exploreState), list);
                }
                result.productWith(choice);
            }
        }
    }
}
