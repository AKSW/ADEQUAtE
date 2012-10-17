package org.aksw.dependency.graph;

import com.hp.hpl.jena.vocabulary.RDF;

public class PropertyNode extends Node{
	
	private String name;
	
	public PropertyNode(String name) {
		this(name, name);
	}
	
	public PropertyNode(String id, String name) {
		super(id, name);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Node asGeneralizedNode(){
		if(name.equals(RDF.type.getURI())){
			label = "rdf:type";
			return this;
		} else {
			return new PropertyNode(id, "Property");
		}
	}
}
