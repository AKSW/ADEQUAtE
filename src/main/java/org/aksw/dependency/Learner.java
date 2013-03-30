package org.aksw.dependency;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.aksw.dependency.converter.DependencyTree2GraphConverter;
import org.aksw.dependency.converter.SPARQLQuery2GraphConverter;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredDirectedSubgraph;
import org.aksw.dependency.graph.DependencyGraphGenerator;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.util.Generalization;
import org.aksw.dependency.util.GraphUtils;
import org.aksw.dependency.util.ManualMapping;
import org.aksw.dependency.util.Matcher;
import org.aksw.dependency.util.SimPackGraphWrapper;
import org.aksw.dependency.util.StableMarriage;
import org.apache.log4j.Logger;

import simpack.measure.graph.GraphIsomorphism;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.Var;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class Learner {
	
	private static final Logger logger = Logger.getLogger(Learner.class);
	
	private Table<Integer, String, String> trainData = HashBasedTable.create();

	private String endpointURL;
	
	private ManualMapping manualMapping = new ManualMapping("resources/keyword-uri-mapping-qald2-dbpedia-train.csv");
	
	private int currentQueryId;
	private StringBuilder matchingOutput = new StringBuilder();
	
	public Learner(String endpointURL) {
		this.endpointURL = endpointURL;
	}
	
	public ColoredDirectedGraph generateSPARQLQueryGraph(String queryString){
		SPARQLQuery2GraphConverter sparqlConverter = new SPARQLQuery2GraphConverter(endpointURL);
		Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
		List<Var> projectVars = query.getProjectVars();//TODO How to handle it, when there are more than 1 variables?
		ColoredDirectedGraph directedGraph = sparqlConverter.getGraph(query);
		return directedGraph;
	}
	
	public Collection<Rule> computeMappingRules(String question, String queryString){
		return computeMappingRules(question, queryString, new HashMap<String, String>());
	}
	
	public Collection<Rule> computeMappingRules(String question, String queryString, Map<String, String> manualMapping){
		//generate the directed graph for the dependencies of the question
		DependencyGraphGenerator dependencyGraphGenerator = new DependencyGraphGenerator();
		ColoredDirectedGraph graph1 = dependencyGraphGenerator.generateDependencyGraph(question);
//		GraphUtils.paint(graph1, "Dependency Graph");
//		GraphUtils.paint(graph1.toGeneralizedGraph(), "Dependency Graph");
		
		//generate the directed graph for the SPARQL query
		ColoredDirectedGraph graph2 = generateSPARQLQueryGraph(queryString);
//		GraphUtils.paint(graph2, "SPARQL Query Graph");
//		GraphUtils.paint(graph2.toGeneralizedGraph(), "SPARQL Query Graph");
		
		//compute all subgraphs for the first graph
		Collection<ColoredDirectedSubgraph> subgraphs1 = GraphUtils.getSubgraphs(graph1);
		
		//compute all subgraphs for the second graph
		Collection<ColoredDirectedSubgraph> subgraphs2 = GraphUtils.getSubgraphs(graph2);
//		for(ColoredDirectedSubgraph g : subgraphs2){
//			System.out.println(g);
//		}
//		System.exit(0);
		
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
		
		//find the mappings between nodes in dependency graph and SPARQL query graph, if exist
		//we start from the URIs in the SPARQL query and try to find the corresponding token in the question, makes more sense
		Matcher matcher = new StableMarriage();
		BiMap<Node, Node> matching = matcher.computeMatching(graph2, graph1, manualMapping);
		logger.info("Matching:");
		for(Entry<Node ,Node> entry : matching.entrySet()){
			logger.info(entry.getKey() + "\t->\t" + entry.getValue());
			matchingOutput.append(currentQueryId + "," + entry.getKey() + "," + entry.getValue()).append("\n");
		}
		
//		//filter out graphs which do not contain at least one node involved in a matching
//		for(Iterator<ColoredDirectedSubgraph> iter = subgraphs1.iterator(); iter.hasNext();){
//			sub = iter.next();
//			Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
//			nodes.retainAll(matching.keySet());
//			if(nodes.size() == 0){
//				iter.remove();
//			}
//		}
//		for(Iterator<ColoredDirectedSubgraph> iter = subgraphs2.iterator(); iter.hasNext();){
//			sub = iter.next();
//			Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
//			nodes.retainAll(matching.values());
//			if(nodes.size() == 0){
//				iter.remove();
//			}
//		}
//		
//		//1. we only match graphs g1 to graphs g2 if for each source node n_s in g1 there exists the corresponding target node n_t in g2 and vice versa
		Set<Rule> learnedRules = new HashSet<Rule>();
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
					learnedRules.add(new Rule(gen1.getGeneralizedGraph(), gen2.getGeneralizedGraph(), generalizedMapping));
				}
			}
		}
		
		
		return learnedRules;
	}
	
	private Map<Rule, Integer> clusterRules(Collection<Rule> rules){
		Map<Rule, Integer> rule2Frequency = new HashMap<Rule, Integer>();
		for(Rule rule : rules){
			int cnt = 0;
			Rule r = null;
			for(Entry<Rule, Integer> entry : rule2Frequency.entrySet()){
				ColoredDirectedGraph currentGraph = rule.asConnectedGraph();
				ColoredDirectedGraph graph = entry.getKey().asConnectedGraph();
				
				boolean sameNodeSet = containSameNodes(currentGraph, graph);
				if(!sameNodeSet){
					continue;
				}
				
				if((currentGraph.edgeSet().size() != graph.edgeSet().size())){
					continue;
				}
				
//				GraphIsomorphismInspector<ColoredDirectedGraph> iso = AdaptiveIsomorphismInspectorFactory.
//						createIsomorphismInspector(currentGraph, graph, null, null);
//				System.out.println(iso.isIsomorphic());
				
				GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(currentGraph), SimPackGraphWrapper.getGraph(graph));
				gi.calculate();
		        r = null;
		        if(gi.getGraphIsomorphism() == 1){
		        	cnt = entry.getValue()+1;
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(cnt > 1){
				if(r != null){
					rule2Frequency.remove(r);
				}
				rule2Frequency.put(rule, Integer.valueOf(cnt));
			} else {
				rule2Frequency.put(rule, Integer.valueOf(1));
			}
			
		}
		return rule2Frequency;
	}
	
	private Map<Rule, Integer> computeRuleFrequency(Map<Rule, Integer> rules){
		return computeRuleFrequency(rules, new HashMap<Rule, Integer>());
	}
	
	private Map<Rule, Integer> computeRuleFrequency(Map<Rule, Integer> rules1, Map<Rule, Integer> merge){
		for (Entry<Rule, Integer> entry1 : rules1.entrySet()) {
			Rule rule1 = entry1.getKey();
			Integer frequency1 = entry1.getValue();
			
			int totalFrequency = frequency1;
			Rule rule = null;
			for (Entry<Rule, Integer> entry2 : merge.entrySet()) {
				rule = null;
				Rule rule2 = entry2.getKey();
				Integer frequency2 = entry2.getValue();
				
				ColoredDirectedGraph graph1 = rule1.asConnectedGraph();
				ColoredDirectedGraph graph2 = rule2.asConnectedGraph();
				
				boolean sameNodeSet = containSameNodes(graph1, graph2);
				if(!sameNodeSet){
					continue;
				}
				
				if((graph1.edgeSet().size() != graph2.edgeSet().size())){
					continue;
				}
				
				GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(graph1), SimPackGraphWrapper.getGraph(graph2));
				gi.calculate();
				
		        if(gi.getGraphIsomorphism() == 1){
//		        	logger.trace("ISOMORPH:\n" + entry.getKey() + "\n" + rule.getSource());
		        	totalFrequency = frequency1 + frequency2;
		        	rule = rule2;
		        	break;
		        }
			}
			if(rule != null){
				merge.put(rule, totalFrequency);
			} else {
				merge.put(rule1, frequency1);
			}
		}
		return merge;
	}
	
	public static <K, V extends Comparable<V>> List<Entry<K, V>> sortByValues(Map<K, V> map){
		List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Entry<K, V>>() {

			@Override
			public int compare(Entry<K, V> o1, Entry<K, V> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
        return entries;
	}
	
	private boolean containSameNodes(ColoredDirectedGraph g1, ColoredDirectedGraph g2){
		if(g1.vertexSet().size() != g2.vertexSet().size()){
			return false;
		}
		for(Node n1 : g1.vertexSet()){
			boolean match = false;
			for(Node n2 : g2.vertexSet()){
				if(n1.getLabel().equals(n2.getLabel())){
					match = true;
					break;
				}
			}
			if(!match){
				return false;
			}
		}
		for(Node n2 : g2.vertexSet()){
			boolean match = false;
			for(Node n1 : g1.vertexSet()){
				if(n1.getLabel().equals(n2.getLabel())){
					match = true;
					break;
				}
			}
			if(!match){
				return false;
			}
		}
		
		return true;
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
	
	public Map<Rule, Integer> learn(Table<Integer, String, String> trainData, ManualMapping manualMapping) {
		this.trainData = trainData;
		this.manualMapping = manualMapping;
		
		Set<Rule> rules = new HashSet<Rule>();
		ExecutorService threadPool = Executors.newFixedThreadPool(4);//Runtime.getRuntime().availableProcessors());
		List<Future<Collection<Rule>>> futures = new ArrayList<Future<Collection<Rule>>>();

		for (Cell<Integer, String, String> row : trainData.cellSet()){
			currentQueryId = row.getRowKey();
			int id = currentQueryId;
//			if(id<=2)continue;if(id==72)continue;
			final String question = row.getColumnKey();
			final String sparqlQuery = row.getValue();
			Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ);
			if(!query.isSelectType()){
				continue;
			}
			
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

			logger.info("###################\t" + id + "\t############################");
//			Collection<Rule> learnedRules = computeMappingRules(question, sparqlQuery, mapping);
//			rules.addAll(learnedRules);
			futures.add(threadPool.submit(new Callable<Collection<Rule>>() {

				@Override
				public Collection<Rule> call() throws Exception {
					return computeMappingRules(question, sparqlQuery, mapping);
				}
			}));
		
//			break;
		}
		for (Future<Collection<Rule>> future : futures) {
			try {
				rules.addAll(future.get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		logger.info("Got " + rules.size() + " rules.");
		Map<Rule, Integer> clusteredRules = clusterRules(rules);
		List<Entry<Rule, Integer>> sortedRules = sortByValues(clusteredRules);
//		printTopKEntries(sortedRules, 5);
		return clusteredRules;
	}
	
	private <K, V> void printTopKEntries(List<Entry<K, V>> entries, int k){
		for(int i = 0; i < Math.min(k, entries.size()); i++){
			logger.info(entries.get(i));
		}
	}
	
	private void clusterRulesMultithreaded(Collection<Rule> rules){
		int threadCount = 8;
		ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
		List<Rule> ruleList = Lists.newArrayList(rules);
		List<List<Rule>> lists = Lists.partition(ruleList, ruleList.size()/threadCount+1);
		List<Map<Rule, Integer>> mapList = new ArrayList<Map<Rule, Integer>>();
		for(List<Rule> l : lists){
			Map<Rule, Integer> map = new HashMap<Rule, Integer>();
			for(Rule r : l){
				map.put(r, 1);
			}
			mapList.add(map);
		}
		List<Future<Map<Rule, Integer>>> futureList = new ArrayList<Future<Map<Rule, Integer>>>();
		for (final Map<Rule, Integer> partition : mapList) {
			futureList.add(threadPool.submit(new Callable<Map<Rule, Integer>>() {

				@Override
				public Map<Rule, Integer> call() throws Exception {
					return computeRuleFrequency(partition);
				}
			}));
		}
		mapList.clear();
		for (Future<Map<Rule, Integer>> future : futureList) {
			try {
				Map<Rule, Integer> result = future.get();
				mapList.add(result);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		futureList.clear();
		while(mapList.size() > 1){
			for(int fromIndex = 0; fromIndex < mapList.size(); fromIndex+=2){
				final List<Map<Rule, Integer>> sub = mapList.subList(fromIndex, Math.min(fromIndex+2, mapList.size()));
				futureList.add(threadPool.submit(new Callable<Map<Rule, Integer>>() {
					@Override
					public Map<Rule, Integer> call() throws Exception {
						if(sub.size() == 2){
							return computeRuleFrequency(sub.get(0), sub.get(1));
						} else {
							return sub.get(0);
						}
						
					}
				}));
			}
//			List<List<Map<Rule, Integer>>> partitionList = Lists.partition(new ArrayList<Map<Rule, Integer>>(mapList), 2);
//			for (final List<Map<Rule, Integer>> partition : partitionList) {
//				futureList.add(threadPool.submit(new Callable<Map<Rule, Integer>>() {
//
//					@Override
//					public Map<Rule, Integer> call() throws Exception {
//						if(partition.size() == 2){
//							System.out.println("Calling:" + partition.get(0).hashCode() + "--" + partition.get(1).hashCode());
//							return computeRuleFrequency(partition.get(0), partition.get(1));
//						} else {
//							return partition.get(0);
//						}
//						
//					}
//				}));
//			}
			List<Map<Rule, Integer>> tmp = new ArrayList<Map<Rule,Integer>>();
			for (Future<Map<Rule, Integer>> future : futureList) {
				try {
					Map<Rule, Integer> result = future.get();
					tmp.add(result);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			mapList.clear();
			mapList.addAll(tmp);
			futureList.clear();
		}
		List<Entry<Rule, Integer>> sorted = sortByValues(mapList.get(0));
	}
	
	public Map<Rule, Integer> learn(Table<Integer, String, String> trainData, Map<Integer, BiMap<String, String>> manualMapping) {
		return learn(trainData, new ManualMapping(manualMapping));
	}
	

	/**
	 * Learn mapping rules from dependency trees to SPARQL queries.
	 * @param trainData as table - row format: (id, question, SPARQL query)
	 */
	public Map<Rule, Integer> learn(Table<Integer, String, String> trainData) {
		return learn(trainData, (ManualMapping)null);
	}
	
	public Map<Rule, Integer> learn(String question, String sparqlQuery, Map<Integer, BiMap<String, String>> manualMapping) {
		Table<Integer, String, String> trainData = HashBasedTable.create();
		trainData.put(1, question, sparqlQuery);
		return learn(trainData, new ManualMapping(manualMapping));
	}
	
	public Map<Rule, Integer> learn(String question, String sparqlQuery) {
		return learn(question, sparqlQuery, null);
	}
}
