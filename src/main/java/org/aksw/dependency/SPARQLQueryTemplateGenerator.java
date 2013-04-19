package org.aksw.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.dependency.graph.ClassNode;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencyGraphGenerator;
import org.aksw.dependency.graph.LiteralNode;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.PropertyNode;
import org.aksw.dependency.graph.ResourceNode;
import org.aksw.dependency.graph.VariableNode;
import org.aksw.dependency.graph.matching.NaiveSubgraphMatcher;
import org.aksw.dependency.graph.matching.SubGraphMatcher;
import org.aksw.dependency.template.Slot;
import org.aksw.dependency.template.SlotType;
import org.aksw.dependency.template.Template;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;

public class SPARQLQueryTemplateGenerator {
	
	private Collection<Rule> rules;
	private Collection<Rule> appliedRules;
	Map<Rule, Map<Node, Node>> ruleWithMarkedNodes;
	private SubGraphMatcher subGraphMatcher = new NaiveSubgraphMatcher();

	public SPARQLQueryTemplateGenerator(Collection<Rule> rules) {
		this.rules = rules;
		ruleWithMarkedNodes = new HashMap<Rule, Map<Node, Node>>(rules.size());
	}
	
	public Query generateSPARQLQuery(String question){
		DependencyGraphGenerator dependencyGraphGenerator = new DependencyGraphGenerator();
		ColoredDirectedGraph dependencyGraph = dependencyGraphGenerator.generateDependencyGraph(question).toGeneralizedGraph();
		return generateSPARQLQuery(dependencyGraph);
	}
	
	public Query generateSPARQLQuery(ColoredDirectedGraph dependencyGraph){
		//check for the (set of) rules which can be applied
		detectApplicableRules(dependencyGraph);
		//build the SPARQL query template
		buildSPARQLQueryTemplate();
		
		return null;
	}
	
	private void detectApplicableRules(ColoredDirectedGraph dependencyGraph){
		appliedRules = new ArrayList<Rule>();
		//for each rule r_i in R
		for(Rule rule : rules){
			ColoredDirectedGraph ruleBodyGraph = rule.getSource();
			//get all matching subgraphs
			Set<Map<Node, Node>> matchingSubgraphs = subGraphMatcher.getMatchingSubgraphs(dependencyGraph, ruleBodyGraph);
			//try to find a matching subgraph which is not already covered by another rule
			for (Map<Node, Node> subGraph : matchingSubgraphs) {System.out.println(subGraph);
				if(!isCoveredByOtherRule(subGraph)){
					ruleWithMarkedNodes.put(rule, subGraph);
				}
			}
		}
	}
	
