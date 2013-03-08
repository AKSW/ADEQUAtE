package org.aksw.dependency.graph;

import org.jgrapht.experimental.equivalence.EquivalenceComparator;
import org.jgrapht.graph.DefaultDirectedGraph;

public class EdgeComparator implements EquivalenceComparator<ColoredEdge, ColoredDirectedGraph>{

	@Override
	public boolean equivalenceCompare(ColoredEdge arg1, ColoredEdge arg2,
			ColoredDirectedGraph context1, ColoredDirectedGraph context2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int equivalenceHashcode(ColoredEdge arg1, ColoredDirectedGraph context) {
		// TODO Auto-generated method stub
		return 0;
	}

}
