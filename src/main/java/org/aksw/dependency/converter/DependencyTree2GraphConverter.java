package org.aksw.dependency.converter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencyGraph;
import org.aksw.dependency.graph.DependencyGraphNode;
import org.aksw.dependency.graph.Node;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;

public class DependencyTree2GraphConverter {

	private static final Logger logger = Logger.getLogger(DependencyTree2GraphConverter.class);

	public ColoredDirectedGraph getGraph(SemanticGraph dependencyGraph) {
		ColoredDirectedGraph graph = new DependencyGraph();

		for (SemanticGraphEdge edge : dependencyGraph.edgeListSorted()) {
			IndexedWord sourceWord = edge.getSource();
			IndexedWord targetWord = edge.getTarget();
			GrammaticalRelation relation = edge.getRelation();

			String color = "black";

			Node source = new DependencyGraphNode(sourceWord.word(), sourceWord.tag());
			Node target = new DependencyGraphNode(targetWord.word(), targetWord.tag());
			Node relationNode = new Node(relation.toString());

			graph.addVertex(source);
			graph.addVertex(target);
			graph.addVertex(relationNode);

			graph.addEdge(source, relationNode, new ColoredEdge("edge", color));
			graph.addEdge(relationNode, target, new ColoredEdge("edge", color));
		}

		return graph;
	}

	public ColoredDirectedGraph getGraph(SemanticGraph dependencyGraph, boolean pruned) {
		ColoredDirectedGraph graph = new DependencyGraph();

		List<String> ignoreWordsWithTag = Arrays.asList(new String[] { "WP", "DT", "PRP"});
		List<String> ignoreRelations = Arrays.asList(new String[]{"conj_and"});
		Map<String, Integer> relationOccurenceCount = new HashMap<String, Integer>();
		for (SemanticGraphEdge edge : dependencyGraph.edgeListSorted()) {
			IndexedWord sourceWord = edge.getSource();
			IndexedWord targetWord = edge.getTarget();
			GrammaticalRelation relation = edge.getRelation();

			if (!pruned || (pruned && !ignoreWordsWithTag.contains(targetWord.tag()) && !ignoreRelations.contains(relation.toString()))) {
				String color = "black";

				Node source = new DependencyGraphNode(sourceWord.word(), sourceWord.tag());
				Node target = new DependencyGraphNode(targetWord.word(), targetWord.tag());
				String relationString = relation.toString();
				Integer cnt = relationOccurenceCount.get(relationString);
				if (cnt == null) {
					cnt = Integer.valueOf(1);
				} else {
					cnt++;
				}
				relationOccurenceCount.put(relationString, cnt);
				relationString += "_" + cnt.toString();
				Node relationNode = new Node(relationString, relation.toString());

				graph.addVertex(source);
				graph.addVertex(target);
				graph.addVertex(relationNode);

				graph.addEdge(source, relationNode, new ColoredEdge("edge", color));
				graph.addEdge(relationNode, target, new ColoredEdge("edge", color));
			} else {
				logger.trace("Omitting edge:" + edge + "(" + edge.getSource() + "," + edge.getTarget() + ")");
			}
		}

		return graph;
	}
}
