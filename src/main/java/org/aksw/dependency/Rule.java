package org.aksw.dependency;

import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.jgrapht.graph.DirectedSubgraph;

public class Rule {
	
	private DirectedSubgraph<Node, ColoredEdge> source;
	private DirectedSubgraph<Node, ColoredEdge> target;
	
	public Rule(DirectedSubgraph<Node, ColoredEdge> source, DirectedSubgraph<Node, ColoredEdge> target) {
		this.source = source;
		this.target = target;
	}
	
	public DirectedSubgraph<Node, ColoredEdge> getSource() {
		return source;
	}
	
	public DirectedSubgraph<Node, ColoredEdge> getTarget() {
		return target;
	}
	

}
