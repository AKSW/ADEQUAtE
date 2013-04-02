package org.aksw.dependency.graph;

public class VariableNode extends Node{
	
	private String varName;
	
	public VariableNode(String name) {
		this(name, name);
	}
	
	public VariableNode(String id, String varName) {
		super(id, varName);
		this.varName = varName;
	}
	
	public String getVarName() {
		return varName;
	}
	
	@Override
	public String toString() {
		return label + "(?" + id + ")";
	}
	
	@Override
	public Node asGeneralizedNode() {
		return new VariableNode(id, "Variable");
	}

}
