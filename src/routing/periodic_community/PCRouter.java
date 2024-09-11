package routing.periodic_community;

import java.util.HashMap;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PCRouter implements RoutingDecisionEngine{
	
	private int label = 5000;
	private Map<Integer, Integer> nodesPerCommunity = new HashMap<Integer, Integer>(); //key: community label, value: number of nodes inside it
	private Map<Integer, Integer> timesInCommunityWith = new HashMap<Integer, Integer>();	//key: node address, value: n of times they were in the same community
	private double meanNodesPerCommunity = -1;

	private Map<Integer, Integer> contactsWithCommunity = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> interCommunityContact = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> intraCommunityContact = new HashMap<Integer, Integer>();
	private Map<String, Integer> deliveredMessages = new HashMap<String, Integer>();
	
	private int intraContacts = 0;
	private int interContacts = 0;

	public PCRouter(Settings settings) {}
	
	public PCRouter(PCRouter pcRouter) {}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		PCRouter otherRouter = getDecisionEngineFromHost(peer);
		updateContacts(thisHost, peer);
		updateBuffer(otherRouter);
		otherRouter.updateContacts(peer, thisHost);
		otherRouter.updateBuffer(this);
	}
	
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {}
	
	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {}
	
	@Override
	public boolean newMessage(Message m) {
		return true;
	}
	
	@Override
	public boolean isFinalDest(Message m, DTNHost aHost) {
		if (m.getTo() == aHost) {
			PCRouter router = this.getDecisionEngineFromHost(aHost);
			router.deliveredMessages.put(m.getId(), 0);
			this.deliveredMessages.put(m.getId(), 0);
		}
		return m.getTo() == aHost;
	}
	
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		MessageRouter router = thisHost.getRouter();
		return m.getTo() != thisHost && !router.hasMessage(m.getId()) && !deliveredMessages.containsKey(m.getId());
	}
	
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

		DTNHost destiny = m.getTo();
		PCRouter destinyRouter = this.getDecisionEngineFromHost(destiny);
		PCRouter otherRouter = this.getDecisionEngineFromHost(otherHost);
		MessageRouter mRouter = otherHost.getRouter();
		
		if(m.getTo() == otherHost && !otherRouter.deliveredMessages.containsKey(m.getId())) return true;
		
		if(mRouter.hasMessage(m.getId()) || deliveredMessages.containsKey(m.getId()) || otherRouter.deliveredMessages.containsKey(m.getId())) 
			return false;
		
		//===============================================================================================================
		int destCommunitySize = this.getNodesPerCommunity().get(this.getLabel());
		int otherCommunitySize = this.getNodesPerCommunity().get(otherRouter.getLabel());
		
		//===============================================================================================================
		int thisIntraContactsWithDest = intraCommunityContact.getOrDefault(destiny.getAddress(), 0);
		int otherIntraContactsWithDest = otherRouter.intraCommunityContact.getOrDefault(destiny.getAddress(), 0);
				
		//===============================================================================================================
		int thisInterContactsWithDest = interCommunityContact.getOrDefault(destiny.getAddress(), 0);
		int otherInterContactsWithDest = otherRouter.interCommunityContact.getOrDefault(destiny.getAddress(), 0);
		
		//===============================================================================================================
		int thisContactsWithDestComm = contactsWithCommunity.getOrDefault(destinyRouter.getLabel(), 0);
		int otherContactsWithDestComm = otherRouter.contactsWithCommunity.getOrDefault(destinyRouter.getLabel(), 0);
				
		//===============================================================================================================
		int thisInCommunityWithDest = timesInCommunityWith.getOrDefault(destiny.getAddress(), 0);
		int otherInCommunityWithDest = otherRouter.timesInCommunityWith.getOrDefault(destiny.getAddress(), 0);
		
		if (this.getLabel() == destinyRouter.getLabel()) {
			if (otherRouter.getLabel() == destinyRouter.getLabel()) {
				boolean cond1 = otherIntraContactsWithDest > thisIntraContactsWithDest;
				boolean cond2 = otherRouter.intraContacts > this.intraContacts;
				boolean cond3 = otherRouter.intraCommunityContact.size() > intraCommunityContact.size();
				boolean cond4 = destCommunitySize < meanNodesPerCommunity;
				if (cond1 && cond2	&& cond3) {
					return true;
				}
				else if((cond1 && cond2) || (cond1 && cond3) || (cond2 && cond3)) {
					return true;
				}
				else if (cond1 || cond2 || cond3) {
					return true;
				}
				else if (cond4) {
					return true;
				}
			}
		}
		else {
			boolean cond1 = otherRouter.interContacts > this.interContacts;
			boolean cond2 = otherRouter.contactsWithCommunity.size() > this.contactsWithCommunity.size();
			boolean cond3 = otherContactsWithDestComm > thisContactsWithDestComm;
			boolean cond4 = otherInterContactsWithDest > thisInterContactsWithDest;
			boolean cond5 = otherRouter.interCommunityContact.size() > interCommunityContact.size();
			
			if (otherRouter.getLabel() == destinyRouter.getLabel()) {
				return true;
			}
			else if (cond1 && cond2 && cond3 && cond4 && cond5) {
				return true;
			}
			else if ((cond1 && cond2 && cond3 && cond4) 
					|| (cond1 && cond2 && cond3 && cond5) || (cond1 && cond2 && cond4 && cond5)
					|| (cond1 && cond3 && cond4 && cond5) || (cond2 && cond3 && cond4 && cond5)) {
				return true;
			}
			else if ((cond1 && cond2 && cond3) 
					|| (cond1 && cond2 && cond4) || (cond1 && cond2 && cond5) 
					|| (cond1 && cond3 && cond4) || (cond1 && cond3 && cond5) 
					|| (cond1 && cond4 && cond5) || (cond2 && cond3 && cond4)
					|| (cond2 && cond3 && cond5) || (cond2 && cond4 && cond5)
					|| (cond3 && cond4 && cond5)) {
				return true;
			}
			else if((cond1 && cond2) 
					|| (cond1 && cond3) || (cond1 && cond4) 
					|| (cond1 && cond5) || (cond2 && cond3) 
					|| (cond2 && cond4) || (cond2 && cond5) 
					|| (cond3 && cond4) || (cond3 && cond5)
					|| (cond4 && cond5)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		return m != null ? this.deliveredMessages.containsKey(m.getId()) : false;
	}
	
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		return true;
	}
	
	@Override
	public RoutingDecisionEngine replicate() {
		return new PCRouter(this);
	}
	
	public PCRouter getDecisionEngineFromHost(DTNHost host) {
		MessageRouter router = host.getRouter();
		assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
		
		return (PCRouter) ((DecisionEngineRouter)router).getDecisionEngine();
	}
	
	private void updateContacts(DTNHost thisHost, DTNHost peer) {
		PCRouter thisRouter = getDecisionEngineFromHost(thisHost);
		PCRouter peerRouter = getDecisionEngineFromHost(peer);
					
		if (thisRouter.getLabel() != peerRouter.getLabel()) {
			contactsWithCommunity.merge(peerRouter.getLabel(), 1, Integer::sum);
			interCommunityContact.merge(peer.getAddress(), 1, Integer::sum);
			interContacts += 1;
		}
		else {
			intraCommunityContact.merge(peer.getAddress(), 1, Integer::sum);
			intraContacts += 1;
		}
	}
	
	public void updateBuffer(PCRouter router) {
		router.deliveredMessages.putAll(deliveredMessages);
	}

	public int getLabel() {
		return label;
	}
	

	public void setLabel(int communityLabel) {
		this.label = communityLabel;
	}

	public Map<Integer, Integer> getNodesPerCommunity() {
		return nodesPerCommunity;
	}

	public void setNodesPerCommunity(Map<Integer, Integer> nodesPerCommunity) {
		this.nodesPerCommunity = new HashMap<Integer, Integer>();
		this.nodesPerCommunity.putAll(nodesPerCommunity);
	}

	public Map<Integer, Integer> getTimesInCommunityWith() {
		return timesInCommunityWith;
	}

	public void setTimesInCommunityWith(Map<Integer, Integer> timesInCommunityWith) {
		this.timesInCommunityWith.putAll(timesInCommunityWith);
	}
	
	public void setMeanNoderPerCommunity(double meanNoderPerCommunity) {
		this.meanNodesPerCommunity = meanNoderPerCommunity;
	}
	
	public void clearCounters() {
		contactsWithCommunity.clear();
		interCommunityContact.clear();
		intraCommunityContact.clear();
		intraContacts = 0;
		interContacts = 0;	
	}
}