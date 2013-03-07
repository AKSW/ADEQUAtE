package org.aksw.dependency.util;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredDirectedSubgraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.IsoGraph;

import simpack.accessor.graph.SimpleGraphAccessor;
import simpack.util.graph.GraphNode;

public class SimPackGraphWrapper {

	public static SimpleGraphAccessor getGraph(ColoredDirectedGraph originalGraph){
		 SimpleGraphAccessor graph = new SimpleGraphAccessor();
		 
		 for(ColoredEdge edge : originalGraph.edgeSet()){
			 GraphNode source = new GraphNode(originalGraph.getEdgeSource(edge));
			 GraphNode target = new GraphNode(originalGraph.getEdgeTarget(edge));
			 
			 graph.addNode(source);
			 graph.addNode(target);
			 graph.setEdge(source, target);
		 }
		 
		 return graph;
	}
	
	public static SimpleGraphAccessor getGraph(ColoredDirectedSubgraph originalGraph){
		 SimpleGraphAccessor graph = new SimpleGraphAccessor();
		 
		 for(ColoredEdge edge : originalGraph.edgeSet()){
			 GraphNode source = new GraphNode(originalGraph.getEdgeSource(edge));
			 GraphNode target = new GraphNode(originalGraph.getEdgeTarget(edge));
			 
			 graph.addNode(source);
			 graph.addNode(target);
			 graph.setEdge(source, target);
		 }
		 
		 return graph;
	}
	
	public static IsoGraph getGraphIdBased(ColoredDirectedGraph originalGraph){
		 IsoGraph graph = new IsoGraph();
		 
		 for(ColoredEdge edge : originalGraph.edgeSet()){
			 GraphNode source = new GraphNode(originalGraph.getEdgeSource(edge));
			 GraphNode target = new GraphNode(originalGraph.getEdgeTarget(edge));
			 
			 graph.addNode(source);
			 graph.addNode(target);
			 graph.setEdge(source, target);
		 }
		 
		 return graph;
	}
	
	public static IsoGraph getGraphIdBased(ColoredDirectedSubgraph originalGraph){
		 IsoGraph graph = new IsoGraph();
		 
		 for(ColoredEdge edge : originalGraph.edgeSet()){
			 GraphNode source = new GraphNode(originalGraph.getEdgeSource(edge));
			 GraphNode target = new GraphNode(originalGraph.getEdgeTarget(edge));
			 
			 graph.addNode(source);
			 graph.addNode(target);
			 graph.setEdge(source, target);
		 }
		 
		 return graph;
	}

}
