/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.graph.matching;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.util.SubsetGenerator;

/**
 *
 * @author ngonga
 */
public class NaiveSubgraphMatcher implements SubGraphMatcher {

    @Override
    public Set<Set<Node>> getMatchingSubgraphs(ColoredDirectedGraph largerGraph, ColoredDirectedGraph smallerGraph) {
        List<Node> largerGraphNodes = new ArrayList<>(largerGraph.vertexSet());
        List<Node> smallerGraphNodes = new ArrayList<>(smallerGraph.vertexSet());

        Set<Set<Node>> result = new HashSet<>();
        if (largerGraph.vertexSet().size() < smallerGraph.vertexSet().size()) {
            return result;
        }

        List<List<Node>> largeGraphSubsets = SubsetGenerator.getSubsets(largerGraphNodes, smallerGraphNodes.size());
        List<List<Node>> smallGraphSubsets = SubsetGenerator.getSubsets(smallerGraphNodes, smallerGraphNodes.size());

        for (List<Node> large : largeGraphSubsets) {
            for (List<Node> small : smallGraphSubsets) {
                if (matches(largerGraph, smallerGraph, large, small)) {
                    result.add(new HashSet<>(large));
                }
            }
        }
        return result;
    }

    /*
     * Checks whether the graph spawned by the nodes described by
     * smallerGraphNodes is a subgraph of the graph spawned by largerGraphNodes
     * in largerGraph
     */
    public boolean matches(ColoredDirectedGraph largerGraph, ColoredDirectedGraph smallerGraph, List<Node> largerGraphNodes, List<Node> smallerGraphNodes) {

        //test 1: node labels
        for (int i = 0; i < smallerGraphNodes.size(); i++) {
            if (!smallerGraphNodes.get(i).getLabel().equals(largerGraphNodes.get(i).getLabel())) {
                return false;
            }
        }

        //test 2: edges
        for (int i = 0; i < smallerGraphNodes.size(); i++) {
            for (int j = 0; j < smallerGraphNodes.size(); j++) {
                //check for edge in small graph
                if (smallerGraph.containsEdge(smallerGraphNodes.get(i), smallerGraphNodes.get(j))) {
                    if (!largerGraph.containsEdge(largerGraphNodes.get(i), largerGraphNodes.get(j))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static void main(String args[]) {
        ColoredDirectedGraph large = new ColoredDirectedGraph();
        ColoredDirectedGraph small = new ColoredDirectedGraph();
        
        List<Node> lNodes = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            lNodes.add(new Node(i + ""));
            large.addVertex(lNodes.get(i));
        }
        
        List<Node> sNodes = new ArrayList<>();
        
        for (int i = 0; i <= 2; i++) {
            sNodes.add(new Node(i + ""));
            small.addVertex(sNodes.get(i));
        }
        
        large.addEdge(lNodes.get(0), lNodes.get(1), new ColoredEdge("edge", "red"));
        large.addEdge(lNodes.get(1), lNodes.get(2), new ColoredEdge("edge", "red"));
        large.addEdge(lNodes.get(2), lNodes.get(0), new ColoredEdge("edge", "red"));

        large.addEdge(lNodes.get(3), lNodes.get(4), new ColoredEdge("edge", "red"));
        large.addEdge(lNodes.get(4), lNodes.get(5), new ColoredEdge("edge", "red"));
        large.addEdge(lNodes.get(5), lNodes.get(3), new ColoredEdge("edge", "red"));
        
        large.addEdge(lNodes.get(2), lNodes.get(3), new ColoredEdge("edge", "red"));
        
        small.addEdge(sNodes.get(0), sNodes.get(1), new ColoredEdge("edge", "red"));
        small.addEdge(sNodes.get(1), sNodes.get(2), new ColoredEdge("edge", "red"));
        small.addEdge(sNodes.get(2), sNodes.get(0), new ColoredEdge("edge", "red"));
        
        System.out.println(new NaiveSubgraphMatcher().getMatchingSubgraphs(large, small));
    }
}
