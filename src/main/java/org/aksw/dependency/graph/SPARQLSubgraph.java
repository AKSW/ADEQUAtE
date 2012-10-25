package org.aksw.dependency.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.aksw.dependency.util.Generalization;
import org.jgrapht.DirectedGraph;

public class SPARQLSubgraph extends ColoredDirectedSubgraph{
	
	public SPARQLSubgraph(DirectedGraph<Node, ColoredEdge> base, Set<Node> vertexSubset,
			Set<ColoredEdge> edgeSubset) {
		super(base, vertexSubset, edgeSubset);
	}
	
	public Generalization generalize(){
		SPARQLGraph graph = new SPARQLGraph();
		Map<Node, Node> mapping = new HashMap<Node, Node>();
		
		for(ColoredEdge edge : edgeSet()){
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);
			
			Node generalizedSource = source.asGeneralizedNode();
			Node generalizedTarget = target.asGeneralizedNode();

			graph.addVertex(generalizedSource);
			graph.addVertex(generalizedTarget);
			graph.addEdge(generalizedSource, generalizedTarget, new ColoredEdge(edge.getLabel(), edge.getColor()));
			
			mapping.put(source, generalizedSource);
			mapping.put(target, generalizedTarget);
		}
		
		return new Generalization(graph, mapping);
	};

}
