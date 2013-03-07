/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.util;

import java.util.ArrayList;
import java.util.List;
import org.aksw.dependency.graph.Node;

/**
 *
 * @author ngonga
 */
public class SubsetGenerator {

    public static List<List<Node>> getSubsets(List<Node> set, int subsetSize) {
        List<List<Node>> oldQueue = new ArrayList<>();
        List<List<Node>> newQueue = new ArrayList<>();

        if (subsetSize > 0 && subsetSize <= set.size()) {
            for (Node node : set) {
                List l = new ArrayList();
                l.add(node);
                oldQueue.add(l);
            }

            for (int i = 1; i < subsetSize; i++) {
                for (List<Node> node : oldQueue) {
                    //get remainig objects
                    for(Node n: set)
                    {
                        if(!node.contains(n))
                        {
                            List copy = new ArrayList(node);
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
        List<Node> set = new ArrayList<>();
        for(int i=0; i<10; i++)
        {
            set.add(new Node(i+""));
        }           
        System.out.println(getSubsets(set, 2));
    }


}
