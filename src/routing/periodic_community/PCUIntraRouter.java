package routing.periodic_community;

import java.util.Collection;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PCUIntraRouter extends PCU implements RoutingDecisionEngine{
	
	boolean receivedTheMsg = false;
				
	public PCUIntraRouter(Settings settings) {
		super(settings);
	}
	
	public PCUIntraRouter(PCUIntraRouter pcuIntraRouter) {
		super(pcuIntraRouter);
	}
	
	public Collection<Message> messageCollection(DTNHost thisHost) {
		MessageRouter mThisRouter = thisHost.getRouter();
		Collection<Message> messageCollection = mThisRouter.getMessageCollection();
		return messageCollection;
	}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		PCUIntraRouter otherRouter = getDecisionEngineFromHost(peer);
		if (this.getLabel() == otherRouter.getLabel()) {
			if (this.msgEvent.isThisAFirstFromCandidate(thisHost.getAddress())) {
				int from = thisHost.getAddress();
				int to = peer.getAddress();
				this.msgEvent.setFirstMsgEventInfo(from, to);
			}
			else {
				Collection<Message> thisMessageCollection = messageCollection(thisHost);
				if (thisMessageCollection.size() > 0 && !otherRouter.receivedTheMsg) {
					if(thisMessageCollection.size() > 1) {
						System.out.println(thisHost.getAddress());
						for (Message m : thisMessageCollection) {
							System.out.println(m.getFrom().getAddress()+ " "+m.getTo().getAddress()+" "+m.getCreationTime()+" "+m.getTtl()+" "+m.initTtl);
						}
					}
					for (Message m : thisMessageCollection) {
						m.setTo(peer);
					}
				}
			}
		}
	}
	
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {}
	
	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {}
	
	@Override
	public boolean newMessage(Message m) {
		this.receivedTheMsg = true;
		return true;
	}
	
	@Override
	public boolean isFinalDest(Message m, DTNHost aHost) {
		boolean isDest = m.getTo() == aHost;
		if(isDest) {
			MessageRouter mThisRouter = aHost.getRouter();
			mThisRouter.createCopyMessage(m.replicate());
		}
		this.receivedTheMsg = true;
		return m.getTo() == aHost;
	}
	
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		this.receivedTheMsg = true;
		return true;
	}
	
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

		DTNHost destiny = m.getTo();
		MessageRouter mOtherRouter = otherHost.getRouter();
		PCUIntraRouter otherRouter = getDecisionEngineFromHost(otherHost);

		
		if(otherRouter.receivedTheMsg || mOtherRouter.hasMessage(m.getId())) return false;
				
		if(destiny == otherHost) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		return false;
	}
	
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		return true;
	}
	
	@Override
	public RoutingDecisionEngine replicate() {
		return new PCUIntraRouter(this);
	}
	
	public PCUIntraRouter getDecisionEngineFromHost(DTNHost host) {
		MessageRouter router = host.getRouter();
		assert router instanceof DecisionEngineRouter : "This router only works with other routers of same type";
		
		return (PCUIntraRouter) ((DecisionEngineRouter)router).getDecisionEngine();
	}
}