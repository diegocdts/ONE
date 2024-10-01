package input;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import core.Settings;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	private Map<Integer, List<Integer>> currentNodesPerCommunity;
	private int nextEventTimeDiff;
	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setNextMsgEventInfo(Map<Integer, List<Integer>> currentNodesPerCommunity, int nextEventTimeDiff) {
		this.currentNodesPerCommunity = currentNodesPerCommunity;
		this.nextEventsTime += 0.1;
		this.nextEventTimeDiff = nextEventTimeDiff;
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
		
		Map.Entry<Integer, List<Integer>> labelNodes = this.currentNodesPerCommunity.entrySet().iterator().next();
		if(labelNodes.getValue().size() == 1) {
			this.currentNodesPerCommunity.remove(labelNodes.getKey());
			if (this.currentNodesPerCommunity.size() == 0) {
				this.nextEventsTime += nextEventTimeDiff;
				return new ExternalEvent(this.nextEventsTime);
			}
			labelNodes = this.currentNodesPerCommunity.entrySet().iterator().next();
		}
		Collections.sort(labelNodes.getValue());
		int from = Collections.min(labelNodes.getValue());
		int to = labelNodes.getValue().remove(1);
		String id = getID();
		
		MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
				drawMessageSize(), responseSize, this.nextEventsTime);
		
		return mce;
	}

}