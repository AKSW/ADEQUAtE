package org.aksw.dependency.graph;

public class ClassNode extends Node{
	
	private String name;
	
	public ClassNode(String name) {
		this(name, name);
	}
	
	public ClassNode(String id, String name) {
		super(id, name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		return new ClassNode(id, "Class");
	}

}
