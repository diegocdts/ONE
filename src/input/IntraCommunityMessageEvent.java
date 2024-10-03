package input;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Settings;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	private Map<Integer, List<Integer>> currentNodesPerCommunity;
	private Map<Integer, Integer> sizeCommunities = new HashMap<Integer, Integer>();
	private int nextEventTimeDiff;
	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setNextMsgEventInfo(Map<Integer, List<Integer>> currentNodesPerCommunity, int nextEventTimeDiff) {
		this.currentNodesPerCommunity = currentNodesPerCommunity;
		this.nextEventsTime += 0.1;
		this.nextEventTimeDiff = nextEventTimeDiff;
		for (Map.Entry<Integer, List<Integer>> labelNodes : this.currentNodesPerCommunity.entrySet()) {
			this.sizeCommunities.put(labelNodes.getKey(), labelNodes.getValue().size());			
		}
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		
		Map.Entry<Integer, List<Integer>> labelNodes = this.currentNodesPerCommunity.entrySet().iterator().next();
		if(labelNodes.getValue().size() <= 1) {
			this.currentNodesPerCommunity.remove(labelNodes.getKey());
			if (this.currentNodesPerCommunity.size() == 0) {
				this.nextEventsTime += nextEventTimeDiff;
				return new ExternalEvent(this.nextEventsTime);
			}
			labelNodes = this.currentNodesPerCommunity.entrySet().iterator().next();
		}
		String id = getID();
		
		Random random = new Random();
		int indexFrom = random.nextInt(labelNodes.getValue().size());
		int from = labelNodes.getValue().remove(indexFrom);

		int indexTo = random.nextInt(labelNodes.getValue().size());
		int to = labelNodes.getValue().remove(indexTo);
		
		labelNodes.getValue().add(from);
		
		MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
				drawMessageSize(), responseSize, this.nextEventsTime);
		
		return mce;
	}

}