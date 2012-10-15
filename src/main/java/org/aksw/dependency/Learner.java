package org.aksw.dependency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.aksw.dependency.converter.DependencyTree2GraphConverter;
import org.aksw.dependency.converter.SPARQLQuery2GraphConverter;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredDirectedSubgraph;
import org.aksw.dependency.graph.DependencyNode;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.util.GraphUtils;
import org.aksw.dependency.util.Matcher;
import org.aksw.dependency.util.StableMarriage;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.vocabulary.RDF;

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

	private static final String TRAIN_FILE = "data/qald2-dbpedia-train.xml";
	private static final Logger logger = Logger.getLogger(Learner.class);

	private SortedMap<Integer, String> id2QuestionMap = new TreeMap<Integer, String>();
	private SortedMap<Integer, String> id2SPARQLQueryMap = new TreeMap<Integer, String>();
	
	private String endpointURL = "http://live.dbpedia.org/sparql";
	
	private void loadTrainData() {
		logger.info("Reading file containing queries and answers...");
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(this.getClass().getClassLoader().getResource(TRAIN_FILE).getPath());
			doc.getDocumentElement().normalize();
			NodeList questionNodes = doc.getElementsByTagName("question");
			int id;
			String question;
			String query;
			Set<String> answers;

			for (int i = 0; i < questionNodes.getLength(); i++) {
				Element questionNode = (Element) questionNodes.item(i);
				// read question ID
				id = Integer.valueOf(questionNode.getAttribute("id"));
				// Read question
				question = ((Element) questionNode.getElementsByTagName("string").item(0)).getChildNodes().item(0)
						.getNodeValue().trim();
				// Read SPARQL query
				query = ((Element) questionNode.getElementsByTagName("query").item(0)).getChildNodes().item(0)
						.getNodeValue().trim();
				if (query.toLowerCase().contains("out of scope")) {
					continue;
				}

				id2QuestionMap.put(id, question);
				id2SPARQLQueryMap.put(id, query);
			}
		} catch (DOMException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("questions.txt"));
			out.write(sb.toString());
			out.close();
		} catch (IOException e) {
			System.out.println("Exception ");

		}
		logger.info("Done.");
	}
	
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
	
	public void findMappings(String question, String queryString){
		//generate the directed graph for the dependencies of the question
		ColoredDirectedGraph graph1 = generateDependencyGraph(question, true);
		GraphUtils.paint(graph1, "Dependency Graph");
		
		//generate the directed graph for the SPARQL query
		ColoredDirectedGraph graph2 = generateSPARQLQueryGraph(queryString);
		GraphUtils.paint(graph2.toGeneralizedGraph(), "SPARQL Query Graph");
		
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
		
		//find the mappings between nodes in dependency graph and SPARQL query graph, if exist
		//we start from the URIs in the SPARQL query and try to find the corresponding token in the question, makes more sense
		for(Node sparqlNode : graph2.vertexSet()){
			String sparqlNodeLabel = sparqlNode.getLabel();
			if(sparqlNodeLabel.startsWith("http:")){//if node is URI
				if(RDF.type.getURI().equals(sparqlNodeLabel)){
					//handle rdf:type predicates
				} else {
					String label = getLabel(URI.create(sparqlNodeLabel));
					for(Node node1 : graph1.vertexSet()){
						if(node1 instanceof DependencyNode && ((DependencyNode)node1).getPosTag() != null){
							System.out.println("? " + label + "==" + node1.getLabel());
						}
					}
				}
			}
		}
		Matcher matcher = new StableMarriage();
		Map<Node, Node> matching = matcher.computeMatching(graph2, graph1);
		System.out.println(matching);
		
		//filter out graphs which do not contain at least one node involved in a matching
		for(Iterator<ColoredDirectedSubgraph> iter = subgraphs1.iterator(); iter.hasNext();){
			sub = iter.next();
			Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
			nodes.retainAll(matching.keySet());
			if(nodes.size() == sub.vertexSet().size()){
				iter.remove();
			}
		}
		for(Iterator<ColoredDirectedSubgraph> iter = subgraphs2.iterator(); iter.hasNext();){
			sub = iter.next();
			Set<Node> nodes = new HashSet<Node>(sub.vertexSet());
			nodes.retainAll(matching.values());
			if(nodes.size() == sub.vertexSet().size()){
				iter.remove();
			}
		}
		
		//1. we only match graphs g1 to graphs g2 if for each source node n_s in g1 there exists the corresponding target node n_t in g2
		for(ColoredDirectedSubgraph sub1 : subgraphs1){
			for(ColoredDirectedSubgraph sub2 : subgraphs2){
				for(Node sourceNode : sub1.vertexSet()){
					Node targetNode = matching.get(sourceNode);
					if(sub2.vertexSet().contains(targetNode)){
						System.out.println("Candidate:\n" + sub1 + "\n" + sub2);
					}
				}
			}
		}
		
		for(Entry<Node, Node> entry : matching.entrySet()){
			
		}
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
	
	
	

	public void learn() {
		loadTrainData();

		for (Entry<Integer, String> id2Question : id2QuestionMap.entrySet()) {
			int id = id2Question.getKey();
			String question = id2Question.getValue();
			String sparqlQuery = id2SPARQLQueryMap.get(id);

			logger.info("###################\t" + id + "\t############################");
			logger.info("Question: " + question);

			

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

		new Learner().findMappings(question, queryString);

	}

}
