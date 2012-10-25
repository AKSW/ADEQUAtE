package org.aksw.dependency.util;

import java.util.Map;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.Node;

public class Generalization {
	
	private ColoredDirectedGraph generalizedGraph;
	private Map<Node, Node> mapping;
	
	public Generalization(ColoredDirectedGraph generalizedGraph, Map<Node, Node> mapping) {
		this.generalizedGraph = generalizedGraph;
		this.mapping = mapping;
	}
	
	public ColoredDirectedGraph getGeneralizedGraph() {
		return generalizedGraph;
	}
	
	public Map<Node, Node> getMapping() {
		return mapping;
	}

}
