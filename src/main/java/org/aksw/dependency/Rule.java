package org.aksw.dependency;

import org.aksw.dependency.graph.ColoredDirectedGraph;

public class Rule {
	
	private ColoredDirectedGraph source;
	private ColoredDirectedGraph target;
	
	public Rule(ColoredDirectedGraph source, ColoredDirectedGraph target) {
		this.source = source;
		this.target = target;
	}
	
	public ColoredDirectedGraph getSource() {
		return source;
	}
	
	public ColoredDirectedGraph getTarget() {
		return target;
	}
	
	@Override
	public String toString() {
		return "Source:\t" + source + "\nTarget:\t" + target;
	}
	

}
