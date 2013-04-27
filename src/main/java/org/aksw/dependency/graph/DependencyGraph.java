package org.aksw.dependency.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;

public class DependencyGraph extends ColoredDirectedGraph{
	
	public ColoredDirectedGraph toGeneralizedGraph(){
		SPARQLGraph graph = new SPARQLGraph();
		
		Map<Node, Node> node2Newnode = new HashMap<Node, Node>(); 
		
		for(ColoredEdge edge : edgeSet()){
			
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);

			Node newSource = node2Newnode.get(source);
			if(newSource == null){
				newSource = source.asGeneralizedNode();
				node2Newnode.put(source, newSource);
			}
			
			Node newTarget = node2Newnode.get(target);
			if(newTarget == null){
				newTarget = target.asGeneralizedNode();
				node2Newnode.put(target, newTarget);
			}
			
			graph.addVertex(newSource);
			graph.addVertex(newTarget);
			graph.addEdge(newSource, newTarget, new ColoredEdge(edge.getLabel(), edge.getColor()));
		}
		
		return graph;
	};
}
