package org.aksw.dependency.graph;

import org.jgrapht.experimental.equivalence.EquivalenceComparator;

public class NodeComparator implements EquivalenceComparator<Node, ColoredDirectedGraph>{
	
	public NodeComparator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean equivalenceCompare(Node node1, Node node2, ColoredDirectedGraph context1,
			ColoredDirectedGraph context2) {
		return node1.getLabel().equals(node2.getLabel());
	}

	@Override
	public int equivalenceHashcode(Node node, ColoredDirectedGraph context) {
		return node.getLabel().hashCode();
	}
	
}
