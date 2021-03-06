package org.aksw.dependency;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.aksw.dependency.rule.Rule;
import org.aksw.dependency.rule.RuleGenerator;
import org.aksw.dependency.rule.RuleModel;
import org.aksw.dependency.rule.sort.FrequencySort;
import org.aksw.dependency.rule.sort.RuleSort;
import org.aksw.dependency.rule.sort.SizeSort;
import org.aksw.dependency.rule.sort.SizeSort.Ordering;
import org.aksw.dependency.template.SPARQLQueryTemplateGenerator;
import org.aksw.dependency.template.Template;
import org.aksw.dependency.util.ManualMapping;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public class QALD2TrainTest {
	
	private static BiMap<String, String> prefixes = HashBiMap.create();
	static {
		prefixes.put("dbp", "http://dbpedia.org/property/");
		prefixes.put("dbr", "http://dbpedia.org/resource/");
		prefixes.put("dbo", "http://dbpedia.org/ontology/");
	}
	
	private Table<Integer, String, String> trainData = HashBasedTable.create();
	private SortedMap<Integer, String> id2QuestionMap = new TreeMap<Integer, String>();
	private SortedMap<Integer, String> id2SPARQLQueryMap = new TreeMap<Integer, String>();
	private Map<Integer, BiMap<String, String>> manualMapping = new HashMap<Integer, BiMap<String,String>>();
	
	private static final String TRAIN_FILE = "data/qald2-dbpedia-train.xml";
	private static final String TEST_FILE = "data/qald2-dbpedia-train.xml";
	private static final String MANUAL_MATCHING_FILE = "data/qald2-dbpedia-train-node_matching.txt";
	private String endpointURL = "http://dbpedia.org/sparql";
	private boolean omitYAGO = true;
	private boolean omitUNION = true;
	private static final int maxNrOfTrainQueries = 100;
	
	private int sampleStart = 50;
	private int sampleEnd = 70;
	private String question;
	private String sparqlQuery;
	private int exampleId = 15;
	
	private int[] trainIds = {55,64};

	@Before
	public void setUp() throws Exception {
		loadTrainData();
		loadManualMapping();
//		System.setErr(new PrintStream("/dev/null"));
		question = id2QuestionMap.get(exampleId);
		sparqlQuery = id2SPARQLQueryMap.get(exampleId);
	}
	
	@Test
	public void learnFromSingleExample() throws FileNotFoundException {
		HashBasedTable<Integer, String, String> trainData = HashBasedTable.create();
		for (int id : trainIds) {
			String question = id2QuestionMap.get(id);
			String sparqlQuery = id2SPARQLQueryMap.get(id);
			trainData.put(id, question, sparqlQuery);
		}
		RuleGenerator ruleGenerator = new RuleGenerator(endpointURL);
		RuleModel ruleModel = ruleGenerator.generateRuleModel(trainData, manualMapping);
		RuleSort sorter = new SizeSort(Ordering.ASCENDING);
		List<Rule> sortedRules = sorter.sortRules(ruleModel.getRules());
		for (Rule rule : sortedRules) {
//			System.out.println(rule);
		}
		sorter = new FrequencySort();
		sortedRules = sorter.sortRules(ruleModel.getRules());
		for (Rule rule : sortedRules) {
//			System.out.println(rule);
		}
		SPARQLQueryTemplateGenerator templateGenerator = new SPARQLQueryTemplateGenerator(ruleModel);
		Collection<Template> templates = templateGenerator.generateSPARQLQueryTemplates(question);
		System.out.println(templates);
	}

	@Test
	public void testRuleLearning() throws FileNotFoundException {
		RuleGenerator ruleGenerator = new RuleGenerator(endpointURL);
		RuleModel ruleModel = ruleGenerator.generateRuleModel(trainData, (ManualMapping)null);
		RuleSort sorter = new SizeSort(Ordering.ASCENDING);
		List<Rule> sortedRules = sorter.sortRules(ruleModel.getRules());
		for (Rule rule : sortedRules) {
			System.out.println(rule);
		}
	}
	
	@Test
	public void testRuleLearningWithManualMappingInput() {
		RuleGenerator ruleGenerator = new RuleGenerator(endpointURL);
		RuleModel ruleModel = ruleGenerator.generateRuleModel(trainData, manualMapping);
		RuleSort sorter = new SizeSort(Ordering.ASCENDING);
		List<Rule> sortedRules = sorter.sortRules(ruleModel.getRules());
		for (Rule rule : sortedRules) {
			System.out.println(rule);
		}
	}
	
	@Test
	public void testRuleApplication() throws FileNotFoundException {
		RuleGenerator ruleGenerator = new RuleGenerator(endpointURL);
		RuleModel ruleModel = ruleGenerator.generateRuleModel(trainData, manualMapping);
		
		SPARQLQueryTemplateGenerator sparqlQueryGenerator = new SPARQLQueryTemplateGenerator(ruleModel);
		String question = "Give me the birthdays of all actors of the television_show Chirmed.";
		question = "Who is the daughter of Bruce Willis married to?";
		Collection<Template> templates = sparqlQueryGenerator.generateSPARQLQueryTemplates(question);
		for (Template template : templates) {
			System.out.println(template);
		}
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

				id2QuestionMap.put(id, question);
				id2SPARQLQueryMap.put(id, query);
				trainData.put(id, question, query);
//				if(trainData.size() == maxNrOfTrainQueries) break;
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

}
