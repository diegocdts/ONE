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
	
	private int communityLabel = 5000;
	private Map<Integer, Integer> nodesPerCommunity = new HashMap<Integer, Integer>(); //key: community label, value: number of nodes inside it
	private Map<Integer, Integer> previousCount = new HashMap<Integer, Integer>();	//key: node address, value: n of times they were in the same community
	public int meanNoderPerCommunity = -1;

	public Map<Integer, Integer> interCommunityContact = new HashMap<Integer, Integer>();
	public Map<Integer, Integer> intraCommunityContact = new HashMap<Integer, Integer>();
	public Map<Integer, Integer> anyContact = new HashMap<Integer, Integer>();
	public Map<String, Integer> deliveredMessages = new HashMap<String, Integer>();
	
	public PCRouter(Settings settings) {}
	
	public PCRouter(PCRouter pcRouter) {}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		updateBuffer(peer);
	}
	
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {
		updateContacts(thisHost, peer);
	}
	
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
		
		if(m.getTo() == otherHost && !otherRouter.deliveredMessages.containsKey(m.getId())) return true;
		
		if(deliveredMessages.containsKey(m.getId())) return false;
				
		int destCommunityLabel = destinyRouter.getCommunityLabel();
		boolean isThisInsideDestCommunity = this.getCommunityLabel() == destCommunityLabel;
		boolean isOtherInsideDestCommunity = otherRouter.getCommunityLabel() == destCommunityLabel;
				
		int thisIntraContacts = this.intraCommunityContact.size();
		int otherIntraContacts = otherRouter.intraCommunityContact.size();
		
		int thisAnyContactsWithDest = anyContact.containsKey(destiny.getAddress()) ?
				anyContact.get(destiny.getAddress()) : 0;
		int otherAnyContactsWithDest = otherRouter.anyContact.containsKey(destiny.getAddress()) ?
				otherRouter.anyContact.get(destiny.getAddress()) : 0;
		
		int nodesInDestCommunity = this.getNodesPerCommunity().get(destCommunityLabel);
		boolean isDestCommunitySmall = nodesInDestCommunity < this.meanNoderPerCommunity * 0.25;
		
		//===============================================================================================================
		int thisIntraContactsWithDest = intraCommunityContact.containsKey(destiny.getAddress()) ?
				intraCommunityContact.get(destiny.getAddress()) : 0;
		int otherIntraContactsWithDest = otherRouter.intraCommunityContact.containsKey(destiny.getAddress()) ?
				otherRouter.intraCommunityContact.get(destiny.getAddress()) : 0;
		
		//===============================================================================================================
		int thisContactsWithOtherCommunities = interCommunityContact.size();
		int otherContactsWithOtherCommunities = otherRouter.interCommunityContact.size();
		
		int thisContactsWithDestCommunity = interCommunityContact.containsKey(destCommunityLabel) ?
				interCommunityContact.get(destCommunityLabel) : 0;
		int otherContactsWithDestCommunity = otherRouter.interCommunityContact.containsKey(destCommunityLabel) ?
				otherRouter.interCommunityContact.get(destCommunityLabel) : 0;
		
		int thisTimesInSameCommunityWithDest = previousCount.containsKey(destiny.getAddress()) ?
				previousCount.get(destiny.getAddress()) : 0;
		int otherTimesInSameCommunityWithDest = otherRouter.previousCount.containsKey(destiny.getAddress()) ?
				otherRouter.previousCount.get(destiny.getAddress()) : 0;
				
		if (isThisInsideDestCommunity) {
			
			if (isOtherInsideDestCommunity) {
//				if (otherIntraContactsWithDest > thisIntraContactsWithDest 
//						|| otherIntraContacts > thisIntraContacts
//						|| otherAnyContactsWithDest > thisAnyContactsWithDest
//						|| otherTimesInSameCommunityWithDest > thisTimesInSameCommunityWithDest
//						|| isDestCommunitySmall) {
//					return true;
//				}
				if (otherIntraContactsWithDest > thisIntraContactsWithDest) {
					return true;
				}
				else if (otherIntraContacts > 1) {
					return true;
				}
				else if (isDestCommunitySmall && otherAnyContactsWithDest > 0) {
					return true;
				}
			}
		}
		else {
			if (isOtherInsideDestCommunity && (otherIntraContactsWithDest >= 1 || otherIntraContacts > 1)) {
				return true;
			}
			else if (otherContactsWithDestCommunity > thisContactsWithDestCommunity) {
				return true;				
			}
			else if ((thisContactsWithOtherCommunities == 0 && otherContactsWithOtherCommunities > 1)
					|| (thisContactsWithOtherCommunities > 0 && otherContactsWithOtherCommunities > thisContactsWithOtherCommunities)) {
				return true;
			}
			else if (otherTimesInSameCommunityWithDest > thisTimesInSameCommunityWithDest) {
				return true;
			}
			else if (otherAnyContactsWithDest > thisAnyContactsWithDest) {
				return true;
			}
			else if (isDestCommunitySmall) {
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
		assert router instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";
		
		return (PCRouter) ((DecisionEngineRouter)router).getDecisionEngine();
	}
	
	private void updateContacts(DTNHost thisHost, DTNHost peer) {
		PCRouter thisRouter = getDecisionEngineFromHost(thisHost);
		PCRouter peerRouter = getDecisionEngineFromHost(peer);
		if (anyContact.containsKey(peer.getAddress())) {
			int current = anyContact.get(peer.getAddress());
			anyContact.put(peer.getAddress(), current + 1);
		}
		else {
			anyContact.put(peer.getAddress(), 1);
		}
					
		if (thisRouter.getCommunityLabel() != peerRouter.getCommunityLabel()) {

			if (interCommunityContact.containsKey(peerRouter.getCommunityLabel())) {
				int currentCount = interCommunityContact.get(peerRouter.getCommunityLabel());
				interCommunityContact.put(peerRouter.getCommunityLabel(), currentCount + 1);
			}
			else {
				interCommunityContact.put(peerRouter.getCommunityLabel(), 1);
			}
		}
		else {
			if (intraCommunityContact.containsKey(peer.getAddress())) {
				int currentCount = intraCommunityContact.get(peer.getAddress());
				intraCommunityContact.put(peer.getAddress(), currentCount + 1);
			}
			else {
				intraCommunityContact.put(peer.getAddress(), 1);
			}
		}
	}
	
	public void updateBuffer(DTNHost peer) {
		PCRouter router = getDecisionEngineFromHost(peer);
		for (String id: deliveredMessages.keySet()) {
			router.deliveredMessages.put(id, 0);
		}
	}

	public int getCommunityLabel() {
		return communityLabel;
	}
	

	public void setCommunityLabel(int communityLabel) {
		this.communityLabel = communityLabel;
	}

	public Map<Integer, Integer> getNodesPerCommunity() {
		return nodesPerCommunity;
	}

	public void setNodesPerCommunity(Map<Integer, Integer> nodesPerCommunity) {
		for(Map.Entry<Integer, Integer> entry: nodesPerCommunity.entrySet()) {
			this.nodesPerCommunity.put(entry.getKey(), entry.getValue());
		}
	}

	public Map<Integer, Integer> getPreviousCount() {
		return previousCount;
	}

	public void setPreviousCount(Map<Integer, Integer> previousCount) {
		for(Map.Entry<Integer, Integer> entry: previousCount.entrySet()) {
			this.previousCount.put(entry.getKey(), entry.getValue());
		}
	}
}