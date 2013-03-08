package org.aksw.dependency.graph;

import org.jgrapht.experimental.equivalence.EquivalenceComparator;

public class NodeeComparator implements EquivalenceComparator<Node, ColoredDirectedGraph>{
	
	public NodeeComparator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean equivalenceCompare(Node arg1, Node arg2, ColoredDirectedGraph context1,
			ColoredDirectedGraph context2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int equivalenceHashcode(Node arg1, ColoredDirectedGraph context) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
