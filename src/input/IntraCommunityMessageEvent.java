package input;

import java.util.ArrayList;
import java.util.List;

import core.Settings;
import core.SimClock;


public class IntraCommunityMessageEvent extends MessageEventGenerator {
	private int from;
	private int to;
	
	private List<Integer> firstFroms = new ArrayList<Integer>();
	public List<String> pair = new ArrayList<String>();

	
	public IntraCommunityMessageEvent(Settings s) {
		super(s);
	}
	
	public void setFirstFroms(List<Integer> firstFroms) {
		this.firstFroms = firstFroms;
	}
	
	public boolean isThisAFirstFromCandidate(int hostAddress) {
		return this.firstFroms.indexOf(hostAddress) > 0;
	}
	
	public void setFirstMsgEventInfo(int from, int to) {
		int indexFrom = this.firstFroms.indexOf(from);
		if (indexFrom > 0) {
			this.from = this.firstFroms.remove(indexFrom);
			this.to = to;
			this.nextEventsTime = SimClock.getTime() + 0.01;
		}
	}
	
	/** 
	 * Returns the next message creation event
	 * @see input.EventQueue#nextEvent()
	 */
	public ExternalEvent nextEvent() {
		int responseSize = 0;
		String id = getID();
		
		String fromTo = from + "_" + to;
		
		if (pair.contains(fromTo)) {
			this.nextEventsTime += drawNextEventTimeDiff();
			return new ExternalEvent(this.nextEventsTime);
		}
		
		MessageCreateEvent mce = new MessageCreateEvent(from, to, id, 
					drawMessageSize(), responseSize, this.nextEventsTime);
			
		this.nextEventsTime += drawNextEventTimeDiff();
		pair.add(fromTo);
			
		return mce;
	}

}