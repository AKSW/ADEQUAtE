package org.aksw.dependency.graph;

public class DependencyNode extends Node{
	
	private String posTag;
	
	public DependencyNode(String label, String posTag) {
		super(label);
		this.posTag = posTag;
	}
	
	public String getPosTag() {
		return posTag;
	}
	
	public String toString() {
		return label + (posTag != null ? ("/" + posTag) : "");
	}
	
	@Override
	public Node asGeneralizedNode(){
		return new Node(posTag);
	}

}
