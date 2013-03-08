package org.aksw.dependency.converter;

import org.aksw.dependency.graph.ColoredDirectedGraph;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;

public class DependencyGraphConverter {
	
	private static final String URI = "http://aksw.org/dependency/";
	
	public static Model convert(SemanticGraph dependencyGraph){
		Model model = ModelFactory.createDefaultModel();
		for(SemanticGraphEdge edge : dependencyGraph.getEdgeSet()){
			Resource subject = model.getResource(URI + edge.getSource().toString());
			Resource object = model.getResource(URI + edge.getTarget().toString());
			Property property = model.getProperty(URI + edge.getRelation().toString());
			model.add(subject, property, object);
		}
		return model;
	}
	
}
