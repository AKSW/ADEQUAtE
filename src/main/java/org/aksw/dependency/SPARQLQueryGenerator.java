package org.aksw.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.DependencyGraphGenerator;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.SPARQLGraph;
import org.aksw.dependency.graph.matching.NaiveSubgraphMatcher;
import org.aksw.dependency.graph.matching.SubGraphMatcher;

import com.hp.hpl.jena.query.Query;

public class SPARQLQueryGenerator {
	
	private Collection<Rule> rules;
	private Collection<Rule> appliedRules;
	Map<Rule, Set<Node>> ruleWithMarkedNodes;
	private SubGraphMatcher subGraphMatcher = new NaiveSubgraphMatcher();

	public SPARQLQueryGenerator(Collection<Rule> rules) {
		this.rules = rules;
		ruleWithMarkedNodes = new HashMap<Rule, Set<Node>>(rules.size());
	}
	
	public Query generateSPARQLQuery(String question){
		DependencyGraphGenerator dependencyGraphGenerator = new DependencyGraphGenerator();
		ColoredDirectedGraph dependencyGraph = dependencyGraphGenerator.generateDependencyGraph(question).toGeneralizedGraph();
		return generateSPARQLQuery(dependencyGraph);
	}
	
	public Query generateSPARQLQuery(ColoredDirectedGraph dependencyGraph){
		appliedRules = new ArrayList<Rule>();
		//for each rule r_i in R
		for(Rule rule : rules){
			ColoredDirectedGraph ruleBodyGraph = rule.getSource();
			//get all matching subgraphs
			Set<Set<Node>> matchingSubgraphs = subGraphMatcher.getMatchingSubgraphs(dependencyGraph, ruleBodyGraph);
			//try to find a matching subgraph which is not already covered by another rule
			for (Set<Node> subGraph : matchingSubgraphs) {
				if(!isCoveredByOtherRule(subGraph)){
					ruleWithMarkedNodes.put(rule, subGraph);
				}
			}
		}
		for (Rule rule : ruleWithMarkedNodes.keySet()) {
			ColoredDirectedGraph target = rule.getTarget();
			((SPARQLGraph)target).toSPARQLQuery();
		}
		return null;
	}
	
	private boolean isCoveredByOtherRule(Set<Node> subGraph){
		for (Entry<Rule, Set<Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Set<Node> otherSubGraph = entry.getValue();
			if(otherSubGraph.containsAll(subGraph)){
				return true;
			}
		}
		return false;
	}
	
	private boolean coversOtherRule(Set<Node> subGraph){
		for (Entry<Rule, Set<Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Set<Node> otherSubGraph = entry.getValue();
			if(subGraph.containsAll(otherSubGraph)){
				return true;
			}
		}
		return false;
	}
}
