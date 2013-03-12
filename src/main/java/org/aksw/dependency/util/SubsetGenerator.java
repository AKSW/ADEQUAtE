/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.aksw.dependency.graph.Node;

/**
 *
 * @author ngonga
 */
public class SubsetGenerator {

    public static Set<Set<Node>> getSubsets(Set<Node> set, int subsetSize) {
        Set<Set<Node>> oldQueue = new HashSet<>();
        Set<Set<Node>> newQueue = new HashSet<>();

        if (subsetSize > 0 && subsetSize <= set.size()) {
            for (Node node : set) {
                Set<Node> l = new HashSet<>();
                l.add(node);
                oldQueue.add(l);
            }

            for (int i = 1; i < subsetSize; i++) {
                newQueue = new HashSet<>();
                for (Set<Node> node : oldQueue) {
                    //get remaining objects
                    for(Node n: set)
                    {
                        if(!node.contains(n))
                        {
                            Set<Node> copy = new HashSet<>(node);
                            copy.add(n);
                            newQueue.add(copy);
                        }
                    }
                }
                oldQueue = newQueue;
            }
        }
        return newQueue;
    }
    
    public static void main(String args[])
    {
        Set<Node> set = new HashSet<>();
        for(int i=0; i<10; i++)
        {
            set.add(new Node(i+""));
        }           
        System.out.println(getSubsets(set, 2));
    }
}
