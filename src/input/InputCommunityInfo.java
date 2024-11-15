package input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import core.Settings;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.periodic_community.PCU;


public class InputCommunityInfo {
	
	String helsinki = "/home/diegocdts/PycharmProjects/FLPUCI-Datasets/helsinki/f9_results/FL-based/FED_PROX_1.0/SLI_community_info/community_id_maps/";
	
	String manhattan = "/home/diegocdts/PycharmProjects/FLPUCI-Datasets/manhattan/f9_results/FL-based/FED_PROX_1.0/SLI_community_info/community_id_maps/";

	String sfc = "/home/diegocdts/PycharmProjects/FLPUCI-Datasets/sanfranciscocabs/f9_results/FL-based/FED_PROX_1.0/SLI_community_info/community_id_maps/";

	String rt = "/home/diegocdts/PycharmProjects/FLPUCI-Datasets/romataxi/f9_results/FL-based/FED_PROX_1.0/SLI_community_info/community_id_maps/";
	
	int _4hours = 14400;
	int _40min = 2400;
	
	public int currentInterval = 0;
	public double currentThreshold = 0;
	public String rootIntervalLabels = getRoot();
	public int intervalSize = rootIntervalLabels == helsinki || rootIntervalLabels == manhattan? _40min : _4hours;
	public String pathIntervalLabels = "";
	public SimScenario scenario;
	public Map<Integer, Integer> currentCommunityIdMap;
	public Map<Integer, List<Integer>> currentNodesPerCommunity;
	public IntraCommunityMessageEvent intraCommunityMsgEvent;
	
	public InputCommunityInfo(SimScenario scenario, double simTime) {
		if (Settings.DEF_SETTINGS_FILE.contains("pcu")) {
			if(scenario.getExternalEvents().get(0) instanceof IntraCommunityMessageEvent) {
				this.intraCommunityMsgEvent = (IntraCommunityMessageEvent) scenario.getExternalEvents().get(0);
			}
			System.out.println(rootIntervalLabels + " " + intervalSize);
			this.scenario = scenario;
			String fileName = "interval_X.txt".replace("X", String.valueOf(this.currentInterval));
			
			String pathIntervalLabels = this.rootIntervalLabels + fileName;
			this.pathIntervalLabels = pathIntervalLabels;
						
			loadCommunityLabels();
			setRouterInfo();
		}		
	}
	
	public void intervalChange(double simTime) {
		if (Settings.DEF_SETTINGS_FILE.contains("pcu")) {
			if (this.currentThreshold + this.intervalSize <= simTime) {
				this.currentThreshold += this.intervalSize;
				this.currentInterval += 1;
				String fileName = "interval_X.txt".replace("X", String.valueOf(this.currentInterval));
				
				String pathIntervalLabels = this.rootIntervalLabels + fileName;
				this.pathIntervalLabels = pathIntervalLabels;
				
				loadCommunityLabels();
				setRouterInfo();
			} 
		}
	}
	
	public void loadCommunityLabels() {
        Map<Integer, Integer> nodeLabelMap = new HashMap<>();
        Map<Integer, List<Integer>> nodesPerCommunity = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(this.pathIntervalLabels))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                int label = Integer.parseInt(values[0]);
                int node = Integer.valueOf(values[1]);
                if (!nodesPerCommunity.containsKey(label)) {
                	nodesPerCommunity.put(label, new ArrayList<Integer>());
                }
                nodesPerCommunity.get(label).add(node);
                nodeLabelMap.put(node, label);
            }
        } catch (IOException e) {
            System.out.println("File "+this.pathIntervalLabels+" does not exist.");
        }

        this.currentCommunityIdMap = nodeLabelMap;
        this.currentNodesPerCommunity = nodesPerCommunity;
	}
		
	public void setRouterInfo() {
		if (Settings.DEF_SETTINGS_FILE.contains("pcu") && !Settings.DEF_SETTINGS_FILE.contains("epidemic")) {
			scenario.getHosts().forEach(host ->
	        {
        		MessageRouter mRouter = host.getRouter();
        		PCU router = (PCU) ((DecisionEngineRouter)mRouter).getDecisionEngine();
	    		router.clearCounters();
	    		if (this.currentCommunityIdMap.containsKey(host.getAddress())) {
	        		int label = this.currentCommunityIdMap.get(host.getAddress());
	        		router.setLabel(label);
	        		router.setNodesPerCommunity(this.currentNodesPerCommunity);
	        	}
	    		if (this.intraCommunityMsgEvent != null) {
	    			router.receivedMsg = false;
	    		}
	        });
		}
		if (this.intraCommunityMsgEvent != null) {
			createFirstIntraMessage();
		}
	}
	
	public void createFirstIntraMessage() {
		this.intraCommunityMsgEvent.setCommunities(this.currentNodesPerCommunity);
	}
	
	public String getRoot() {
		if (Settings.DEF_SETTINGS_FILE.contains("helsinki")) return helsinki;
		else if (Settings.DEF_SETTINGS_FILE.contains("manhattan")) return manhattan;
		else if (Settings.DEF_SETTINGS_FILE.contains("rt")) return rt;
		else return sfc;
	}
}
