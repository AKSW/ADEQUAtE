package org.aksw.dependency.converter;

import java.util.Set;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.util.TriplePatternExtractor;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

public class SPARQLQuery2GraphConverter {
	
	private static final String COLOR = "white";

	public ColoredDirectedGraph getGraph(Query query){
		ColoredDirectedGraph graph = new ColoredDirectedGraph();
		
		TriplePatternExtractor extractor = new TriplePatternExtractor();
		Set<Triple> triples = extractor.extractTriplePattern(query);
		
		int i=0;
		for(Triple t : triples){
			Node subject = new Node(t.getSubject().toString());
			Node predicate = new Node(t.getPredicate().toString());
			Node object = new Node(t.getObject().toString());
			
			graph.addVertex(subject);
			graph.addVertex(predicate);
			graph.addVertex(object);
			
			graph.addEdge(subject, predicate, new ColoredEdge("edge"+i++, COLOR));
			graph.addEdge(predicate, object, new ColoredEdge("edge"+i++, COLOR));
			
		}
		
		return graph;
	}
	
	public ColoredDirectedGraph getGraph(String query){
		return getGraph(QueryFactory.create(query, Syntax.syntaxARQ));
	}
}