	private void buildSPARQLQueryTemplate(){
		for (Entry<Rule, Map<Node, Node>> rule2SubGraph : ruleWithMarkedNodes.entrySet()) {
			Rule rule = rule2SubGraph.getKey();
			System.out.println("+++++++++++++++++++++++\nProcessing rule\n" + rule);
			Map<Node, Node> sourceNodes = rule2SubGraph.getValue();
			System.out.print("Source nodes: ");
			for (Entry<Node, Node> entry : sourceNodes.entrySet()) {
				System.out.print(entry + ", ");
			}System.out.println();
			ColoredDirectedGraph targetGraph = rule.getTarget();
			Map<Node, Node> mapping = rule.getMapping();
			
			for (Entry<Node, Node> entry : mapping.entrySet()) {
				Node key = entry.getKey();
				Node value = entry.getValue();
				System.out.println(key + "->" + value);
			}
			
			//replace each node in the target graph with the token of the mapped node in the source graph
			for (Entry<Node, Node> entry : sourceNodes.entrySet()) {
				Node sourceNode = entry.getKey();
				Node sourceRuleNode = entry.getValue();
				Node targetNode = mapping.get(sourceRuleNode);
				if(targetNode != null){
					System.out.println(sourceNode.getId() + " replaces " + targetNode);
					targetNode.setLabel(sourceNode.getId());
					for(Node n : targetGraph.vertexSet()){
						if(n.equals(targetNode)){
							n.setLabel(sourceNode.getId());
						}
					}
//					System.out.println(targetNode.getLabel());
//					System.out.println(targetGraph.containsVertex(targetNode));
				}
			}
			System.out.println(targetGraph);
			//get all nodes in graph
			Set<Node> nodes = targetGraph.vertexSet();
			//firstly, find all nodes which represent a property
			Set<Node> propertyNodes = new HashSet<Node>();
			for (Node node : nodes) {
				if(node instanceof PropertyNode){
					propertyNodes.add(node);
				}
			}
			
			int literals = 0;
			int classes = 0;
			int resources = 0;
			int properties = 0;
			Collection<Slot> slots = new HashSet<Slot>();
			Set<Triple> triples = new HashSet<Triple>();
			//create the triples by starting from the property nodes
			for (Node propertyNode : propertyNodes) {
				//the predicate
				SlotType slotType = SlotType.PROPERTY;
				String varName = slotType.getPlaceHolder() + properties++;
				String token = propertyNode.getLabel();
				slots.add(new Slot(varName, token, slotType));
				com.hp.hpl.jena.graph.Node predicate = Var.alloc(varName);
//				com.hp.hpl.jena.graph.Node predicate = com.hp.hpl.jena.graph.Node.createURI(propertyNode.getLabel());
				//the subjects
				List<com.hp.hpl.jena.graph.Node> subjects = new ArrayList<com.hp.hpl.jena.graph.Node>();
				Set<ColoredEdge> incomingEdges = targetGraph.incomingEdgesOf(propertyNode);
				for (ColoredEdge edge : incomingEdges) {
					Node sourceNode = targetGraph.getEdgeSource(edge);
					if(sourceNode instanceof VariableNode){
						subjects.add(Var.alloc(sourceNode.getId()));
					} else {
						slotType = SlotType.RESOURCE;
						varName = slotType.getPlaceHolder() + resources++;
						token = sourceNode.getLabel();
						slots.add(new Slot(varName, token, slotType));
						subjects.add(Var.alloc(varName));
//						subjects.add(com.hp.hpl.jena.graph.Node.createURI(sourceNode.getLabel()));
					}
				}
				//the objects
				List<com.hp.hpl.jena.graph.Node> objects = new ArrayList<com.hp.hpl.jena.graph.Node>();
				Set<ColoredEdge> outgoingEdges = targetGraph.outgoingEdgesOf(propertyNode);
				for (ColoredEdge edge : outgoingEdges) {
					Node targetNode = targetGraph.getEdgeTarget(edge);
					if(targetNode instanceof VariableNode){
						objects.add(Var.alloc(targetNode.getId()));
					} else if(targetNode instanceof LiteralNode){
						slotType = SlotType.LITERAL;
						varName = slotType.getPlaceHolder() + literals++;
						token = targetNode.getLabel();
						slots.add(new Slot(varName, token, slotType));
						objects.add(Var.alloc(varName));
//						objects.add(com.hp.hpl.jena.graph.Node.createLiteral(targetNode.getLabel()));
					} else if(targetNode instanceof ClassNode){
						slotType = SlotType.CLASS;
						varName = slotType.getPlaceHolder() + classes++;
						token = targetNode.getLabel();
						slots.add(new Slot(varName, token, slotType));
						objects.add(Var.alloc(varName));
//						objects.add(com.hp.hpl.jena.graph.Node.createURI(targetNode.getLabel()));
					} else if(targetNode instanceof ResourceNode){
						slotType = SlotType.RESOURCE;
						varName = slotType.getPlaceHolder() + resources++;
						token = targetNode.getLabel();
						slots.add(new Slot(varName, token, slotType));
						objects.add(Var.alloc(varName));
//						objects.add(com.hp.hpl.jena.graph.Node.createURI(targetNode.getLabel()));
					}
				}
				
				
				if(!(subjects.isEmpty() || objects.isEmpty())){
					for (com.hp.hpl.jena.graph.Node subject : subjects) {
						for (com.hp.hpl.jena.graph.Node object : objects) {
							Triple triple = Triple.create(subject, predicate, object);
							triples.add(triple);
						}
					}
				}
				
			}
			//create templates only if there is at least one triple pattern contained
			if(!triples.isEmpty()){
				ElementTriplesBlock block = new ElementTriplesBlock();
				for (Triple triple : triples) {
					block.addTriple(triple);
				}
				ElementGroup body = new ElementGroup();
				body.addElement(block);
				Query query = QueryFactory.create();
				query.setQuerySelectType();
				query.setQueryResultStar(true);
				query.setQueryPattern(body);
				
				Template template = new Template(query, slots);
				System.out.println(template);
			}
		}
	}
	
	private boolean isCoveredByOtherRule(Map<Node, Node> subGraph){
		for (Entry<Rule, Map<Node, Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Map<Node, Node> otherSubGraph = entry.getValue();
			if(otherSubGraph.keySet().containsAll(subGraph.keySet())){
				return true;
			}
		}
		return false;
	}
	
	private boolean coversOtherRule(Map<Node, Node> subGraph){
		for (Entry<Rule, Map<Node, Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Map<Node, Node> otherSubGraph = entry.getValue();
			if(subGraph.keySet().containsAll(otherSubGraph.keySet())){
				return true;
			}
		}
		return false;
	}
}
