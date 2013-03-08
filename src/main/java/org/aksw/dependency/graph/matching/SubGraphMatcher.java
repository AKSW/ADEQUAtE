/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.graph.matching;

import java.util.List;
import java.util.Set;
import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.Node;

/**
 *
 * @author ngonga
 */
public interface SubGraphMatcher {
    Set<Set<Node>> getMatchingSubgraphs(ColoredDirectedGraph largerGraph, ColoredDirectedGraph smallerGraph);
}
