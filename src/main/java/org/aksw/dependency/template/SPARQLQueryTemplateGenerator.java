package org.aksw.dependency.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.dependency.graph.ClassNode;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencyGraph;
import org.aksw.dependency.graph.DependencyGraphGenerator;
import org.aksw.dependency.graph.LiteralNode;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.PropertyNode;
import org.aksw.dependency.graph.ResourceNode;
import org.aksw.dependency.graph.VariableNode;
import org.aksw.dependency.graph.matching.NaiveSubgraphMatcher;
import org.aksw.dependency.graph.matching.SubGraphMatcher;
import org.aksw.dependency.rule.Rule;
import org.aksw.dependency.rule.RuleModel;
import org.aksw.dependency.rule.sort.FrequencySort;
import org.aksw.dependency.rule.sort.RuleSort;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;

public class SPARQLQueryTemplateGenerator {
	
	
	private static final Logger logger = Logger.getLogger(SPARQLQueryTemplateGenerator.class.getName());
	
	private Collection<Rule> appliedRules;
	private Map<Rule, Map<Node, Node>> ruleWithMarkedNodes;
	private SubGraphMatcher subGraphMatcher = new NaiveSubgraphMatcher();
	
	private RuleModel ruleModel;
	
	private int literals = 0;
	private int classes = 0;
	private int resources = 0;
	private int properties = 0;
	private int variables = 0;
	private Map<Node, Slot> slotCache = new HashMap<>();
	private Map<Node, Var> varCache = new HashMap<>();
	
	public SPARQLQueryTemplateGenerator(RuleModel ruleModel) {
		this.ruleModel = ruleModel;
		ruleWithMarkedNodes = new LinkedHashMap<Rule, Map<Node, Node>>(ruleModel.getRules().size());
	}

	public SPARQLQueryTemplateGenerator(Collection<Rule> rules) {
		this.ruleModel = new RuleModel(rules);
		ruleWithMarkedNodes = new LinkedHashMap<Rule, Map<Node, Node>>(rules.size());
	}
	
	public Collection<Template> generateSPARQLQueryTemplates(String question){
		DependencyGraphGenerator dependencyGraphGenerator = new DependencyGraphGenerator();	
		ColoredDirectedGraph dependencyGraph = dependencyGraphGenerator.generateDependencyGraph(question).toGeneralizedGraph();
		return generateSPARQLQueryTemplates(dependencyGraph);
	}
	
	public Collection<Template> generateSPARQLQueryTemplates(ColoredDirectedGraph dependencyGraph){
		reset();
		
		//check for the (set of) rules which can be applied
		detectApplicableRules(dependencyGraph);
		
		//build the SPARQL query template
		Collection<Template> templates = buildSPARQLQueryTemplates();
		
		return templates;
	}
	
	private void reset(){
		ruleWithMarkedNodes.clear();
		slotCache.clear();
		varCache.clear();
		literals = 0;
		classes = 0;
		resources = 0;
		properties = 0;
		variables = 0;
	}
	
	
	private void detectApplicableRules(ColoredDirectedGraph dependencyGraph){
		logger.info("Dependency graph:\n" + dependencyGraph.prettyPrint());
		logger.info("Detecting applicable rules...");
		appliedRules = new ArrayList<Rule>();
		//for each rule r_i in R
		for(Rule rule : ruleModel.getRules()){
			ColoredDirectedGraph ruleBodyGraph = rule.getSource();
			//get all matching subgraphs
			Set<Map<Node, Node>> matchingSubgraphs = subGraphMatcher.getMatchingSubgraphs(dependencyGraph, ruleBodyGraph);
			//try to find a matching subgraph which is not already covered by another rule
			for (Map<Node, Node> subGraph : matchingSubgraphs) {
				Rule otherRule = coversOtherRule(subGraph);
				if(otherRule != null){
					ruleWithMarkedNodes.remove(otherRule);
					if(!isCoveredByOtherRule(subGraph))
						ruleWithMarkedNodes.put(rule, subGraph);
				} else if(!isCoveredByOtherRule(subGraph)){
					ruleWithMarkedNodes.put(rule, subGraph);
				}
			}
		}
		logger.info("...found " + ruleWithMarkedNodes.size() + " rules.");
	}
	
