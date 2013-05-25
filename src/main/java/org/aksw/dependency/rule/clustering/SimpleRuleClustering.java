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

import simpack.measure.graph.GraphIsomorphism;

public class SimpleRuleClustering implements RuleClustering {
	
	
	private static final Logger logger = Logger.getLogger(SimpleRuleClustering.class.getName());

	@Override
	public Map<Rule, Integer> clusterRules(Collection<Rule> rules) {
		logger.info("Clustering rules...");
		Map<Rule, Integer> rule2Frequency = new HashMap<Rule, Integer>();
		for(Rule rule : rules){
			int cnt = 0;
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
		        	cnt = entry.getValue()+1;
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(cnt > 1){
				if(r != null){
					rule2Frequency.remove(r);
				}
				rule2Frequency.put(rule, Integer.valueOf(cnt));
			} else {
				rule2Frequency.put(rule, Integer.valueOf(1));
			}
			
		}
		logger.info("...got " + rule2Frequency.size() + " clusters.");
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
		GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(sourceGraph1), SimPackGraphWrapper.getGraph(sourceGraph2));
		gi.calculate();
		if(gi.getGraphIsomorphism() != 1){
			return false;
		}
		
		gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(targetGraph1), SimPackGraphWrapper.getGraph(targetGraph2));
		gi.calculate();
		if(gi.getGraphIsomorphism() != 1){
			return false;
		}
		
		gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(rule1.asConnectedGraph()), SimPackGraphWrapper.getGraph(rule2.asConnectedGraph()));
		gi.calculate();
        return gi.getGraphIsomorphism() == 1;
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
