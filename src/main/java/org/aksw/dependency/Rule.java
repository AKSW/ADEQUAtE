package org.aksw.dependency;

import java.util.Map;
import java.util.Map.Entry;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;

public class Rule {
	
	private ColoredDirectedGraph source;
	private ColoredDirectedGraph target;
	private Map<Node, Node> mapping;
	
	public Rule(ColoredDirectedGraph source, ColoredDirectedGraph target, Map<Node, Node> mapping) {
		this.source = source;
		this.target = target;
		this.mapping = mapping;
	}
	
	public ColoredDirectedGraph getSource() {
		return source;
	}
	
	public ColoredDirectedGraph getTarget() {
		return target;
	}
	
	public Map<Node, Node> getMapping() {
		return mapping;
	}
	
	public ColoredDirectedGraph asConnectedGraph(){
		ColoredDirectedGraph connectedGraph = new ColoredDirectedGraph(source, target);
		
		for(Entry<Node, Node> entry : mapping.entrySet()){
			Node source = entry.getKey();
			Node target = entry.getValue();
			connectedGraph.addVertex(source);
			connectedGraph.addVertex(target);
			connectedGraph.addEdge(source, target, new ColoredEdge("edge", "yellow"));
		}
		
		return connectedGraph;
	}
	
	@Override
	public String toString() {
		return "Rule:\nSource:\t" + source + "\nTarget:\t" + target + "\nMapping:\t" + mapping;
	}
	

}
