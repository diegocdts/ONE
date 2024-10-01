package routing.periodic_community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

public class PCUIntraRouter extends PCU implements RoutingDecisionEngine{
	
	public PCUIntraRouter(Settings settings) {
		super(settings);
	}
	
	public PCUIntraRouter(PCUIntraRouter pcuIntraRouter) {
		super(pcuIntraRouter);
	}
	
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {}
	
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {}
	
	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {}
	
	@Override
	public boolean newMessage(Message m) {
		System.out.println(m.getFrom() + " " + m.getTo() + " " + m.getId());
		return true;
	}
	
	@Override
	public boolean isFinalDest(Message m, DTNHost aHost) {
		return m.getTo() == aHost;
	}
	
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		MessageRouter router = thisHost.getRouter();
		return m.getTo() != thisHost && !router.hasMessage(m.getId());
	}
	
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {

		DTNHost destiny = m.getTo();
		PCU destinyRouter = this.getDecisionEngineFromHost(destiny);
		PCU otherRouter = this.getDecisionEngineFromHost(otherHost);
		MessageRouter mRouter = otherHost.getRouter();
		
		if(m.getTo() == otherHost) return true;
		
		if(mRouter.hasMessage(m.getId())) return false;
				
		//===============================================================================================================

		if (otherRouter.getLabel() == destinyRouter.getLabel()) {
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