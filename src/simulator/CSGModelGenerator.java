package simulator;

import prism.ModelGenerator;

import java.util.BitSet;
import java.util.Map;

public interface CSGModelGenerator extends ModelGenerator {
    Map<Integer, BitSet> getSynchMap();
}
