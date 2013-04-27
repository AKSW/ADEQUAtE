package org.aksw.dependency.rule.clustering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.rule.Rule;
import org.aksw.dependency.util.SimPackGraphWrapper;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsSame;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;

import simpack.measure.graph.GraphIsomorphism;

public class OptimizedRuleClustering implements RuleClustering {
	
	
	private static final Logger logger = Logger.getLogger(OptimizedRuleClustering.class.getName());

	@Override
	public Map<Rule, Integer> clusterRules(Collection<Rule> rules) {
		return null;
	}
	
	@Override
	public Map<Rule, Integer> clusterRules(Map<String, Collection<Rule>> questionWithRules) {
		logger.info("Clustering rules...");
		Map<Rule, Integer> rule2Frequency = new HashMap<Rule, Integer>();
		Set<Rule> allRules = new HashSet<>();
		
		for (Collection<Rule> entry : questionWithRules.values()) {
			allRules.addAll(entry);
		}
		for(Rule rule1 : allRules){
			int cnt = 0;
			Rule r = null;
			for(Entry<Rule, Integer> entry : rule2Frequency.entrySet()){
				Rule rule2 = entry.getKey();
				
				boolean equalRules = equalRules(rule1, rule2);
				
		        r = null;
		        if(equalRules){
		        	cnt = entry.getValue()+1;
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(cnt > 1){
				if(r != null){
					rule2Frequency.remove(r);
				}
				rule2Frequency.put(rule1, Integer.valueOf(cnt));
			} else {
				rule2Frequency.put(rule1, Integer.valueOf(1));
			}
			
		}
		logger.info("...got " + rule2Frequency.size() + " clusters.");
		return rule2Frequency;
	}
	
	private boolean equalRules(Rule rule1, Rule rule2){
		ColoredDirectedGraph sourceGraph1 = rule1.getSource();
		ColoredDirectedGraph sourceGraph2 = rule2.getSource();
		ColoredDirectedGraph targetGraph1 = rule1.getTarget();
		ColoredDirectedGraph targetGraph2 = rule2.getTarget();
		
		boolean sameNodeSet = containSameNodes(sourceGraph1, sourceGraph2) && containSameNodes(targetGraph1, targetGraph2);
		if(!sameNodeSet){
			return false;
		}
		
		boolean sameEdgeSet = containSameEdges(sourceGraph1, sourceGraph2) && containSameEdges(targetGraph1, targetGraph2);
		if(!sameEdgeSet){
			return false;
		}
		
		boolean isomorphic = isomorphic(sourceGraph1, sourceGraph2);
		if(!isomorphic){
			return false;
		}
		
		isomorphic = isomorphic(targetGraph1, targetGraph2);
		if(!isomorphic){
			return false;
		}
		
		isomorphic = isomorphic(rule1.asConnectedGraph(), rule2.asConnectedGraph());
		
		return isomorphic;
	}
	
	private boolean isomorphic(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2){
		GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(graph1), SimPackGraphWrapper.getGraph(graph2));
		gi.calculate();
		return gi.getGraphIsomorphism() == 1;
//		GraphIsomorphismInspector iso =
//	            AdaptiveIsomorphismInspectorFactory.createIsomorphismInspector(
//	            		graph1,
//	            		graph2,
//	                null,
//	                null);
//		long start = System.currentTimeMillis();
//		boolean isomorphic = iso.isIsomorphic();
//		long end = System.currentTimeMillis();
////		System.out.println("Operation took " + (end - start) + "ms");
//		return isomorphic;
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
	
	private boolean containSameEdges(ColoredDirectedGraph g1, ColoredDirectedGraph g2){
		if(g1.edgeSet().size() != g2.edgeSet().size()){
			return false;
		}
		for(ColoredEdge edge1 : g1.edgeSet()){
			Node source1 = g1.getEdgeSource(edge1);
			Node target1 = g1.getEdgeTarget(edge1);
			boolean match = false;
			for(ColoredEdge edge2 : g2.edgeSet()){
				Node source2 = g2.getEdgeSource(edge2);
				Node target2 = g2.getEdgeTarget(edge2);
				if(source1.getLabel().equals(source2.getLabel()) && target1.getLabel().equals(target2.getLabel())){
					match = true;
					break;
				}
			}
			if(!match){
				return false;
			}
		}
		for(ColoredEdge edge2 : g2.edgeSet()){
			Node source2 = g2.getEdgeSource(edge2);
			Node target2 = g2.getEdgeTarget(edge2);
			boolean match = false;
			for(ColoredEdge edge1 : g1.edgeSet()){
				Node source1 = g1.getEdgeSource(edge1);
				Node target1 = g1.getEdgeTarget(edge1);
				if(source1.getLabel().equals(source2.getLabel()) && target1.getLabel().equals(target2.getLabel())){
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
