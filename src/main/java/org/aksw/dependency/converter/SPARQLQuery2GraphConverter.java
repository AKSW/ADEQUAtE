package org.aksw.dependency.converter;

import java.util.Set;

import org.aksw.dependency.graph.ClassNode;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.PropertyNode;
import org.aksw.dependency.graph.ResourceNode;
import org.aksw.dependency.graph.SPARQLGraph;
import org.aksw.dependency.graph.VariableNode;
import org.aksw.dependency.util.SPARQLUtil;
import org.aksw.dependency.util.TriplePatternExtractor;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.vocabulary.RDF;

public class SPARQLQuery2GraphConverter {
	
	private static final Logger logger = Logger.getLogger(SPARQLQuery2GraphConverter.class);
	private static final String COLOR = "white";
	
	private String endpointURL;
	
	public SPARQLQuery2GraphConverter(String endpointURL) {
		this.endpointURL = endpointURL;
	}

	public ColoredDirectedGraph getGraph(Query query){
		ColoredDirectedGraph graph = new SPARQLGraph();
		
		TriplePatternExtractor extractor = new TriplePatternExtractor();
		Set<Triple> triples = extractor.extractTriplePattern(query);
		
		for(Triple t : triples){
			Node subject = getNode(t.getSubject());
			Node predicate = getNode(t.getPredicate());
			Node object = getNode(t.getObject());
			
			graph.addVertex(subject);
			graph.addVertex(predicate);
			graph.addVertex(object);
			
			graph.addEdge(subject, predicate, new ColoredEdge("edge", COLOR));
			graph.addEdge(predicate, object, new ColoredEdge("edge", COLOR));			
		}
		return graph;
	}
	
	public ColoredDirectedGraph getGraph(String query){
		return getGraph(QueryFactory.create(query, Syntax.syntaxARQ));
	}
	
	public Node getNode(com.hp.hpl.jena.graph.Node node){
		if(node.isVariable()){
			return new VariableNode(node.toString());
		} else if(node.isURI()){
			String uri = node.getURI();
			if(node.matches(RDF.type.asNode())){
				return new PropertyNode(uri, uri);
			} else {
				if(SPARQLUtil.isClass(endpointURL, uri)){
					return new ClassNode(uri, uri);
				} else if(SPARQLUtil.isProperty(endpointURL, uri)){
					return new PropertyNode(uri);
				} else {
					return new ResourceNode(uri);
				}
			}
		} else if(node.isLiteral()){
			
		} 
		return null;
	}
}
