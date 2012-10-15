package org.aksw.dependency.util;

import java.util.Map;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.Node;

public interface Matcher {
	
	Map<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2);

}
