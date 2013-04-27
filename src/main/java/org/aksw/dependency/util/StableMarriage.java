package org.aksw.dependency.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.DependencyGraphNode;
import org.aksw.dependency.graph.Node;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class StableMarriage implements Matcher{
	
	private Map<String, String> mapping;
	
	public BiMap<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2, Map<String, String> mapping){
		this.mapping = mapping;
		//1. build the preferences
		Map<Node, List<Node>> preferences1 = new HashMap<Node, List<Node>>();
		Map<Node, List<Node>> preferences2 = new HashMap<Node, List<Node>>();
		
		//handle only useful nodes of both graphs
		List<Node> nodes1 = new ArrayList<Node>();
		for(Node node : graph1.vertexSet()){
			if(node.getLabel().startsWith("http:")){
				if(!RDF.type.getURI().equals(node.getLabel()) && !RDFS.label.getURI().equals(node.getLabel())){
					nodes1.add(node);
				}
			}
		}
		List<Node> nodes2 = new ArrayList<Node>();
		for(Node node : graph2.vertexSet()){
			String label = node.getLabel();
			if(node instanceof DependencyGraphNode && ((DependencyGraphNode)node).getPosTag() != null){
				nodes2.add(node);
			} else {
				if(label.startsWith("prep_")){
					nodes2.add(node);
				}
			}
		}
		BiMap<Node, Node> manualMatching = applyManualMapping(nodes2, nodes1);
		
		for (Node node1 : nodes1) {
			if(node1.getLabel().startsWith("http:")){
				SortedSet<MatchingCandidate> candidateSet = new TreeSet<StableMarriage.MatchingCandidate>();
				String label1 = getLabel(URI.create(node1.getLabel()));
				for (Node node2 : nodes2) {
					String label2 = node2.getLabel();
					if(label2.startsWith("prep_")){
						label2 = label2.replace("prep_", "");
					}
					float score = getScore(label1, label2);
					candidateSet.add(new MatchingCandidate(score, node2));
				}
				List<Node> candidates = new ArrayList<Node>();
				for(MatchingCandidate c : candidateSet){
					candidates.add(c.getNode());
				}
				preferences1.put(node1, candidates);
			}
		}
		//not efficient, but maybe later we have a score which is not symmetric
		for (Node node2 : nodes2) {
			SortedSet<MatchingCandidate> candidateSet = new TreeSet<StableMarriage.MatchingCandidate>();
			String label2 = node2.getLabel();
			for (Node node1 : nodes1) {
				if(node1.getLabel().startsWith("http:")){
					String label1 = getLabel(URI.create(node1.getLabel()));
					float score = getScore(label2, label1);
					candidateSet.add(new MatchingCandidate(score, node1));
				}
			}
			List<Node> candidates = new ArrayList<Node>();
			for(MatchingCandidate c : candidateSet){
				candidates.add(c.getNode());
			}
			preferences2.put(node2, new ArrayList<Node>(candidates));
		}
		//2. compute a stable matching
		BiMap<Node, Node> matching = match(nodes1, preferences1, preferences2);
		matching.putAll(manualMatching);
		return matching;
		
	}
	
	private BiMap<Node, Node> applyManualMapping(List<Node> nodes1, List<Node> nodes2){
		BiMap<Node, Node> matching = HashBiMap.create();
		for(Iterator<Node> it1 = nodes1.iterator(); it1.hasNext();){
			Node node1 = it1.next();
			String label1 = node1.toString();
			if(mapping.containsKey(label1)){
				for(Iterator<Node> it2 = nodes2.iterator(); it2.hasNext();){
					Node node2 = it2.next();
					String label2 = node2.getLabel().toString();
					if(mapping.get(label1).equals(label2)){
						matching.put(node1, node2);
						it1.remove();
						it2.remove();
					}
				}
			}
		}
		return matching;
	}
	
	public BiMap<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2){
		return computeMatching(graph1, graph2, new HashMap<String, String>());
	}
	
	private BiMap<Node, Node> match(List<Node> nodes,
			Map<Node, List<Node>> preferences1,
			Map<Node, List<Node>> preferences2){
		BiMap<Node, Node> matchedTo = HashBiMap.create();
        List<Node> freeNodes = new LinkedList<Node>();
        freeNodes.addAll(nodes);
        while(!freeNodes.isEmpty()){
            Node thisNode = freeNodes.remove(0); //get a load of THIS node
            List<Node> thisNodePrefers = preferences1.get(thisNode);
            for(Node node : thisNodePrefers){
                if(matchedTo.get(node) == null){//node is free
                    matchedTo.put(node, thisNode);
                    break;
                }else{
                    Node otherNode = matchedTo.get(node);
                    List<Node> thisOtherNodePrefers = preferences2.get(node);
                    if(thisOtherNodePrefers.indexOf(thisNode) <
                    		thisOtherNodePrefers.indexOf(otherNode)){
                        //node prefers this node to the node it's engaged to
                        matchedTo.put(node, thisNode);
                        freeNodes.add(otherNode);
                        break;
                    }//else no change...keep looking for this node
                }
            }
        }
        return matchedTo;
    }
	
	private float getScore(String s1, String s2){
		AbstractStringMetric metric = new Levenshtein();
		float score = metric.getSimilarity(s1, s2);
		
		return score;
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
	
	private String getLabelWithSPARQL(URI uri){
		String query = String.format("SELECT ?label WHERE {<%s> <http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER(LANGMATCHES(LANG(?label), 'en'))} LIMIT 1", uri.toString());
		
		QueryExecution qExec = QueryExecutionFactory.create(query);
		ResultSet rs = qExec.execSelect();
		if(rs.hasNext()){
			return rs.next().getLiteral("label").getLexicalForm();
		}
		return getLabel(uri);
	}
	
	public class MatchingCandidate implements Comparable<MatchingCandidate>{
		private float score;
		private Node node;
		
		public MatchingCandidate(float score, Node node) {
			super();
			this.score = score;
			this.node = node;
		}
		
		public Node getNode() {
			return node;
		}
		
		public float getScore() {
			return score;
		}

		public int compareTo(MatchingCandidate o) {
			float diff = o.getScore() - score;
			if(diff == 0){
				return o.getNode().getLabel().compareTo(node.getLabel());
			} else if(diff > 0){
				return 1;
			} else {
				return -1;
			}
		}
		
		
	}
	

}


