package org.aksw.dependency.graph;

import org.jgrapht.graph.DefaultDirectedGraph;

public class ColoredDirectedGraph extends DefaultDirectedGraph<Node, ColoredEdge>{
	
	public ColoredDirectedGraph() {
		super(ColoredEdge.class);
	}
	
	public ColoredDirectedGraph(ColoredDirectedGraph ... graphs) {
		super(ColoredEdge.class);
		
		for(ColoredDirectedGraph g : graphs){
			for(Node node : g.vertexSet()){
				addVertex(node);
			}
			
			for(ColoredEdge edge : g.edgeSet()){
				addEdge(g.getEdgeSource(edge), g.getEdgeTarget(edge), edge);
			}
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColoredDirectedGraph other = (ColoredDirectedGraph) obj;
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