	private Collection<Template> buildSPARQLQueryTemplates(){
		Collection<Template> templates = new ArrayList<>();
		Collection<Slot> slots = new HashSet<Slot>();
		Set<Triple> triples = new HashSet<Triple>();
		for (Entry<Rule, Map<Node, Node>> rule2SubGraph : ruleWithMarkedNodes.entrySet()) {
			Rule rule = rule2SubGraph.getKey();
			logger.info("Applying rule\n" + rule);
			Map<Node, Node> sourceNodes = rule2SubGraph.getValue();
			
			ColoredDirectedGraph targetGraph = rule.getTarget();
			Map<Node, Node> mapping = rule.getMapping();
			
			for (Entry<Node, Node> entry : mapping.entrySet()) {
				Node key = entry.getKey();
				Node value = entry.getValue();
			}
			
			//replace each node in the target graph with the token of the mapped node in the source graph
			for (Entry<Node, Node> entry : sourceNodes.entrySet()) {
				Node sourceNode = entry.getKey();
				Node sourceRuleNode = entry.getValue();
				Node targetNode = mapping.get(sourceRuleNode);
				if(targetNode != null){
//					System.out.println(sourceNode.getId() + " replaces " + targetNode);
					targetNode.setLabel(sourceNode.getId());
					for(Node n : targetGraph.vertexSet()){
						if(n.equals(targetNode)){
							n.setLabel(sourceNode.getId());
						}
					}
				}
			}
			//get all nodes in graph
			Set<Node> nodes = targetGraph.vertexSet();
			//firstly, find all nodes which represent a property
			Set<Node> propertyNodes = new HashSet<Node>();
			for (Node node : nodes) {
				if(node instanceof PropertyNode){
					propertyNodes.add(node);
				}
			}
			
			int properties = 0;
			//create the triples by starting from the property nodes
			for (Node propertyNode : propertyNodes) {
				//the predicate
				SlotType slotType = SlotType.PROPERTY;
				String varName = slotType.getPlaceHolder() + properties++;
				String token = propertyNode.getLabel();
				Slot slot;
				if(!token.equals("rdf:type")){
					slots.add(new Slot(varName, token, slotType));
				}
				com.hp.hpl.jena.graph.Node predicate = Var.alloc(varName);
//				com.hp.hpl.jena.graph.Node predicate = com.hp.hpl.jena.graph.Node.createURI(propertyNode.getLabel());
				//the subjects
				List<com.hp.hpl.jena.graph.Node> subjects = new ArrayList<com.hp.hpl.jena.graph.Node>();
				Set<ColoredEdge> incomingEdges = targetGraph.incomingEdgesOf(propertyNode);
				for (ColoredEdge edge : incomingEdges) {
					Node sourceNode = targetGraph.getEdgeSource(edge);
					if(sourceNode instanceof VariableNode){
						subjects.add(getVar(sourceNode));
					} else {
						slot = getSlot(sourceNode);
						slots.add(slot);
						subjects.add(Var.alloc(slot.getAnchor()));
//						subjects.add(com.hp.hpl.jena.graph.Node.createURI(sourceNode.getLabel()));
					}
				}
				//the objects
				List<com.hp.hpl.jena.graph.Node> objects = new ArrayList<com.hp.hpl.jena.graph.Node>();
				Set<ColoredEdge> outgoingEdges = targetGraph.outgoingEdgesOf(propertyNode);
				for (ColoredEdge edge : outgoingEdges) {
					Node targetNode = targetGraph.getEdgeTarget(edge);
					if(targetNode instanceof VariableNode){
						objects.add(getVar(targetNode));
					} else {
						slot = getSlot(targetNode);
						slots.add(slot);
						objects.add(Var.alloc(slot.getAnchor()));
						//if object is class then predicate is always rdf:type
						if(targetNode instanceof ClassNode){
							predicate = RDF.type.asNode();
						}
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
			templates.add(template);
		}
		return templates;
	}
	
	private Var getVar(Node node){
		Var var = varCache.get(node);
		if(var == null){
			var = Var.alloc("var" + variables++);
			varCache.put(node, var);
		}
		return var;
	}
	
	private Slot getSlot(Node node){
		Slot slot = slotCache.get(node);
		if(slot == null){
			String varName;
			String token;
			SlotType slotType;
			if(node instanceof LiteralNode){
				slotType = SlotType.LITERAL;
				varName = slotType.getPlaceHolder() + literals++;
				token = node.getLabel();
				slot = new Slot(varName, token, slotType);
			} else if(node instanceof ClassNode){
				slotType = SlotType.CLASS;
				varName = slotType.getPlaceHolder() + classes++;
				token = node.getLabel();
				slot = new Slot(varName, token, slotType);
			} else if(node instanceof ResourceNode){
				slotType = SlotType.RESOURCE;
				varName = slotType.getPlaceHolder() + resources++;
				token = node.getLabel();
				slot = new Slot(varName, token, slotType);
			}
			slotCache.put(node, slot);
		}
		return slot;
	}
	
	private boolean isCoveredByOtherRule(Map<Node, Node> subGraph){
		for (Entry<Rule, Map<Node, Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Map<Node, Node> otherSubGraph = entry.getValue();
			if(subGraph.keySet().size() <= otherSubGraph.keySet().size()){
//				System.out.println("Graph\n" + asTreeSet(subGraph.keySet()) + " is covered by other\n" + asTreeSet(otherSubGraph.keySet()));
				if(otherSubGraph.keySet().containsAll(subGraph.keySet())){
					return true;
				}
			}
		}
		return false;
	}
	
	private Rule coversOtherRule(Map<Node, Node> subGraph){
		for (Entry<Rule, Map<Node, Node>> entry : ruleWithMarkedNodes.entrySet()) {
			Rule rule = entry.getKey();
			Map<Node, Node> otherSubGraph = entry.getValue();
			if(subGraph.keySet().size() >= otherSubGraph.keySet().size()){
//				System.out.println("Graph\n" + asTreeSet(subGraph.keySet()) + " covers other\n" + asTreeSet(otherSubGraph.keySet()));
				if(subGraph.keySet().containsAll(otherSubGraph.keySet())){
					return rule;
				}
			}
		}
		return null;
	}
	
	private TreeSet<Node> asTreeSet(Collection<Node> nodes){
		TreeSet<Node> treeSet = new TreeSet<Node>(new NodeIdComparator());
		treeSet.addAll(nodes);
		return treeSet;
	}
	
	class NodeIdComparator implements Comparator<Node>{

		@Override
		public int compare(Node n1, Node n2) {
			return n1.getId().compareTo(n2.getId());
		}
		
	}
}
