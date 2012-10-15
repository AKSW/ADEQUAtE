package org.aksw.dependency.graph;

import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedSubgraph;

public class ColoredDirectedSubgraph extends DirectedSubgraph<Node, ColoredEdge>{

	public ColoredDirectedSubgraph(DirectedGraph<Node, ColoredEdge> base, Set<Node> vertexSubset,
			Set<ColoredEdge> edgeSubset) {
		super(base, vertexSubset, edgeSubset);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColoredDirectedSubgraph other = (ColoredDirectedSubgraph) obj;
		return other.vertexSet().equals(vertexSet()) && other.edgeSet().equals(edgeSet());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((vertexSet() == null) ? 0 : vertexSet().hashCode());
		result = prime * result + ((edgeSet() == null) ? 0 : edgeSet().hashCode());
		return result;
	}
	
	public ColoredDirectedGraph toGeneralizedGraph(){return null;};

}
