package org.aksw.dependency.rule;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;

public class Rule implements Serializable{
	
	private static final long serialVersionUID = 2995597261242916043L;
	private String label;
	private ColoredDirectedGraph source;
	private ColoredDirectedGraph target;
	private Map<Node, Node> mapping;
	
	public Rule(ColoredDirectedGraph source, ColoredDirectedGraph target, Map<Node, Node> mapping) {
		this.source = source;
		this.target = target;
		this.mapping = mapping;
	}
        
        public Rule(ColoredDirectedGraph source, ColoredDirectedGraph target, Map<Node, Node> mapping, String label) {
		this.source = source;
		this.target = target;
		this.mapping = mapping;
                this.label = label;
	}
	
        public String getLabel()
        {
            return label;
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
            if(label == null)
		return "Rule:\nSource:\t" + source.dump() + "\nTarget:\t" + target.dump() + "\nMapping:\t" + mapping;
            else
                return "Rule "+label+":\nSource:\t" + source + "\nTarget:\t" + target + "\nMapping:\t" + mapping;
	}
	
	public static Rule parse(ColoredDirectedGraph sourceGraph, ColoredDirectedGraph targetGraph, String mappingString){
		Map<Node, Node> mapping = new HashMap<>();
		String[] edges = mappingString.split("\\)");
		for (String edge : edges) {
			String[] edgeNodes = edge.replace("(", "").replace(")", "").split(",");
			Node source = new Node(edgeNodes[0]);
			Node target = new Node(edgeNodes[1]);
			mapping.put(source, target);
		}
		return new Rule(sourceGraph, targetGraph, mapping);
	}
	

}
