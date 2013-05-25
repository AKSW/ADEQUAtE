package org.aksw.dependency.rule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aksw.dependency.converter.SPARQLQuery2GraphConverter;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredDirectedSubgraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencyGraphGenerator;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.PropertyNode;
import org.aksw.dependency.rule.clustering.OptimizedRuleClustering;
import org.aksw.dependency.rule.clustering.RuleClustering;
import org.aksw.dependency.util.Generalization;
import org.aksw.dependency.util.GraphUtils;
import org.aksw.dependency.util.ManualMapping;
import org.aksw.dependency.util.Matcher;
import org.aksw.dependency.util.StableMarriage;
import org.apache.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.Var;

public class RuleGenerator {
	
	private static final Logger logger = Logger.getLogger(RuleGenerator.class);
	
	private Table<Integer, String, String> trainData = HashBasedTable.create();

	private String endpointURL;
	
	private ManualMapping manualMapping;
	
	private int currentQueryId;
	private StringBuilder matchingOutput = new StringBuilder();
	
	private static final File ruleCacheDirectory = new File("rule-cache");
	
	public RuleGenerator(String endpointURL) {
		this.endpointURL = endpointURL;
		
		ruleCacheDirectory.mkdirs();
	}
	
	public ColoredDirectedGraph generateSPARQLQueryGraph(String queryString){
		SPARQLQuery2GraphConverter sparqlConverter = new SPARQLQuery2GraphConverter(endpointURL);
		Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
		List<Var> projectVars = query.getProjectVars();//TODO How to handle it, when there are more than 1 variables?
		ColoredDirectedGraph directedGraph = sparqlConverter.getGraph(query);
		return directedGraph;
	}
	
	public Map<String, Collection<Rule>> computeMappingRules(String question, String queryString){
		return computeMappingRules(question, queryString, new HashMap<String, String>());
	}
	
