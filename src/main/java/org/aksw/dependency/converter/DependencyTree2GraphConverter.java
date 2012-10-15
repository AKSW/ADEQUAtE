package org.aksw.dependency.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencyGraph;
import org.aksw.dependency.graph.DependencyNode;
import org.aksw.dependency.graph.Node;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;

public class DependencyTree2GraphConverter {
	
	public ColoredDirectedGraph getGraph(SemanticGraph dependencyGraph){
		ColoredDirectedGraph graph = new DependencyGraph();

		int i = 0;
	   for(SemanticGraphEdge edge : dependencyGraph.edgeListSorted()){
		   IndexedWord sourceWord = edge.getSource();
		   IndexedWord targetWord = edge.getTarget();
		   GrammaticalRelation relation = edge.getRelation();
		   
		   String color = "black";
		   
		   Node source = new DependencyNode(sourceWord.word(), sourceWord.tag());
		   Node target = new DependencyNode(targetWord.word(), targetWord.tag());
		   Node relationNode = new Node(relation.toString());
		   
		   graph.addVertex(source);
		   graph.addVertex(target);
		   graph.addVertex(relationNode);
		   
		   graph.addEdge(source, relationNode, new ColoredEdge("edge" + i++, color));
		   graph.addEdge(relationNode, target, new ColoredEdge("edge" + i++, color));
	   }
	   
	   return graph;
	}
	
	public ColoredDirectedGraph getGraph(SemanticGraph dependencyGraph, boolean pruned){
		ColoredDirectedGraph graph = new DependencyGraph();

		int i = 0;
		
		List<String> ignore = Arrays.asList(new String[]{"WP", "DT", "PRP"});
		Map<String, Integer> relationOccurenceCount = new HashMap<String, Integer>();
	   for(SemanticGraphEdge edge : dependencyGraph.edgeListSorted()){
		   IndexedWord sourceWord = edge.getSource();
		   IndexedWord targetWord = edge.getTarget();
		   GrammaticalRelation relation = edge.getRelation();
		   System.out.println(edge + "(" + edge.getSource() + "," + edge.getTarget() + ")");
		   
		   if(!pruned || (pruned && !ignore.contains(targetWord.tag()))){
			   String color = "black";
			   
			   Node source = new DependencyNode(sourceWord.word(), sourceWord.tag());
			   Node target = new DependencyNode(targetWord.word(), targetWord.tag());
			   String relationString = relation.toString();
			   Integer cnt = relationOccurenceCount.get(relationString);
			   if(cnt == null){
				   cnt = Integer.valueOf(1);
			   } else {
				   cnt++;
			   }
			   relationOccurenceCount.put(relationString, cnt);
			   relationString += "_" + cnt.toString();
			   Node relationNode = new Node(relationString);
			   
			   graph.addVertex(source);
			   graph.addVertex(target);
			   graph.addVertex(relationNode);
			   
			   graph.addEdge(source, relationNode, new ColoredEdge("edge" + i++, color));
			   graph.addEdge(relationNode, target, new ColoredEdge("edge" + i++, color));
		   } else {
			   System.out.println("Omitting edge:" + edge + "(" + edge.getSource() + "," + edge.getTarget() + ")");
		   }
	   }
	   System.out.println(graph);
	   
	   return graph;
	}
}
