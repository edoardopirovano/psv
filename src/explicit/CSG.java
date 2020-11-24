package explicit;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import explicit.STPGExplicit;
import prism.ModelType;
import prism.PrismException;

public class CSG extends STPGExplicit {

	protected Map<Integer, BitSet> synchMap; //get rid of this
	protected Map<Integer, String> playerNames; 
	protected Map<Integer, List<String>> playerActions;  
	protected Map<Integer, Map<String, Map<String, Map<String, Double>>>> rewards; 
	
	public CSG(STPGExplicit stpg) {
		super(stpg);
		this.synchMap = new HashMap<Integer, BitSet>();
		this.playerActions = new HashMap<Integer, List<String>>();
				
		printInfo();
	}
	
	public CSG(STPGExplicit stpg, Map<Integer, BitSet> mp) {
		super(stpg);
		this.synchMap = mp;
		this.playerActions = new HashMap<Integer, List<String>>();
	}
	
	public CSG(STPGExplicit stpg, int[] permut, Map<Integer, BitSet> mp) {
		super(stpg, permut);
		this.synchMap = mp;
		this.playerActions = new HashMap<Integer, List<String>>();
	}
	
	public CSG(STPGExplicit stpg, Map<Integer, BitSet> mp,
				Map<Integer, Map<String, Map<String, Map<String, Double>>>> rw) {
		super(stpg);
		this.synchMap = mp;
		this.playerActions = new HashMap<Integer, List<String>>();
		this.rewards = rw;
	}
	
	public Map<Integer, BitSet> getSynchMap() {
		return this.synchMap;
	}
	
	public int getNumPlayers() {
		return this.playerNames.size();
	}

	public String getPlayerName(int p) {
		return this.playerNames.get(p);
	}
	
	public Map<Integer, String> getPlayerNames () {
		return this.playerNames;
	}
	
	public List<String> getActionsForPlayer(int p) {
		return this.playerActions.get(p);
	}
	
	public Map<Integer, Map<String, Map<String, Map<String, Double>>>> getRewards() {
		return this.rewards;
	}
	
	public void setSynch(BitSet b, int i) {
		this.synchMap.put(i, b);
	}
	
	public void setPlayerInfo(Map<Integer, String> playerNames) {
		this.playerNames = new HashMap<Integer, String>(playerNames);
	}
	
	public void setActionsForPlayer(List<String> a, int p) {
		this.playerActions.put(p, a);
	}
	
	public void printInfo() {	
		for(int s = 0; s < this.getNumStates(); s++) {
			System.out.println("-- s " + s);
			for(int t = 0; t < this.getNumChoices(s); t++) {
				System.out.println(this.getAction(s, t));
				System.out.println(this.getChoice(s, t));
			}
		}
	}
	
	@Override
	public ModelType getModelType() {
		return ModelType.CSG;
	}

	@Override
	public String getStateName(int i) {
		return statesList.get(i).toString();
	}
}
