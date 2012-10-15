package org.aksw.dependency.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.DependencyNode;
import org.aksw.dependency.graph.Node;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.RDF;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class StableMarriage implements Matcher{
	
	public Map<Node, Node> computeMatching(ColoredDirectedGraph graph1, ColoredDirectedGraph graph2){
		//1. build the preferences
		Map<Node, List<Node>> preferences1 = new HashMap<Node, List<Node>>();
		Map<Node, List<Node>> preferences2 = new HashMap<Node, List<Node>>();
		
		//handle only useful nodes of both graphs
		List<Node> nodes1 = new ArrayList<Node>();
		for(Node node : graph1.vertexSet()){
			if(node.getLabel().startsWith("http:")){
				if(!RDF.type.getURI().equals(node.getLabel())){
					nodes1.add(node);
				}
			}
		}
		List<Node> nodes2 = new ArrayList<Node>();
		for(Node node : graph2.vertexSet()){
			String label = node.getLabel();
			if(node instanceof DependencyNode && ((DependencyNode)node).getPosTag() != null){
				nodes2.add(node);
			} else {
				if(label.startsWith("prep_")){
					nodes2.add(node);
				}
			}
		}
		
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
		Map<Node, Node> matching = match(nodes1, preferences1, preferences2);
		return matching;
		
	}
	
	private Map<Node, Node> match(List<Node> nodes,
			Map<Node, List<Node>> preferences1,
			Map<Node, List<Node>> preferences2){
		Map<Node, Node> matchedTo = new HashMap<Node, Node>();
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


