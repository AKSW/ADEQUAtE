package org.aksw.dependency.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.core.Var;

public class SPARQLGraph extends ColoredDirectedGraph{

	public ColoredDirectedGraph toGeneralizedGraph(){
		SPARQLGraph graph = new SPARQLGraph();
		
		Map<Node, Node> node2Newnode = new HashMap<Node, Node>(); 
		
		for(Node node : vertexSet()){
			Node newNode = node.asGeneralizedNode();
			node2Newnode.put(node, newNode);
			graph.addVertex(newNode);
		}
		
		for(ColoredEdge edge : edgeSet()){
			
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);

			Node newSource = node2Newnode.get(source);
			Node newTarget = node2Newnode.get(target);
			
			graph.addEdge(newSource, newTarget, new ColoredEdge(edge.getLabel(), edge.getColor()));
		}
		
		return graph;
	}
	
	public Query toSPARQLQuery(){
		//get all nodes in graph
		Set<Node> nodes = vertexSet();
		//find first all nodes which represent a property
		Set<Node> propertyNodes = new HashSet<Node>();
		for (Node node : nodes) {
			if(node instanceof PropertyNode){
				propertyNodes.add(node);
			}
		}
		//create the triples starting from the property nodes
		for (Node node : propertyNodes) {
			//the predicate
			com.hp.hpl.jena.graph.Node predicate = com.hp.hpl.jena.graph.Node.createURI(node.getId());
			//the subjects
			List<com.hp.hpl.jena.graph.Node> subjects = new ArrayList<com.hp.hpl.jena.graph.Node>();
			Set<ColoredEdge> incomingEdges = incomingEdgesOf(node);
			for (ColoredEdge edge : incomingEdges) {
				Node source = getEdgeSource(edge);
				if(source instanceof VariableNode){
					subjects.add(Var.alloc(source.getId()));
				} else {
					subjects.add(com.hp.hpl.jena.graph.Node.createURI(source.getId()));
				}
			}
			//the objects
			List<com.hp.hpl.jena.graph.Node> objects = new ArrayList<com.hp.hpl.jena.graph.Node>();
			Set<ColoredEdge> outgoingEdges = outgoingEdgesOf(node);
			for (ColoredEdge edge : outgoingEdges) {
				Node target = getEdgeTarget(edge);
				if(target instanceof VariableNode){
					objects.add(Var.alloc(target.getId()));
				} else if(target instanceof LiteralNode){
					subjects.add(com.hp.hpl.jena.graph.Node.createLiteral(target.getId()));
				} else {
					subjects.add(com.hp.hpl.jena.graph.Node.createURI(target.getId()));
				}
			}
			
			if(!(subjects.isEmpty() || objects.isEmpty())){
				for (com.hp.hpl.jena.graph.Node subject : subjects) {
					for (com.hp.hpl.jena.graph.Node object : objects) {
						Triple triple = Triple.create(subject, predicate, object);
						System.out.println(triple);
					}
				}
			}
		}
		
		return null;
	}
}
