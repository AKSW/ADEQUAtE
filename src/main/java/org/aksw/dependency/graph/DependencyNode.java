package org.aksw.dependency.graph;

public class DependencyNode extends Node{
	
	private String posTag;
	
	public DependencyNode(String id, String label, String posTag) {
		super(id, label);
		this.posTag = posTag;
	}
	
	public DependencyNode(String id, String posTag) {
		this(id, id, posTag);
	}
	
	public String getPosTag() {
		return posTag;
	}
	
	public String toString() {
		return label + (posTag != null ? ("/" + posTag) : "");
	}
	
	@Override
	public Node asGeneralizedNode(){
		return new Node(id, posTag);
	}

}
