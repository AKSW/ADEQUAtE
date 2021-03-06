package org.aksw.dependency.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.dependency.util.SimpleGraphFormatter;
import org.jgrapht.graph.DefaultDirectedGraph;

public class ColoredDirectedGraph extends DefaultDirectedGraph<Node, ColoredEdge>{
	
	private transient SimpleGraphFormatter formatter = new SimpleGraphFormatter();
	
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
	
	public String dump(){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Node node : vertexSet()) {
			sb.append(node.getLabel() + "(" + node.getId() + "),");
		}
		sb.append("][");
		for (ColoredEdge edge : edgeSet()) {
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);
			sb.append(edge.getLabel() + "(" + source.getLabel() + "(" + source.getId() + "),(" + target.getLabel() + "(" + target.getId() + ")),");
		}
		sb.append("]");
		return sb.toString();
	}
	

	public Set<Node> getRoots(){
		Set<Node> roots = new HashSet<>();
		for (Node node : vertexSet()) {
			if(inDegreeOf(node) == 0){
				roots.add(node);
			}
		}
		return roots;
	}
	
	public String prettyPrint(){
		return formatter.formatGraph(this);
	}
	
	public static ColoredDirectedGraph parse(String nodesAsString, String edgesAsString){
		ColoredDirectedGraph g = new ColoredDirectedGraph();
		//parse nodes
		Map<String, Node> id2Node = new HashMap<>();
		String[] nodes = nodesAsString.split(",");
		for (String nodeId : nodes) {
			String id = nodeId;
			Node node;
			if(nodeId.contains("(")){
				id = nodeId.substring(0, nodeId.indexOf('('));
				String label = "";
				Pattern p = Pattern.compile("(\\()(.*?)(\\))");
				Matcher m = p.matcher(nodeId);
				while (m.find()) {
				  label = m.group(2);
				}
				node = new Node(id, label);
			} else {
				node = new Node(nodeId);
			}
			id2Node.put(id, node);
			g.addVertex(node);
		}
		//parse edges
		String[] edges = edgesAsString.split("\\)");
		for (String edge : edges) {
			String[] edgeNodes = edge.replace("(", "").replace(")", "").split(",");
			Node source = id2Node.get(edgeNodes[0]);
			Node target = id2Node.get(edgeNodes[1]);
			g.addEdge(source, target, new ColoredEdge("edge", "white"));
		}
		return g;
	}

}
