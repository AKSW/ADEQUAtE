package org.aksw.dependency.graph;

public class ClassNode extends Node{
	
	private String className;
	
	public ClassNode(String className) {
		super(className);
		this.className = className;
	}
	
	public String getClassName() {
		return className;
	}
	
	public Node asGeneralizedNode(){
		return new ClassNode("Class");
	}

}
