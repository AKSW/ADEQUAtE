package org.aksw.dependency.util;

import java.util.Map;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.Node;

import com.google.common.collect.BiMap;

public interface Matcher {
	
	BiMap<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2);
	
	BiMap<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2, Map<String, String> manulaMapping);

}
