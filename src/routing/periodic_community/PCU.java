package routing.periodic_community;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;

public abstract class PCU{
	
	protected int label = 5000;
	protected Map<Integer, List<Integer>> nodesPerCommunity = new HashMap<Integer, List<Integer>>();

	protected Map<Integer, Integer> contactsWithCommunity = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> interCommunityContact = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> intraCommunityContact = new HashMap<Integer, Integer>();
	protected Map<String, Integer> deliveredMessages = new HashMap<String, Integer>();
	
	protected int intraContacts = 0;
	protected int interContacts = 0;

	public PCU(Settings settings) {}
	
	public PCU(PCU pcRouter) {}
		
	public PCU getDecisionEngineFromHost(DTNHost host) {
		MessageRouter router = host.getRouter();
		assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
		
		return (PCU) ((DecisionEngineRouter)router).getDecisionEngine();
	}
	
	public void updateContacts(DTNHost thisHost, DTNHost peer) {
		PCU thisRouter = getDecisionEngineFromHost(thisHost);
		PCU peerRouter = getDecisionEngineFromHost(peer);
					
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
	
	public void updateBuffer(PCU router) {
		router.deliveredMessages.putAll(deliveredMessages);
	}
	
	public int getLabel() {
		return label;
	}
	
	public void setLabel(int communityLabel) {
		this.label = communityLabel;
	}

	public Map<Integer, List<Integer>> getNodesPerCommunity() {
		return nodesPerCommunity;
	}

	public void setNodesPerCommunity(Map<Integer, List<Integer>> nodesPerCommunity) {
		this.nodesPerCommunity = new HashMap<Integer, List<Integer>>();
		this.nodesPerCommunity.putAll(nodesPerCommunity);
	}
	
	public void clearCounters() {
		contactsWithCommunity.clear();
		interCommunityContact.clear();
		intraCommunityContact.clear();
		intraContacts = 0;
		interContacts = 0;	
	}
}