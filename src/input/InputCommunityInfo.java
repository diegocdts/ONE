package input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import core.Settings;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.periodic_community.PCRouter;


public class InputCommunityInfo {
	
	String[] helsinki = {"/home/diegocdts/PycharmProjects/FLPUCI-Datasets/helsinki/f9_results/FL-based/FED_AVG/SLI_community_info/community_id_maps/", 
			"/home/diegocdts/PycharmProjects/FLPUCI-Datasets/helsinki/f9_results/FL-based/FED_AVG/SLI_community_info/previous_community_count/"};
	
	String[] manhattan = {"/home/diegocdts/PycharmProjects/FLPUCI-Datasets/manhattan/f9_results/FL-based/FED_AVG/SLI_community_info/community_id_maps/", 
	"/home/diegocdts/PycharmProjects/FLPUCI-Datasets/manhattan/f9_results/FL-based/FED_AVG/SLI_community_info/previous_community_count/"};
	
	public int currentInterval = 0;
	public double currentThreshold = 1;
	public String rootIntervalLabels = manhattan[0];
	public String rootPreviousCommunityCount = manhattan[1];
	public double intervalSize = 2400;
	public String pathIntervalLabels = "";
	public String pathPreviousCommunityCount = "";
	public SimScenario scenario;
	public Map<Integer, Integer> currentCommunityIdMap;
	public Map<String, Integer> currentPreviousCountMap;
	public Map<Integer, Integer> currentNodesPerCommunity;
	
	public InputCommunityInfo(SimScenario scenario, double simTime) {
		if (Settings.DEF_SETTINGS_FILE.contains("pcrouter")) {
			
			this.scenario = scenario;
			String fileName = "interval_X.txt".replace("X", String.valueOf(this.currentInterval));
			
			String pathIntervalLabels = this.rootIntervalLabels + fileName;
			this.pathIntervalLabels = pathIntervalLabels;

			String pathPreviousCommunityCount = this.rootPreviousCommunityCount + fileName;
			this.pathPreviousCommunityCount = pathPreviousCommunityCount;
			
			loadCommunityLabels();
			loadPreviousCommunityCount();
			setRouterInfo();
		}		
	}
	
	public void intervalChange(double simTime) {
		if (Settings.DEF_SETTINGS_FILE.contains("pcrouter")) {
			if (this.currentThreshold + this.intervalSize <= simTime) {
				this.currentThreshold += this.intervalSize;
				this.currentInterval += 1;
				String fileName = "interval_X.txt".replace("X", String.valueOf(this.currentInterval));
				
				String pathIntervalLabels = this.rootIntervalLabels + fileName;
				this.pathIntervalLabels = pathIntervalLabels;
				
				String pathPreviousCommunityCount = this.rootPreviousCommunityCount + fileName;
				this.pathPreviousCommunityCount = pathPreviousCommunityCount;
				
				loadCommunityLabels();
				loadPreviousCommunityCount();
				setRouterInfo();
			} 
		}
	}
	
	public void loadCommunityLabels() {
        Map<Integer, Integer> nodeLabelMap = new HashMap<>();
        Map<Integer, Integer> nodesPerCommunity = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(this.pathIntervalLabels))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                int label = Integer.parseInt(values[0]);
                int node = Integer.valueOf(values[1]);
                if (nodesPerCommunity.containsKey(label)) {
                	nodesPerCommunity.put(label, nodesPerCommunity.get(label) + 1);
                }
                else {
                	nodesPerCommunity.put(label, 1);
                }
                nodeLabelMap.put(node, label);
            }
        } catch (IOException e) {
            System.out.println("File "+this.pathIntervalLabels+" does not exist.");
        }

        this.currentCommunityIdMap = nodeLabelMap;
        this.currentNodesPerCommunity = nodesPerCommunity;
	}
	
	public void loadPreviousCommunityCount() {
		Map<String, Integer> previousCountMap = new HashMap<>();
				
		try (BufferedReader br = new BufferedReader(new FileReader(this.pathPreviousCommunityCount))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                String pair = values[0];
                int count = Integer.parseInt(values[1]);
                previousCountMap.put(pair, count);
            }
        } catch (IOException e) {
            System.out.println("File "+this.pathPreviousCommunityCount+" does not exist.");
        }
        
        this.currentPreviousCountMap = previousCountMap;
	}
	
	public void setRouterInfo() {
		if (Settings.DEF_SETTINGS_FILE.contains("pcrouter")) {
			scenario.getHosts().forEach(host ->
	        {
        		MessageRouter mRouter = host.getRouter();
        		PCRouter router = (PCRouter) ((DecisionEngineRouter)mRouter).getDecisionEngine();
        		router.setMeanNoderPerCommunity(scenario.getHosts().size() / this.currentNodesPerCommunity.size());
	    		router.clearCounters();
	    		if (this.currentCommunityIdMap.containsKey(host.getAddress())) {
	        		int label = this.currentCommunityIdMap.get(host.getAddress());
	        		router.setLabel(label);
	        		router.setNodesPerCommunity(this.currentNodesPerCommunity);
	        	}
	    		
	    		Map<Integer, Integer> previousCount = new HashMap<>();
	        	String key = host.getAddress() + "_";
	        	this.currentPreviousCountMap.forEach((pair, count) -> {
	        		if (pair.contains(key)) {
	        			previousCount.put(Integer.valueOf(pair.replace(key, "")), count);
	        		}
	        	});
	        	router.setTimesInCommunityWith(previousCount);
	        });
		}
	}
}
