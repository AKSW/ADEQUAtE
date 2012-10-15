package org.aksw.dependency.graph;

import java.util.Set;

import org.jgrapht.DirectedGraph;

public class DependencySubgraph extends ColoredDirectedSubgraph{
	
	public DependencySubgraph(DirectedGraph<Node, ColoredEdge> base, Set<Node> vertexSubset,
			Set<ColoredEdge> edgeSubset) {
		super(base, vertexSubset, edgeSubset);
	}
	
	public ColoredDirectedGraph toGeneralizedGraph(){
		DependencyGraph graph = new DependencyGraph();
		
		for(ColoredEdge edge : edgeSet()){
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);
			
			source = source.asGeneralizedNode();
			target = target.asGeneralizedNode();
			graph.addVertex(source);
			graph.addVertex(target);
			graph.addEdge(source, target, new ColoredEdge(edge.getLabel(), edge.getColor()));
		}
		
		return graph;
	};
}
