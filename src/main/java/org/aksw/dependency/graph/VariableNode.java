package org.aksw.dependency.graph;

public class VariableNode extends Node{
	
	private String varName;
	
	public VariableNode(String varName) {
		super(varName);
		this.varName = varName;
	}
	
	public String getVarName() {
		return varName;
	}
	
	@Override
	public String toString() {
		return varName;
	}
	
	@Override
	public Node asGeneralizedNode() {
		return new VariableNode("Variable");
	}

}
