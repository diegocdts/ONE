package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class IFPCURouter extends PCU implements RoutingDecisionEngine{
					
	public IFPCURouter(Settings settings) {
		super(settings);
	}
	
	public IFPCURouter(IFPCURouter pcuIntraRouter) {
		super(pcuIntraRouter);
	}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		PCU otherRouter = getDecisionEngineFromHost(peer);
		updateContacts(thisHost, peer);
		otherRouter.updateContacts(peer, thisHost);
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
		return m.getTo().getAddress() == aHost.getAddress();
	}
	
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		return true;
	}
	
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

		DTNHost destiny = m.getTo();
		PCU destinyRouter = this.getDecisionEngineFromHost(destiny);
		IFPCURouter otherRouter = getDecisionEngineFromHost(otherHost);
		MessageRouter mRouter = otherHost.getRouter();
		
		if (mRouter.hasMessage(m.getId())) return false;
						
		if (destinyRouter.getLabel() == otherRouter.getLabel()) {
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
		
		return false;
	}
	
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		return false;
	}
	
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		return false;
	}
	
	@Override
	public RoutingDecisionEngine replicate() {
		return new IFPCURouter(this);
	}
	
	public IFPCURouter getDecisionEngineFromHost(DTNHost host) {
		MessageRouter router = host.getRouter();
		assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
		
		return (IFPCURouter) ((DecisionEngineRouter)router).getDecisionEngine();
	}
}