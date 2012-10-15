package org.aksw.dependency.graph;

public class ResourceNode extends Node{
	
	private String name;
	
	public ResourceNode(String name) {
		super(name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		return new ResourceNode("Resource");
	}

}
