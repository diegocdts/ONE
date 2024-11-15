package input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Settings;
import core.SimClock;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	public int from;
	public int to;
	public Random rnd = new Random();
	
	private Map<Integer, List<Integer>> currentNodesPerCommunity = new HashMap<Integer, List<Integer>>();
	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setCommunities(Map<Integer, List<Integer>> currentNodesPerCommunity) {
		rnd.setSeed(3);
		this.currentNodesPerCommunity.putAll(currentNodesPerCommunity);
		this.nextEventsTime = SimClock.getTime();
		if (currentNodesPerCommunity.size() > 0) {
			Map.Entry<Integer, List<Integer>> firstEntry = currentNodesPerCommunity.entrySet().iterator().next();
			this.from = firstEntry.getValue().remove(rnd.nextInt(firstEntry.getValue().size()));
		}
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		if (currentNodesPerCommunity.size() > 0) {
			Map.Entry<Integer, List<Integer>> firstEntry = currentNodesPerCommunity.entrySet().iterator().next();
			if (firstEntry.getValue().size() >= 2) {
				this.to = firstEntry.getValue().removeFirst();
			}
			else {
				currentNodesPerCommunity.remove(firstEntry.getKey());
				if (currentNodesPerCommunity.size() > 0) {
					firstEntry = currentNodesPerCommunity.entrySet().iterator().next();
					this.from = firstEntry.getValue().remove(rnd.nextInt(firstEntry.getValue().size()));
				}
				return new ExternalEvent(Double.MAX_VALUE);
			}
			this.nextEventsTime = SimClock.getTime();
		}
		else {
			this.nextEventsTime = Double.MAX_VALUE;
			return new ExternalEvent(Double.MAX_VALUE);
		}
		int responseSize = 0;
		String id = getID();
		System.out.println(from+" "+to);
				
		MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
					drawMessageSize(), responseSize, this.nextEventsTime);
						
		return mce;
	}

}