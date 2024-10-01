package input;

import java.util.List;

import core.Settings;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	private int from;
	private List<Integer> toList;
	private int nextEventTimeDiff;
	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setNextMsgEventInfo(int from, List<Integer> toList, int nextEventTimeDiff) {
		this.from = from;
		this.toList = toList;
		this.nextEventsTime += 1;
		this.nextEventTimeDiff = nextEventTimeDiff;
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0; /* no responses requested */
				
		if (this.toList.size() == 0) {
			this.nextEventsTime += nextEventTimeDiff;
			return new ExternalEvent(this.nextEventsTime);
		}
		
		int to = toList.remove(0);
		String id = getID();
				
		MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
				drawMessageSize(), responseSize, this.nextEventsTime);
		
		return mce;
	}

}