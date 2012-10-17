package org.aksw.dependency;

import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.jgrapht.DirectedGraph;

public class Rule {
	
	private DirectedGraph<Node, ColoredEdge> source;
	private DirectedGraph<Node, ColoredEdge> target;
	
	public Rule(DirectedGraph<Node, ColoredEdge> source, DirectedGraph<Node, ColoredEdge> target) {
		this.source = source;
		this.target = target;
	}
	
	public DirectedGraph<Node, ColoredEdge> getSource() {
		return source;
	}
	
	public DirectedGraph<Node, ColoredEdge> getTarget() {
		return target;
	}
	
	@Override
	public String toString() {
		return "Source:\t" + source + "\nTarget:\t" + target;
	}
	

}
