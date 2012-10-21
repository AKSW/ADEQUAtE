package org.aksw.dependency.graph;


public class LiteralNode extends Node{
	
	private String name;
	
	public LiteralNode(String name) {
		this(name, name);
	}
	
	public LiteralNode(String id, String name) {
		super(id, name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		return new LiteralNode(id, "Literal");
	}
}
