/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.graph.matching;

import java.util.*;
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
        Set<Node> largerGraphNodes = new HashSet<>(largerGraph.vertexSet());
        Set<Node> smallerGraphNodes = new HashSet<>(smallerGraph.vertexSet());

        Set<Set<Node>> result = new HashSet<>();
        if (largerGraph.vertexSet().size() < smallerGraph.vertexSet().size()) {
            return result;
        }

        Set<Set<Node>> largeGraphSubsets = SubsetGenerator.getSubsets(largerGraphNodes, smallerGraphNodes.size());
        Set<Set<Node>> smallGraphSubsets = SubsetGenerator.getSubsets(smallerGraphNodes, smallerGraphNodes.size());

        for (Set<Node> large : largeGraphSubsets) {
            for (Set<Node> small : smallGraphSubsets) {
                if (matches(largerGraph, smallerGraph, large, small)) {
                    result.add(new HashSet<>(large));
                }
            }
        }
        return result;
    }

    public Set<Node> copy(Set<Node> nodes) {
        Set<Node> copy = new HashSet<>();
        for (Node n : nodes) {
            copy.add(n);
        }
        return copy;
    }

    public List<Node> copy(List<Node> nodes) {
        List<Node> copy = new ArrayList<>();
        for (Node n : nodes) {
            copy.add(n);
        }
        return copy;
    }
    /*
     * Checks whether the graph spawned by the nodes described by
     * smallerGraphNodes is a subgraph of the graph spawned by largerGraphNodes
     * in largerGraph
     */

    public boolean matches(ColoredDirectedGraph largerGraph, ColoredDirectedGraph smallerGraph, Set<Node> largerGraphNodes, Set<Node> smallerGraphNodes) {

        //test 1: node labels
        //Create copy
        Map<Node, Set<Node>> map = new HashMap<>();
        for (Node n1 : smallerGraphNodes) {
            for (Node n2 : largerGraphNodes) {
                if (n2.getLabel().equals(n1.getLabel())) {
                    if (!map.containsKey(n1)) {
                        map.put(n1, new HashSet<Node>());
                    }
                    map.get(n1).add(n2);
                }
            }
        }
        // if not all nodes from small were matched
        if (map.keySet().size() < smallerGraphNodes.size()) {
            return false;
        }

        // if not all nodes from large were matched
        Set<Node> large = new HashSet<>();
        for (Node n : map.keySet()) {
            large.addAll(map.get(n));
        }
        if (large.size() < largerGraphNodes.size()) {
            return false;
        }

        //all nodes were mapped. Now for the edges. We simply generate all possible solutions and check them
        // generate all solutions from Map
        List<Node> small = new ArrayList<>(smallerGraphNodes);
        Set<List<Node>> solutions = new HashSet<>();
        Set<List<Node>> oldSolutions = new HashSet<>();

        for (Node n : map.get(small.get(0))) {
            List<Node> l = new ArrayList<>();
            l.add(n);
            oldSolutions.add(l);
        }

        for (int i = 1; i < small.size(); i++) {
            // get all nodes with match small.get(i)
            Set<Node> nodes = map.get(small.get(i));

            //add this node to the possible solutions
            for (List<Node> list : oldSolutions) {
                for (Node n : nodes) {
                    List<Node> list2 = copy(list);
                    list2.add(n);
                    solutions.add(list2);
                }
            }

            oldSolutions = new HashSet<>(solutions);
        }

        //System.out.println(solutions);

        //test 2: edges, solution contains large graph edges, small contains small graph edges
        for (List<Node> solution : solutions) {
            if (solution.size() == small.size()) {
                if (checkSolution(solution, small, smallerGraph, largerGraph)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkSolution(List<Node> solution, List<Node> small, ColoredDirectedGraph smallGraph, ColoredDirectedGraph largeGraph) {
        for (int i = 0; i < small.size(); i++) {
            for (int k = 0; k < small.size(); k++) {
                if (smallGraph.containsEdge(small.get(i), small.get(k))) {
                    if (!largeGraph.containsEdge(solution.get(i), solution.get(k))) {
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
            lNodes.add(new Node(i + "", "A"));
            large.addVertex(lNodes.get(i));
        }

        List<Node> sNodes = new ArrayList<>();

        for (int i = 0; i <= 2; i++) {
            sNodes.add(new Node(i + "", "A"));
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
