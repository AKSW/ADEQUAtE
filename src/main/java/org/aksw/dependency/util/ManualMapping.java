package org.aksw.dependency.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

public class ManualMapping {
	
	private static Map<String, String> prefixes = new HashMap<String, String>();
	static {
		prefixes.put("dbp", "http://dbpedia.org/property/");
		prefixes.put("dbr", "http://dbpedia.org/resource/");
		prefixes.put("dbo", "http://dbpedia.org/ontology/");
	}
	
	private Map<Integer, BiMap<String, String>> queryId2Mapping = new HashMap<Integer, BiMap<String,String>>();
	
	public ManualMapping(Map<Integer, BiMap<String, String>> queryId2Mapping){
		this.queryId2Mapping = queryId2Mapping;
	}
	
	public ManualMapping(String file) {
		try {
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				if(strLine.startsWith("#"))continue;
				List<String> split = Lists.newArrayList(Splitter.on(",").trimResults().omitEmptyStrings().split(strLine));
				if(split.size() != 3){
					System.err.println("Invalid format: " + split);
					continue;
				}
				int queryId = Integer.parseInt(split.get(0));
				String token = split.get(1);
				String uri = replacePrefix(split.get(2));
				
				BiMap<String, String> mapping = queryId2Mapping.get(queryId);
				if(mapping == null){
					mapping = HashBiMap.create();
					queryId2Mapping.put(queryId, mapping);
				}
				mapping.put(token, uri);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String replacePrefix(String prefixedURI){
		String uri = prefixedURI;
		for(Entry<String, String> entry : prefixes.entrySet()){
			if(uri.startsWith(entry.getKey())){
				uri = uri.replace(entry.getKey() + ":", entry.getValue());
				break;
			}
		}
		return uri;
	}
	
	public BiMap<String, String> getMapping(int queryId){
		return queryId2Mapping.get(queryId);
	}
	
	public boolean containsMapping(int queryId){
		return queryId2Mapping.containsKey(queryId);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Entry<Integer, BiMap<String, String>> entry : queryId2Mapping.entrySet()){
			sb.append(entry.getKey() + "\t" + entry.getValue() + "\n");
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		ManualMapping m = new ManualMapping("src/main/resources/data/qald2-dbpedia-train-node_matching.txt");
		Map<String, String> mapping = m.getMapping(2);
		System.out.println(mapping);
		System.out.println(mapping.containsKey("birthdays/NNS"));
		System.out.println("birthdays/NNS".hashCode());
	}

}
