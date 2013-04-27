package org.aksw.dependency.graph;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.aksw.dependency.converter.DependencyTree2GraphConverter;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class DependencyGraphGenerator {
	
	private static final Logger logger = Logger.getLogger(DependencyGraphGenerator.class.getName());

	public ColoredDirectedGraph generateDependencyGraph(String question, boolean collapseNounPhrases,
			boolean omitUnimportantNodes) {
//		try {
//			System.setErr(new PrintStream("/dev/null"));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		// in a preprocessing step we have to check for noun phrases and combine
		// then, otherwise the dependency graph will be useless
		logger.debug("Original Question: " + question);
		if(collapseNounPhrases){
			question = collapseNounPhrases(question);
		}
		
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
			logger.info("Preprocessed Question: " + sb.toString().trim());

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencyGraph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			// System.out.println(dependencyGraph.typedDependencies());
//			dependencyGraph.prettyPrint();
			System.setErr(System.err);
			detectProjectionNode(dependencyGraph);

			// convert dependency tree to directed graph
			DependencyTree2GraphConverter dependencyConverter = new DependencyTree2GraphConverter();
			ColoredDirectedGraph directedGraph = dependencyConverter.getGraph(dependencyGraph, omitUnimportantNodes);
			
			return directedGraph;
		}
		
		System.setErr(System.err);

		return null;
	}

	public ColoredDirectedGraph generateDependencyGraph(String question, boolean collapseNounPhrases) {
		return generateDependencyGraph(question, collapseNounPhrases, true);
	}

	public ColoredDirectedGraph generateDependencyGraph(String question) {
//	try {
//		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
//		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
//		LexicalizedParser lp = LexicalizedParser.loadModel();
//		Tree tree = lp.apply(question);
//		GrammaticalStructure gs = gsf.newGrammaticalStructure(tree);
//		Collection tdl = gs.typedDependenciesCCprocessed(true);
//		Main.writeImage(tree,tdl, "image.png",3);
//	} catch (Exception e) {
//		e.printStackTrace();
//	}
		return generateDependencyGraph(question, true);
	}
	
	private void detectProjectionNode(SemanticGraph dependencyGraph){
		//we assume only one root node
		IndexedWord root = dependencyGraph.getFirstRoot();
		//get the set of relations which type the root node is connected to children
		Set<GrammaticalRelation> childRelations = dependencyGraph.childRelns(root);
		//get a list of outgoing edges
		List<SemanticGraphEdge> outgoingEdges = dependencyGraph.getOutEdgesSorted(root);
		
		for (SemanticGraphEdge semanticGraphEdge : outgoingEdges) {
			GrammaticalRelation grammaticalRelation = semanticGraphEdge.getRelation();
			IndexedWord child = semanticGraphEdge.getDependent();
			if(grammaticalRelation.equals(EnglishGrammaticalRelations.DIRECT_OBJECT)){
				System.out.println("Projection Node: " + child.originalText());
			} else if(grammaticalRelation.equals(EnglishGrammaticalRelations.ATTRIBUTIVE)){
				System.out.println("Projection Node: " + child.originalText());
			} else {
				
			}
		}
	}

	private String collapseNounPhrases(String question) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		// create an empty Annotation just with the given question
		Annotation document = new Annotation(question);

		// run all Annotators on this question
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		// we expect only one sentence as question
		CoreMap annotatedSentence = sentences.get(0);
		StringBuilder sb = new StringBuilder();
		List<String> nounTags = Arrays.asList(new String[] { "NN", "NNS" });
		List<String> properNounTags = Arrays.asList(new String[] { "NNP", "NNPS" });
		String nounPhrase = "";
		boolean properNoun = false;
		boolean noun = false;
		for (CoreLabel token : annotatedSentence.get(TokensAnnotation.class)) {
			// this is the text of the token
			String word = token.get(TextAnnotation.class);
			// this is the POS tag of the token
			String pos = token.get(PartOfSpeechAnnotation.class);

			if (nounTags.contains(pos)) {
				if (noun) {
					nounPhrase += "_";
				} else {
					if (!nounPhrase.isEmpty()) {
						sb.append(nounPhrase + " ");
						nounPhrase = "";
					}
				}
				noun = true;
				properNoun = false;
				nounPhrase += word;
			} else if (properNounTags.contains(pos)) {
				if (properNoun) {
					nounPhrase += "_";
				} else {
					if (!nounPhrase.isEmpty()) {
						sb.append(nounPhrase + " ");
						nounPhrase = "";
					}
				}
				properNoun = true;
				noun = false;
				nounPhrase += word;
			} else {
				if (!nounPhrase.isEmpty()) {
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
