package input;


import java.util.HashMap;
import java.util.Map;

import core.Settings;
import core.SimClock;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	private int from;
	private int to;
	public Map<String, Boolean> sentFromTo = new HashMap<String, Boolean>();
	
	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setNextMsgEventInfo(int from, int to) {
		this.from = from;
		this.to = to;
		this.nextEventsTime = SimClock.getTime() + 0.01;
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {		
		String pair = from+"_"+to;
		
		boolean alreadySentFromTo = this.sentFromTo.getOrDefault(pair, false);
		
		if (!alreadySentFromTo) {
			int responseSize = 0;
			
			String id = getID();
			
			MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
					drawMessageSize(), responseSize, this.nextEventsTime);
			
			this.nextEventsTime += drawNextEventTimeDiff();
			
			this.sentFromTo.put(pair, true);
			
			return mce;
		}
		this.nextEventsTime += drawNextEventTimeDiff();
		
		return new ExternalEvent(this.nextEventsTime);
	}

}