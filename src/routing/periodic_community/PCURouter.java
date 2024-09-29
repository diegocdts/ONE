package routing.periodic_community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PCURouter extends PCU implements RoutingDecisionEngine{

	public PCURouter(Settings settings) {
		super(settings);
	}
	
	public PCURouter(PCURouter pcuRouter) {
		super(pcuRouter);
	}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		PCURouter otherRouter = getDecisionEngineFromHost(peer);
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
			PCURouter router = this.getDecisionEngineFromHost(aHost);
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
		PCURouter destinyRouter = this.getDecisionEngineFromHost(destiny);
		PCURouter otherRouter = this.getDecisionEngineFromHost(otherHost);
		MessageRouter mRouter = otherHost.getRouter();
		
		if(m.getTo() == otherHost && !otherRouter.deliveredMessages.containsKey(m.getId())) return true;
		
		if(mRouter.hasMessage(m.getId()) || deliveredMessages.containsKey(m.getId()) || otherRouter.deliveredMessages.containsKey(m.getId())) 
			return false;

		if (this.getLabel() == destinyRouter.getLabel()) {
			if (otherRouter.getLabel() == destinyRouter.getLabel()) {
				int thisIntraContactsWithDest = intraCommunityContact.getOrDefault(destiny.getAddress(), 0);
				int otherIntraContactsWithDest = otherRouter.intraCommunityContact.getOrDefault(destiny.getAddress(), 0);
				
				boolean cond1 = otherIntraContactsWithDest > thisIntraContactsWithDest;
				boolean cond2 = otherRouter.intraContacts > this.intraContacts;
				boolean cond3 = otherRouter.intraCommunityContact.size() > intraCommunityContact.size();
				int check = (cond1? 1 : 0) + (cond2? 1 : 0) + (cond3? 1 : 0);
				
				if (check >= 1) {
					return true;
				}
			}
		}
		else {			
			if (otherRouter.getLabel() == destinyRouter.getLabel()) {
				return true;
			}
			else {
				int thisInterContactsWithDest = interCommunityContact.getOrDefault(destiny.getAddress(), 0);
				int otherInterContactsWithDest = otherRouter.interCommunityContact.getOrDefault(destiny.getAddress(), 0);
				
				int thisContactsWithDestComm = contactsWithCommunity.getOrDefault(destinyRouter.getLabel(), 0);
				int otherContactsWithDestComm = otherRouter.contactsWithCommunity.getOrDefault(destinyRouter.getLabel(), 0);
				
				boolean cond1 = otherRouter.interContacts > this.interContacts;
				boolean cond2 = otherRouter.contactsWithCommunity.size() > this.contactsWithCommunity.size();
				boolean cond3 = otherContactsWithDestComm > thisContactsWithDestComm;
				boolean cond4 = otherInterContactsWithDest > thisInterContactsWithDest;
				boolean cond5 = otherRouter.interCommunityContact.size() > interCommunityContact.size();
				int check = (cond1? 1 : 0) + (cond2? 1 : 0) + (cond3? 1 : 0) + (cond4? 1 : 0) + (cond5? 1 : 0);
				
				if (check >= 2) {
					return true;
				}
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
		return new PCURouter(this);
	}
	
	public PCURouter getDecisionEngineFromHost(DTNHost host) {
		MessageRouter router = host.getRouter();
		assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
		
		return (PCURouter) ((DecisionEngineRouter)router).getDecisionEngine();
	}
}