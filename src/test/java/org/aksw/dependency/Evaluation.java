package org.aksw.dependency;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.aksw.dependency.rule.RuleGenerator;
import org.aksw.dependency.rule.RuleModel;
import org.aksw.dependency.template.SPARQLQueryTemplateGenerator;
import org.aksw.dependency.template.Template;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

public class Evaluation {
	
	
	private static final Logger logger = Logger.getLogger(Evaluation.class.getName());
	
	private static BiMap<String, String> prefixes = HashBiMap.create();
	static {
		prefixes.put("dbp", "http://dbpedia.org/property/");
		prefixes.put("dbr", "http://dbpedia.org/resource/");
		prefixes.put("dbo", "http://dbpedia.org/ontology/");
	}

	private static final String TRAIN_FILE = "data/qald2-dbpedia-train.xml";
	private static final String MANUAL_MATCHING_FILE = "data/qald2-dbpedia-train-node_matching.txt";
	
	private static final boolean omitYAGO = true;
	private static final boolean omitUNION = true;
	private static final boolean omitASK = true;
	
	private static final int maxNrOfTrainQueries = 50;
	private static final int nrOfFolds = 5;
	private static String endpointURL = "http://dbpedia.org/sparql";
	
	private Table<Integer, String, String> trainData = TreeBasedTable.create();
	private Map<Integer, BiMap<String, String>> manualMapping = new HashMap<Integer, BiMap<String,String>>();
	
	private void loadTrainData() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(this.getClass().getClassLoader().getResource(TRAIN_FILE).getPath());
			doc.getDocumentElement().normalize();
			NodeList questionNodes = doc.getElementsByTagName("question");
			int id;
			String question;
			String query;

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
				if(omitYAGO && query.toLowerCase().contains("yago")){
					continue;
				}
				if(omitUNION && query.toLowerCase().contains("union")){
					continue;
				}
				if(omitASK && query.toLowerCase().contains("ask")){
					continue;
				}

				trainData.put(id, question, query);
				if(trainData.size() == maxNrOfTrainQueries) break;
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
	}
	
	private String expandPrefix(String prefixedURI){
		for (Entry<String, String> entry : prefixes.entrySet()) {
			String prefix = entry.getKey();
			String namespace = entry.getValue();
			if(prefixedURI.startsWith(prefix + ":")){
				return prefixedURI.replace(prefix + ":", namespace);
			}
		}
		return prefixedURI;
	}
	
	private void loadManualMapping(){
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(this.getClass().getClassLoader().getResource(MANUAL_MATCHING_FILE).getPath()));
			String str;
			while ((str = in.readLine()) != null) {
				List<String> split = Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(str));
				if(split.size() != 3){
					System.err.println("Invalid format: " + split);
					continue;
				}
				int id = Integer.parseInt(split.get(0));
				String token = split.get(1);
				String uri = expandPrefix(split.get(2));
				BiMap<String, String> token2URI = manualMapping.get(id);
				if(token2URI == null){
					token2URI = HashBiMap.create(1);
					manualMapping.put(id, token2URI);
				}
				token2URI.put(token, uri);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  
		}
	}
	
	public void run(){
		loadManualMapping();
		loadTrainData();
		performCrossValidation();
	}
	
	private void performCrossValidation(){
		logger.info("Performing " + nrOfFolds + "-fold cross validation...");
		//partition the train data
		List<Run> runs = split(trainData);
		
		int i = 1;
		for (Run run : runs) {if(i++ != 5)continue;
			Table<Integer, String, String> trainSet = run.getTrainSet();
			Table<Integer, String, String> testSet = run.getTestSet();
			
			//generate rule model based on train set
			logger.info("Running training phase...");
			RuleGenerator ruleGenerator = new RuleGenerator(endpointURL);
			RuleModel ruleModel = ruleGenerator.generateRuleModel(trainSet, manualMapping);
			
			//apply the template generation on each question in the test set
			logger.info("Running testing phase...");
			SPARQLQueryTemplateGenerator templateGenerator = new SPARQLQueryTemplateGenerator(ruleModel);
			for(Cell<Integer, String, String> cell : testSet.cellSet()){
				String question = cell.getColumnKey();
				logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++\nEvaluating \"" + question + "\"");
				Collection<Template> templates = templateGenerator.generateSPARQLQueryTemplates(question);
				logger.info("Generated " + templates.size() + " templates.");
				for (Template template : templates) {
					logger.info(template);
				}
			}
		}
		
	}
	
	private List<Run> split(Table<Integer, String, String> data) {
		List<Run> runs = new ArrayList<Run>();
		// partition the train data
		int size = trainData.size() / nrOfFolds;
		List<List<Cell<Integer, String, String>>> partition = Lists.partition(new ArrayList<>(trainData.cellSet()),
				size);
		// for n-fold cross validation, use n-1 folds as train set and perform
		// evaluation on 1 fold
		for (int i = 0; i < partition.size(); i++) {
			// create test set
			Table<Integer, String, String> testSet = TreeBasedTable.create();
			for (Cell<Integer, String, String> cell : partition.get(i)) {
				testSet.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
			}
			// create train set
			Table<Integer, String, String> trainSet = TreeBasedTable.create();
			for (int j = 0; j < partition.size(); j++) {
				if (i != j) {
					for (Cell<Integer, String, String> cell : partition.get(j)) {
						trainSet.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
					}
				}
			}
			runs.add(new Run(trainSet, testSet));

		}
		return runs;
	}
	
	private void train(){
		
	}
	
	private void test(){
		
	}
	
	class Run {
		
		private Table<Integer, String, String> testSet;
		private Table<Integer, String, String> trainSet;

		public Run(Table<Integer, String, String> trainSet, Table<Integer, String, String> testSet) {
			this.testSet = testSet;
			this.trainSet = trainSet;
		}
		
		public Table<Integer, String, String> getTestSet() {
			return testSet;
		}
		
		public Table<Integer, String, String> getTrainSet() {
			return trainSet;
		}
	}
	
	public static void main(String[] args) throws Exception {
		new Evaluation().run();
	}

}
