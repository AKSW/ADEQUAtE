package org.aksw.dependency;

import java.io.IOException;
import java.io.ObjectInputStream;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;

public class NLP {
	
	private static final String PARSER_MODEL_FILE = "models/lexparser/englishPCFG.ser";
	private static final String POS_TAGGER_MODEL_FILE = "models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	
	private static LexicalizedParser parser;
	private static MaxentTagger tagger;
	
	public static Tree getParseTree(String text){
		if(parser == null){
			try {
				parser = LexicalizedParser.loadModel(new ObjectInputStream(NLP.class.getClassLoader().getResourceAsStream(PARSER_MODEL_FILE)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Tree parseTree = parser.apply(text);
		return parseTree;
	}
	
	public static String getPOS(String text){
		if(tagger == null){
			try {
				tagger = new MaxentTagger(NLP.class.getClassLoader().getResource(POS_TAGGER_MODEL_FILE).getPath());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return tagger.tagString(text);
	}
	
	public static void main(String[] args) {
		String text = "Which is the highest mountain in Germany";
		System.out.println(NLP.getParseTree(text));
		System.out.println(NLP.getPOS(text));
		System.out.println(NLP.getPOS(text));
	}

}
