package org.aksw.dependency.graph;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.aksw.dependency.converter.DependencyTree2GraphConverter;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DependencyGraphGenerator {

	public ColoredDirectedGraph generateDependencyGraph(String question, boolean collapseNounPhrases,
			boolean omitUnimportantNodes) {
		// in a preprocessing step we have to check for noun phrases and combine
		// then, otherwise the dependency graph will be useless
		System.out.println("Original Question: " + question);
		if(collapseNounPhrases){
			question = collapseNounPhrases(question);
			System.out.println("Preprocessed Question: " + question);
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
			System.out.println(sb.toString().trim());

			// this is the Stanford dependency graph of the current sentence
			SemanticGraph dependencyGraph = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
			// System.out.println(dependencyGraph.typedDependencies());

			// convert dependency tree to directed graph
			DependencyTree2GraphConverter dependencyConverter = new DependencyTree2GraphConverter();
			ColoredDirectedGraph directedGraph = dependencyConverter.getGraph(dependencyGraph, omitUnimportantNodes);

			return directedGraph;
		}

		return null;
	}

	public ColoredDirectedGraph generateDependencyGraph(String question, boolean collapseNounPhrases) {
		return generateDependencyGraph(question, collapseNounPhrases, true);
	}

	public ColoredDirectedGraph generateDependencyGraph(String question) {
		return generateDependencyGraph(question, true);
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