	private Collection<Rule> readFromDisk(String question, String queryString){
		Collection<Rule> rules = null;
		
		HashFunction md5 = Hashing.md5();
		String hash = md5.newHasher().putString(question).putString(queryString).hash().toString();
		File file = new File(ruleCacheDirectory, hash + ".ser");
		if(file.exists()){
			InputStream fis = null;
			try {
				fis = new FileInputStream(file);
				ObjectInputStream o = new ObjectInputStream(fis);
				rules = (Collection<Rule>) o.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.err.println(e);
			} finally {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
		return rules;
	}
	
	private void writeToDisk(Collection<Rule> rules, String question, String queryString){
		HashFunction md5 = Hashing.md5();
		String hash = md5.newHasher().putString(question).putString(queryString).hash().toString();
		File file = new File(ruleCacheDirectory, hash + ".ser");
		OutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			ObjectOutputStream o = new ObjectOutputStream(fos);
			o.writeObject(rules);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}
	
	public Map<String, Collection<Rule>> computeMappingRules(String question, String queryString, Map<String, String> manualMapping){
		//try to read the rules from cache
		Collection<Rule> learnedRules = readFromDisk(question, queryString);
		
		if(learnedRules == null){
			logger.info("Computing rules for \"" + question + "\"");
			learnedRules = new HashSet<>();
			//generate the directed graph for the dependencies of the question
			DependencyGraphGenerator dependencyGraphGenerator = new DependencyGraphGenerator();
			ColoredDirectedGraph graph1 = dependencyGraphGenerator.generateDependencyGraph(question);
//			GraphUtils.paint(graph1, "Dependency Graph");
//			GraphUtils.paint(graph1.toGeneralizedGraph(), "Dependency Graph");
			
			//generate the directed graph for the SPARQL query
			ColoredDirectedGraph graph2 = generateSPARQLQueryGraph(queryString);
//			GraphUtils.paint(graph2, "SPARQL Query Graph");
//			GraphUtils.paint(graph2.toGeneralizedGraph(), "SPARQL Query Graph");
			
			//compute all subgraphs for the first graph
			Collection<ColoredDirectedSubgraph> subgraphs1 = GraphUtils.getSubgraphs(graph1);
			
			//compute all subgraphs for the second graph
			Collection<ColoredDirectedSubgraph> subgraphs2 = GraphUtils.getSubgraphs(graph2);
			
			//prune the set of subgraphs
			ColoredDirectedSubgraph sub;
			for(Iterator<ColoredDirectedSubgraph> iter = subgraphs1.iterator(); iter.hasNext();){
				sub = iter.next();
				//remove subgraph if it contains only a single node
				if(sub.edgeSet().isEmpty()){
					iter.remove();
				}
			}
			for(Iterator<ColoredDirectedSubgraph> iter = subgraphs2.iterator(); iter.hasNext();){
				sub = iter.next();
				//remove subgraph if it contains only a single node
				if(sub.edgeSet().isEmpty()){
					iter.remove();
				}
			}
			
			// expand the subgraphs such that complete triple patterns are contained
			for (ColoredDirectedSubgraph subGraph : subgraphs2) {
				try {
					for (Node node : new HashSet<Node>(subGraph.vertexSet())) {
						if (node instanceof PropertyNode) {
							//missing incoming links
							if (subGraph.inDegreeOf(node) == 0) {
								Set<ColoredEdge> incomingEdges = graph2.incomingEdgesOf(node);
								for (ColoredEdge edge : incomingEdges) {
									Node source = graph2.getEdgeSource(edge);
									subGraph.addVertex(source);
									subGraph.addEdge(source, node, edge);
								}
							}
							//missing outgoing links
							if (subGraph.outDegreeOf(node) == 0) {
								Set<ColoredEdge> outgoingEdges = graph2.outgoingEdgesOf(node);
								for (ColoredEdge edge : outgoingEdges) {
									Node target = graph2.getEdgeTarget(edge);
									subGraph.addVertex(target);
									subGraph.addEdge(node, target, edge);
								}
							}
						}
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}
			//has to be done because the hashcodes are not updated after modifiying the graphs
			subgraphs2 = new HashSet<>(subgraphs2);
			
			//find the mappings between nodes in dependency graph and SPARQL query graph, if exist
			//we start from the URIs in the SPARQL query and try to find the corresponding token in the question, makes more sense
			Matcher matcher = new StableMarriage();
			BiMap<Node, Node> matching = matcher.computeMatching(graph2, graph1, manualMapping);
			logger.info("Matching:");
			for(Entry<Node ,Node> entry : matching.entrySet()){
				logger.info(entry.getKey() + "\t->\t" + entry.getValue());
				matchingOutput.append(currentQueryId + "," + entry.getKey() + "," + entry.getValue()).append("\n");
			}
			
//			//filter out graphs which do not contain at least one node involved in a matching
//			for(Iterator<ColoredDirectedSubgraph> iter = subgraphs1.iterator(); iter.hasNext();){
//				sub = iter.next();
//				Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
//				nodes.retainAll(matching.keySet());
//				if(nodes.size() == 0){
//					iter.remove();
//				}
//			}
//			for(Iterator<ColoredDirectedSubgraph> iter = subgraphs2.iterator(); iter.hasNext();){
//				sub = iter.next();
//				Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
//				nodes.retainAll(matching.values());
//				if(nodes.size() == 0){
//					iter.remove();
//				}
//			}
//			
//			//1. we only match graphs g1 to graphs g2 if for each source node n_s in g1 there exists the corresponding target node n_t in g2 and vice versa
			for(ColoredDirectedSubgraph sub1 : subgraphs1){
				for(ColoredDirectedSubgraph sub2 : subgraphs2){
					boolean candidate = true;
					Map<Node, Node> mapping = new HashMap<Node, Node>();
					for(Node sourceNode : sub1.vertexSet()){
						Node targetNode = matching.get(sourceNode);
						if(targetNode != null){
							if(!sub2.vertexSet().contains(targetNode)){
								candidate = false;
								break;
							}
							mapping.put(sourceNode, targetNode);
						}
					}
					for(Node sourceNode : sub2.vertexSet()){
						Node targetNode = matching.inverse().get(sourceNode);
						if(targetNode != null){
							if(!sub1.vertexSet().contains(targetNode)){
								candidate = false;
								break;
							}
							mapping.put(targetNode, sourceNode);
						}
					}
					if(candidate){
						Generalization gen1 = sub1.generalize();
						Generalization gen2 = sub2.generalize();
						Map<Node, Node> generalizedMapping = new HashMap<Node, Node>();
						for(Entry<Node, Node> entry : mapping.entrySet()){
							generalizedMapping.put(gen1.getMapping().get(entry.getKey()), gen2.getMapping().get(entry.getValue()));
						}
//						
						learnedRules.add(new Rule(gen1.getGeneralizedGraph(), gen2.getGeneralizedGraph(), generalizedMapping));
					}
				}
			}
			writeToDisk(learnedRules, question, queryString);
		}
		
		Map<String, Collection<Rule>> result = new HashMap<>();
		result.put(question, learnedRules);
		return result;
	}
	
	private String getLabel(URI uri){
		String rendering = uri.getFragment();
        if (rendering != null && rendering.length()>0) {
            return rendering;
        }
        else {
            String s = uri.toString();
            int lastSlashIndex = s.lastIndexOf('/');
            if(lastSlashIndex != -1 && lastSlashIndex != s.length() - 1) {
                return s.substring(lastSlashIndex + 1);
            }
        }
        return uri.toString();
	}
	
	private Map<String, Collection<Rule>> generateRules() {
		logger.info("Generating rules...");
		Map<String, Collection<Rule>> rules = new LinkedHashMap<>();
		ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<Map<String, Collection<Rule>>>> futures = new ArrayList<Future<Map<String, Collection<Rule>>>>();

		for (Cell<Integer, String, String> row : trainData.cellSet()){
			currentQueryId = row.getRowKey();
			
			int id = currentQueryId;
			final String question = row.getColumnKey();
			final String sparqlQuery = row.getValue();
			
			final Map<String, String> mapping;
			if(manualMapping == null){
				mapping = new HashMap<String, String>();
			} else {
				if(manualMapping.containsMapping(id)){
					mapping = manualMapping.getMapping(id);
				} else {
					mapping = new HashMap<String, String>();
				}
			}
			futures.add(threadPool.submit(new Callable<Map<String, Collection<Rule>>>() {

				@Override
				public Map<String, Collection<Rule>> call() throws Exception {
					return computeMappingRules(question, sparqlQuery, mapping);
				}
			}));
		
		}
		for (Future<Map<String, Collection<Rule>>> future : futures) {
			try {
				rules.putAll(future.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		Collection<Rule> allRules = new HashSet<>();
		for (Collection<Rule> r : rules.values()) {
			allRules.addAll(r);
		}
		logger.info("...got " + allRules.size() + " rules.");
		return rules;
	}
	
	public RuleModel generateRuleModel(Table<Integer, String, String> trainData, ManualMapping manualMapping){
		this.trainData = trainData;
		this.manualMapping = manualMapping;
		
		//generate for each question a set of rules
		Map<String, Collection<Rule>> rules = generateRules();
		
		//cluster the rules, i.e. generate cluster with frequency for rules which generalized form is equal
		RuleClustering ruleClustering = new OptimizedRuleClustering();
//		RuleClustering ruleClustering = new SimpleRuleClustering();
		Map<Rule, Integer> clusteredRules = ruleClustering.clusterRules(rules);
		
		return new RuleModel(clusteredRules.keySet());
	}
	
	public RuleModel generateRuleModel(Table<Integer, String, String> trainData, Map<Integer, BiMap<String, String>> manualMapping) {
		return generateRuleModel(trainData, new ManualMapping(manualMapping));
	}
	
	public static <K, V> void printTopKEntries(List<Entry<K, V>> entries, int k){
		for(int i = 0; i < Math.min(k, entries.size()); i++){
			logger.info(entries.get(i).getValue());
		}
	}
	
	
	
}
