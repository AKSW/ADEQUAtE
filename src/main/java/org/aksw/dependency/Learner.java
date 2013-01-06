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

	private String endpointURL = "http://dbpedia.org/sparql";
	
	private boolean useManualMappings = false;
	
	private ManualMapping manualMapping = new ManualMapping("resources/keyword-uri-mapping-qald2-dbpedia-train.csv");
	
	private int currentQueryId;
	private StringBuilder matchingOutput = new StringBuilder();
	
	
	
	private String collapseNounPhrases(String question){
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// create an empty Annotation just with the given question
		Annotation document = new Annotation(question);

		// run all Annotators on this question
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		//we expect only one sentence as  question
		CoreMap annotatedSentence = sentences.get(0);
		StringBuilder sb = new StringBuilder();
		List<String> nounTags = Arrays.asList(new String[]{"NN", "NNS", "NNP", "NNPS"});
		String nounPhrase = "";
		for (CoreLabel token : annotatedSentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);
			if(nounTags.contains(pos)){
				if(!nounPhrase.isEmpty()){
					nounPhrase += "_";
				}
				nounPhrase += word;
			} else {
				if(!nounPhrase.isEmpty()){
					sb.append(nounPhrase + " ");
					nounPhrase = "";
				}
				sb.append(word + " ");
			}
			
			
		}
		return sb.toString().trim();
	}
	
	private ColoredDirectedGraph generateDependencyGraph(String question, boolean pruned){
		//in a preprocessing step we have to check for noun phrases and combine then, otherwise the dependency graph will be useless
		System.out.println("Original Question: " + question);
		question = collapseNounPhrases(question);
		System.out.println("Preprocessed Question: " + question);
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// create an empty Annotation just with the given question
		Annotation document = new Annotation(question);

		// run all Annotators on this question
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		for (CoreMap sentence : sentences) {
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			StringBuilder sb = new StringBuilder();
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);

				sb.append(word + "/" + pos + " ");
			}
			System.out.println(sb.toString().trim());

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencyGraph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
//			System.out.println(dependencyGraph.typedDependencies());

			// convert dependency tree to directed graph
			DependencyTree2GraphConverter dependencyConverter = new DependencyTree2GraphConverter();
			ColoredDirectedGraph directedGraph = dependencyConverter.getGraph(dependencyGraph, pruned);
			
			return directedGraph;
		}
		
		return null;
	}
	
	private ColoredDirectedGraph generateSPARQLQueryGraph(String queryString){
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
		ColoredDirectedGraph graph1 = generateDependencyGraph(question, true);
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
	
	private void computeRuleFrequency(Collection<Rule> rules){
		Map<Rule, Integer> graphCnt = new HashMap<Rule, Integer>();
		for(Rule rule : rules){
//			logger.info("Candidate:\n" + rule);
			int cnt = 0;
			Rule r = null;
			for(Entry<Rule, Integer> entry : graphCnt.entrySet()){
				ColoredDirectedGraph source = entry.getKey().getSource();
				ColoredDirectedGraph target = entry.getKey().getTarget();
				
				ColoredDirectedGraph currentSource = rule.getSource();
				ColoredDirectedGraph currentTarget = rule.getTarget();
				
				boolean sameNodeSet = containSameNodes(source, currentSource) && containSameNodes(target, currentTarget);
				if(!sameNodeSet){
					continue;
				}
				
				if((source.edgeSet().size() != currentSource.edgeSet().size()) && (target.edgeSet().size() != currentTarget.edgeSet().size())){
					continue;
				}
				
				GraphIsomorphism sourceGi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(source), SimPackGraphWrapper.getGraph(currentSource));
				sourceGi.calculate();
				GraphIsomorphism targetGi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(target), SimPackGraphWrapper.getGraph(currentTarget));
				targetGi.calculate();
		        r = null;
		        if(sourceGi.getGraphIsomorphism() == 1 && targetGi.getGraphIsomorphism() == 1){
//		        	logger.trace("ISOMORPH:\n" + entry.getKey() + "\n" + rule.getSource());
		        	cnt = entry.getValue()+1;
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(cnt > 1){
				if(r != null){
					graphCnt.remove(r);
				}
				graphCnt.put(rule, Integer.valueOf(cnt));
			} else {
				graphCnt.put(rule, Integer.valueOf(1));
			}
			
		}
		
		List<Entry<Rule, Integer>> sortedRules = sortByValues(graphCnt);
		
		for(Entry<Rule, Integer> entry : sortedRules){
			if(entry.getValue()>5){
				System.out.println("****************************************");
				System.out.println(entry);
			}
		}
	}
	
	private void computeRuleFrequency2(Collection<Rule> rules){
		Map<Rule, Integer> graphCnt = new HashMap<Rule, Integer>();
		System.out.println("#Rules:\t" + rules.size());
		for(Rule rule : rules){
//			logger.info("Candidate:\n" + rule);
			int cnt = 0;
			Rule r = null;
			for(Entry<Rule, Integer> entry : graphCnt.entrySet()){
				ColoredDirectedGraph currentGraph = rule.asConnectedGraph();
				ColoredDirectedGraph graph = entry.getKey().asConnectedGraph();
				
				boolean sameNodeSet = containSameNodes(currentGraph, graph);
				if(!sameNodeSet){
					continue;
				}
				
				if((currentGraph.edgeSet().size() != graph.edgeSet().size())){
					continue;
				}
				
				GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(currentGraph), SimPackGraphWrapper.getGraph(graph));
				gi.calculate();
		        r = null;
		        if(gi.getGraphIsomorphism() == 1){
//		        	logger.trace("ISOMORPH:\n" + entry.getKey() + "\n" + rule.getSource());
		        	cnt = entry.getValue()+1;
		        	r = entry.getKey();
		        	break;
		        }
			}
			if(cnt > 1){
				if(r != null){
					graphCnt.remove(r);
				}
				graphCnt.put(rule, Integer.valueOf(cnt));
			} else {
				graphCnt.put(rule, Integer.valueOf(1));
			}
			
		}
		
		List<Entry<Rule, Integer>> sortedRules = sortByValues(graphCnt);
		
		for(Entry<Rule, Integer> entry : sortedRules){
			if(entry.getValue()>5){
				System.out.println("****************************************");
				System.out.println(entry);
			}
		}
	}
	
	protected <K, V extends Comparable<V>> List<Entry<K, V>> sortByValues(Map<K, V> map){
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
	
	public void learn(Table<Integer, String, String> trainData, ManualMapping manualMapping) {
		this.trainData = trainData;
		this.manualMapping = manualMapping;
		
		Set<Rule> rules = new HashSet<Rule>();

		for (Cell<Integer, String, String> row : trainData.cellSet()){
			currentQueryId = row.getRowKey();
			int id = currentQueryId;
			if(id<=2)continue;if(id==72)continue;
			String question = row.getColumnKey();
			String sparqlQuery = row.getValue();
			Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ);
			if(!query.isSelectType()){
				continue;
			}
			
			Map<String, String> mapping = manualMapping.getMapping(id);
			if(mapping == null){
				mapping = new HashMap<String, String>();
			}

			logger.info("###################\t" + id + "\t############################");
			Collection<Rule> learnedRules = computeMappingRules(question, sparqlQuery, mapping);
			rules.addAll(learnedRules);
//			break;
		}
		
		int threadCount = 6;
		ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
		List<Rule> ruleList = Lists.newArrayList(rules);
		List<List<Rule>> lists = Lists.partition(ruleList, ruleList.size() / threadCount);
		List<Future<Map<Rule, Integer>>> list = new ArrayList<Future<Map<Rule, Integer>>>();
		for (List<Rule> partition : lists) {
			list.add(threadPool.submit(new RuleCounter(partition)));
		}
		for (Future<Map<Rule, Integer>> future : list) {
			try {
				Map<Rule, Integer> result = future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
//		computeRuleFrequency2(rules);
	}
	
	public void learn(Table<Integer, String, String> trainData, Map<Integer, BiMap<String, String>> manualMapping) {
		learn(trainData, new ManualMapping(manualMapping));
	}
	

	/**
	 * Learn mapping rules from dependency trees to SPARQL queries.
	 * @param trainData as table - row format: (id, question, SPARQL query)
	 */
	public void learn(Table<Integer, String, String> trainData) {
		learn(trainData, (ManualMapping)null);
	}
	
	private class RuleCounter implements Callable<Map<Rule, Integer>>{
		
		private Collection<Rule> rules;
		
		public RuleCounter(Collection<Rule> rules) {
			this.rules = rules;
		}

		private Map<Rule, Integer> computeRuleFrequency(Collection<Rule> rules){
			Map<Rule, Integer> rule2Frequency = new HashMap<Rule, Integer>();
			System.out.println("#Rules:\t" + rules.size());
			for(Rule rule : rules){
//				logger.info("Candidate:\n" + rule);
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
					
					GraphIsomorphism gi = new GraphIsomorphism(SimPackGraphWrapper.getGraph(currentGraph), SimPackGraphWrapper.getGraph(graph));
					gi.calculate();
			        r = null;
			        if(gi.getGraphIsomorphism() == 1){
//			        	logger.trace("ISOMORPH:\n" + entry.getKey() + "\n" + rule.getSource());
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

		@Override
		public Map<Rule, Integer> call() throws Exception {
			return computeRuleFrequency(rules);
		}

	}

	public static void main(String[] args) {
		String question = "What is the highest mountain in Germany?";
		String queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> "  
				+ "SELECT DISTINCT ?uri ?string WHERE {"
				+ " ?uri a dbo:Mountain ." + " ?uri dbo:locatedInArea res:Germany ."
				+ " ?uri dbo:elevation ?elevation ." + "} ORDER BY DESC(?elevation) LIMIT 1";
		
//		question = "Give me all soccer clubs in Premier League.";
//		queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> " + 
//		"SELECT DISTINCT ?uri WHERE {?uri a dbo:SoccerClub. ?uri dbo:league res:Premier_League}";
//		
//		question = "Give me all episodes of the first season of the HBO television series The Sopranos!";
//		queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> " + 
//		"SELECT DISTINCT ?uri WHERE {?uri dbo:series res:The_Sopranos  . ?uri dbo:seasonNumber 1 .}";

		question = "Which companies work in the aerospace industry as well as on nuclear reactor technology?";
		queryString = "PREFIX dbo:  <http://dbpedia.org/ontology/> " +
				"PREFIX dbp: <http://dbpedia.org/property/> " +
				"PREFIX res:  <http://dbpedia.org/resource/> " +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"SELECT DISTINCT ?uri WHERE {" +
				"?uri rdf:type dbo:Company  ." +
				"?uri dbp:industry res:Aerospace ." +
				"?uri dbp:industry res:Nuclear_reactor_technology .}";
//		new Learner().computeMappingRules(question, queryString);
//		new Learner().learn();

	}

}
