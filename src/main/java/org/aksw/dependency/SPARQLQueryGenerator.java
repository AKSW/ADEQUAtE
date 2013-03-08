package org.aksw.dependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.aksw.dependency.converter.DependencyTree2GraphConverter;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.IsoGraph;
import org.aksw.dependency.util.GraphUtils;
import org.aksw.dependency.util.SimPackGraphWrapper;
import org.jgrapht.experimental.isomorphism.AdaptiveIsomorphismInspectorFactory;
import org.jgrapht.experimental.isomorphism.GraphIsomorphismInspector;

import simpack.accessor.graph.SimpleGraphAccessor;
import simpack.measure.graph.MaxCommonSubgraphIsoValiente;
import simpack.measure.graph.MaxGraphIsoCoveringValiente;
import simpack.measure.graph.SubgraphIsomorphism;
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

public class SPARQLQueryGenerator {
	
	private DependencyTree2GraphConverter dependencyConverter = new DependencyTree2GraphConverter();
	private Collection<Rule> rules;
	private Collection<Rule> appliedRules;

	public SPARQLQueryGenerator(Collection<Rule> rules) {
		this.rules = rules;
	}
	
	public void generateSPARQLQuery(String question){
		SemanticGraph dependencyGraph = generateDependencyGraph(question);
		generateSPARQLQuery(dependencyGraph);
	}
	
	public void generateSPARQLQuery(SemanticGraph dependencyGraph){
		ColoredDirectedGraph directedGraph = dependencyConverter.getGraph(dependencyGraph, true);
		directedGraph = directedGraph.toGeneralizedGraph();
		generateSPARQLQuery(directedGraph);
	}
	
	public void generateSPARQLQuery(ColoredDirectedGraph dependencyGraph){
		appliedRules = new ArrayList<Rule>();
		for(Rule rule : rules){
			ColoredDirectedGraph ruleGraph = rule.getSource();
			GraphUtils.paint(dependencyGraph, "Dep");
			GraphUtils.paint(ruleGraph, "Rule Source");
			boolean contains = contains(dependencyGraph, ruleGraph);
			if(contains){
				applyRule(rule);
			}
			break;
		}
//		while(true){}
	}
	
	private void applyRule(Rule rule){
		ColoredDirectedGraph target = rule.getTarget();
		appliedRules.add(rule);
	}
	
	private boolean contains(ColoredDirectedGraph container, ColoredDirectedGraph containee){
//		if(!container.vertexSet().containsAll(containee.vertexSet())){
//			return false;
//		}
		IsoGraph g1 = SimPackGraphWrapper.getGraphIdBased(container);
		IsoGraph g2 = SimPackGraphWrapper.getGraphIdBased(containee);
		System.out.println("Nodes G1: " + g1.getNodeSet());
		System.out.println("Nodes G2: " + g2.getNodeSet());
		String similarityMeasure = "Levenshtein";
        int minCliqueSize = 2;
        double structureWeight = 0.2d;
        double labelWeight = 1d;
        String denominator = "small";//if "first", size of first graph. if "small",size of smaller graph. if "big", size of bigger graph. if "average", average size of both graphs.
        boolean groupNodes = false;
        
//		SubgraphIsomorphism sgi = new SubgraphIsomorphism(g2, g1, 
//				similarityMeasure, minCliqueSize, structureWeight, 
//				labelWeight, denominator, groupNodes);
//		sgi.calculate();
//		System.out.println(sgi.getSimilarity());
		
		System.out.println(GraphUtils.contains(container, containee));
		
		return false;
	}
	
	
	private SemanticGraph generateDependencyGraph(String question) {
		question = collapseNounPhrases(question);
		CoreMap annotatedSentence = annotate(question);

		// traversing the words in the current sentence
		// a CoreLabel is a CoreMap with additional token-specific methods
		StringBuilder sb = new StringBuilder();
		for (CoreLabel token : annotatedSentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);

			sb.append(word + "/" + pos + " ");
		}
		// this is the Stanford dependency graph of the current sentence
		SemanticGraph dependencyGraph = annotatedSentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
		return dependencyGraph;

	}
	
	private CoreMap annotate(String sentence){
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, parse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// create an empty Annotation just with the given question
		Annotation document = new Annotation(sentence);

		// run all Annotators on this question
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		
		//we expect only one sentence as  question
		CoreMap annotatedSentence = sentences.get(0);
		return annotatedSentence;
	}
	
	private String collapseNounPhrases(String question){
		CoreMap annotatedSentence = annotate(question);
		
		StringBuilder sb = new StringBuilder();
		List<String> nounTags = Arrays.asList(new String[]{"NN", "NNS"});
		List<String> properNounTags = Arrays.asList(new String[]{"NNP", "NNPS"});
		String nounPhrase = "";
		boolean properNoun = false;
		boolean noun = false;
		for (CoreLabel token : annotatedSentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);
			
			if(nounTags.contains(pos)){
				if(noun){
					nounPhrase += "_";
				} else {
					if(!nounPhrase.isEmpty()){
						sb.append(nounPhrase + " ");
						nounPhrase = "";
					}
				}
				noun = true;
				properNoun = false;
				nounPhrase += word;
			} else if(properNounTags.contains(pos)){
				if(properNoun){
					nounPhrase += "_";
				} else {
					if(!nounPhrase.isEmpty()){
						sb.append(nounPhrase + " ");
						nounPhrase = "";
					}
				}
				properNoun = true;
				noun = false;
				nounPhrase += word;
			} else {
				if(!nounPhrase.isEmpty()){
					sb.append(nounPhrase + " ");
					nounPhrase = "";
				}
				sb.append(word + " ");
				noun = false;
				properNoun = false;
			}
			
			
		}
		return sb.toString().trim();
	}
}
