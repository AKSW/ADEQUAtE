package org.aksw.dependency.graph;

public class ResourceNode extends Node{
	
	private String name;
	
	public ResourceNode(String name) {
		this(name, name);
	}
	
	public ResourceNode(String id, String name) {
		super(id, name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		return new ResourceNode(id, "Resource");
	}

}
