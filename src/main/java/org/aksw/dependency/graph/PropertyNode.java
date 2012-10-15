package org.aksw.dependency.graph;

import com.hp.hpl.jena.vocabulary.RDF;

public class PropertyNode extends Node{
	
	private String name;
	
	public PropertyNode(String name) {
		super(name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		if(name.equals(RDF.type.getURI())){
			return this;
		} else {
			return new PropertyNode("Property");
		}
	}
}
