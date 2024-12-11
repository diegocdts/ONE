
package routing.community;

import java.util.*;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;


public class EpSoc implements RoutingDecisionEngine {
	public static final String CENTRALITY_ALG_SETTING = "centralityAlg";
	
	protected Map<DTNHost, Double> startTimestamps;
	protected Map<DTNHost, List<Duration>> connHistory;
	protected Map<String, Integer> blockRegister = new HashMap<String, Integer>();
	
	protected Centrality centrality;
	
	
	public EpSoc(Settings s)
	{
		if(s.contains(CENTRALITY_ALG_SETTING))
			this.centrality = (Centrality) 
				s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
		else
			this.centrality = new CWindowCentrality(s);
	}
	
	
	public EpSoc(EpSoc proto)
	{
		this.centrality = proto.centrality.replicate();
		startTimestamps = new HashMap<DTNHost, Double>();
		connHistory = new HashMap<DTNHost, List<Duration>>();
	}

	public void connectionUp(DTNHost thisHost, DTNHost peer){}

	
	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{
		DTNHost myHost = con.getOtherNode(peer);
		EpSoc de = this.getOtherDecisionEngine(peer);
		
		this.startTimestamps.put(peer, SimClock.getTime());
		de.startTimestamps.put(myHost, SimClock.getTime());		
	}
	
	public void connectionDown(DTNHost thisHost, DTNHost peer)
	{
		double time = startTimestamps.get(peer);
		double etime = SimClock.getTime();
		
		// Find or create the connection history list
		List<Duration> history;
		if(!connHistory.containsKey(peer))
		{
			history = new LinkedList<Duration>();
			connHistory.put(peer, history);
		}
		else
			history = connHistory.get(peer);
		
		// add this connection to the list
		if(etime - time > 0)
			history.add(new Duration(time, etime));
		
		startTimestamps.remove(peer);
	}

	public boolean newMessage(Message m)
	{
		return true; // Always keep and attempt to forward a created message
	}

	public boolean isFinalDest(Message m, DTNHost aHost)
	{
		return m.getTo() == aHost; // Unicast Routing
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		MessageRouter mRouter = thisHost.getRouter();
		
		if (mRouter.hasMessage(m.getId()) || blockRegister.containsKey(m.getId())) return false;
		
		double nodeCentrality = getGlobalCentrality();
		if(nodeCentrality > m.previousCentrality) {
			if (nodeCentrality < 1) {
				nodeCentrality += 1;
			}
			double newTtl = m.getInitTtl() / nodeCentrality;
			m.setTtl((int)(newTtl));
			blockRegister.put(m.getId(), 0);
		}
		return m.getTo() != thisHost;
	}

	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost)
	{
		EpSoc de = getOtherDecisionEngine(otherHost);
		MessageRouter mRouter = otherHost.getRouter();
		
		if (mRouter.hasMessage(m.getId()) || de.blockRegister.containsKey(m.getId())) return false;
				
		m.previousCentrality = getGlobalCentrality();
		return true;
	}

	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost)
	{
		return false;
	}

	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld)
	{
		blockRegister.put(m.getId(), 0);
		return true;
	}

	public RoutingDecisionEngine replicate()
	{
		return new EpSoc(this);
	}
	
	protected double getGlobalCentrality()
	{
		return this.centrality.getGlobalCentrality(connHistory);
	}

	private EpSoc getOtherDecisionEngine(DTNHost h)
	{
		MessageRouter otherRouter = h.getRouter();
		assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";
		
		return (EpSoc) ((DecisionEngineRouter)otherRouter).getDecisionEngine();
	}
}
