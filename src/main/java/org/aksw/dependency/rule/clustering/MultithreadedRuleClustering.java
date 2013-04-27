package org.aksw.dependency.rule.clustering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.rule.Rule;
import org.aksw.dependency.util.SimPackGraphWrapper;
import org.apache.log4j.Logger;

import simpack.measure.graph.GraphIsomorphism;

import com.google.common.collect.Lists;

public class MultithreadedRuleClustering implements RuleClustering {
	
	
	private static final Logger logger = Logger.getLogger(MultithreadedRuleClustering.class.getName());

	@Override
	public Map<Rule, Integer> clusterRules(Collection<Rule> rules) {
		logger.info("Clustering rules...");
		int nrOfThreads = Runtime.getRuntime().availableProcessors();
		final ExecutorService executor = Executors.newFixedThreadPool(nrOfThreads);
		List<Rule> ruleList = new ArrayList<>(rules);
		int partitionSize = ruleList.size() / nrOfThreads;
		List<List<Rule>> partition = Lists.partition(ruleList, partitionSize);
		for (List<Rule> list : partition) {
			final Map<Rule, Integer> rule2Frequency = new HashMap<Rule, Integer>();
			for(Rule rule : list){
				rule2Frequency.put(rule, 1);
			}
			executor.submit(new Callable<Map<Rule, Integer>>() {

				@Override
				public Map<Rule, Integer> call() throws Exception {
					return cluster(new HashMap<Rule, Integer>(), rule2Frequency);
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
//		logger.info("...got " + rule2Frequency.size() + " clusters.");
		return null;
	}
	
	private Map<Rule, Integer> cluster(Map<Rule, Integer> rule2Frequency, Map<Rule, Integer> rules){
		for(Entry<Rule, Integer> ruleWithFrequency : rules.entrySet()){
			Rule rule = ruleWithFrequency.getKey();
			int frequency = ruleWithFrequency.getValue();
			Rule r = null;
			for(Entry<Rule, Integer> entry : rule2Frequency.entrySet()){
				ColoredDirectedGraph currentGraph = rule.asConnectedGraph();
				ColoredDirectedGraph graph = entry.getKey().asConnectedGraph();
				
				boolean sameNodeSet = containSameNodes(currentGraph, graph);
				if(!sameNodeSet){
					continue;
				}
				
				if((currentGraph.edgeSet().size() != graph.edgeSet().size())){
					continue;
				}
				
//				GraphIsomorphismInspector<ColoredDirectedGraph> iso = AdaptiveIsomorphismInspectorFactory.
//						createIsomorphismInspector(currentGraph, graph, null, null);
//				System.out.println(iso.isIsomorphic());
				
				GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(currentGraph), SimPackGraphWrapper.getGraph(graph));
				gi.calculate();
		        r = null;
		        if(gi.getGraphIsomorphism() == 1){
		        	frequency += entry.getValue();
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(frequency > 1){
				if(r != null){
					rule2Frequency.remove(r);
				}
				rule2Frequency.put(rule, Integer.valueOf(frequency));
			} else {
				rule2Frequency.put(rule, Integer.valueOf(1));
			}
		}
		return rule2Frequency;
	}
	
	@Override
	public Map<Rule, Integer> clusterRules(Map<String, Collection<Rule>> questionWithRules) {
		Set<Rule> allRules = new HashSet<>();
		
		for (Collection<Rule> entry : questionWithRules.values()) {
			allRules.addAll(entry);
		}
		
		return clusterRules(allRules);
	}
	
	private boolean containSameNodes(ColoredDirectedGraph g1, ColoredDirectedGraph g2){
		if(g1.vertexSet().size() != g2.vertexSet().size()){
			return false;
		}
		for(Node n1 : g1.vertexSet()){
			boolean match = false;
			for(Node n2 : g2.vertexSet()){
				if(n1.getLabel().equals(n2.getLabel())){
					match = true;
					break;
				}
			}
			if(!match){
				return false;
			}
		}
		for(Node n2 : g2.vertexSet()){
			boolean match = false;
			for(Node n1 : g1.vertexSet()){
				if(n1.getLabel().equals(n2.getLabel())){
					match = true;
					break;
				}
			}
			if(!match){
				return false;
			}
		}
		
		return true;
	}

}
