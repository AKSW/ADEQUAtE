package org.aksw.dependency.graph.clustering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.dependency.Rule;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.EdgeComparator;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.NodeComparator;
import org.aksw.dependency.util.SimPackGraphWrapper;
import org.jgrapht.DirectedGraph;
import org.jgrapht.experimental.equivalence.EquivalenceComparator;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;

import simpack.measure.graph.GraphIsomorphism;

public class SimpleGraphClustering {

	public void clusterRules(List<Rule> rules){
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
//				
//				GraphIsomorphismInspector<ColoredDirectedGraph> iso = AdaptiveIsomorphismInspectorFactory.
//						createIsomorphismInspector(currentGraph, graph, new EquivalenceComparator<Node, DefaultDirectedGraph<Node, ColoredEdge>>() {
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
