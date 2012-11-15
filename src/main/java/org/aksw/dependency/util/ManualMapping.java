package org.aksw.dependency.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ManualMapping {
	
	private static Map<String, String> prefixes = new HashMap<String, String>();
	static {
		prefixes.put("dbp", "http://dbpedia.org/property/");
		prefixes.put("dbr", "http://dbpedia.org/resource/");
		prefixes.put("dbo", "http://dbpedia.org/ontology/");
	}
	
	private Map<Integer, BiMap<String, String>> queryId2Mapping = new HashMap<Integer, BiMap<String,String>>();
	
	public ManualMapping() {}
	
	public ManualMapping(String file) {
		try {
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			br.readLine();//Skip first line
			String strLine;
			while ((strLine = br.readLine()) != null) {
				String[] data = strLine.split("\t");
				int queryId = Integer.parseInt(data[0].trim());
				String keyword = data[1].trim();
				String uri = replacePrefix(data[2].trim());
				
				BiMap<String, String> mapping = queryId2Mapping.get(queryId);
				if(mapping == null){
					mapping = HashBiMap.create();
					queryId2Mapping.put(queryId, mapping);
				}
				mapping.put(keyword, uri);
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
		System.out.println(new ManualMapping("resources/keyword-uri-mapping-qald2-dbpedia-train.csv"));
	}

}
