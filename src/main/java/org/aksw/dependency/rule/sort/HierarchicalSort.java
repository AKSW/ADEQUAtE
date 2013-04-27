/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.rule.sort;

import com.google.common.collect.Lists;
import java.util.*;

import org.aksw.dependency.graph.matching.NaiveSubgraphMatcher;
import org.aksw.dependency.rule.Rule;

/**
 *
 * @author ngonga
 */
public class HierarchicalSort implements RuleSort {

    NaiveSubgraphMatcher nsm;

    public HierarchicalSort() {
        nsm = new NaiveSubgraphMatcher();
    }

    public boolean contains(Rule r1, Rule r2) {
        return !nsm.getMatchingSubgraphs(r1.getSource(), r2.getTarget()).isEmpty();
    }

    public <T extends Number> List<Rule> sortRules(Map<Rule, T> rules) {
        List<Rule> rs = new ArrayList<Rule>(rules.keySet());

        //top rule contains all rules
        Rule top = new Rule(null, null, null);
        Map<Rule, Set<Rule>> childMap = new HashMap<Rule, Set<Rule>>();
        childMap.put(top, rules.keySet());

        Map<Rule, Double> hierarchy = new HashMap<Rule, Double>();

        //generate containment hierarchy
        for (int i = 0; i < rs.size(); i++) {
            for (int j = i + 1; j < rs.size(); j++) {
                boolean c12 = contains(rs.get(i), rs.get(j));
                boolean c21 = contains(rs.get(i), rs.get(j));

                //the rules are not equivalent. Important as equivalent rules would lead to cycles in propagate
                if (!(c12 && c21)) {

                    //i contains j
                    if (c12) {

                        if (!childMap.containsKey(rs.get(i))) {
                            childMap.put(rs.get(i), new HashSet<Rule>());
                        }
                        childMap.get(rs.get(i)).add(rs.get(j));
                    } else //j contains i
                    {
                        if (!childMap.containsKey(rs.get(j))) {
                            childMap.put(rs.get(j), new HashSet<Rule>());
                        }
                        childMap.get(rs.get(j)).add(rs.get(i));
                    }
                }
            }
        }
        propagate(childMap, top, 0, hierarchy);
        List<Rule> result = (new FrequencySort()).sortRules(hierarchy);
//        result.remove(top);
        return Lists.reverse(result);
    }
    

	@Override
	public List<Rule> sortRules(Collection<Rule> rules) {
		return null;
	}

    private void propagate(Map<Rule, Set<Rule>> childMap, Rule top, int index, Map<Rule, Double> hierarchy) {
        if (childMap.containsKey(top)) {
            if (!childMap.get(top).isEmpty()) {
                for (Rule r : childMap.get(top)) {
                    if (!hierarchy.containsKey(r)) {
                        hierarchy.put(r, (double) (index + 1));
                        propagate(childMap, r, index + 1, hierarchy);
                    } else {
                        if (hierarchy.get(r) < (index + 1)) {
                            hierarchy.put(r, (double) (index + 1));
                            propagate(childMap, r, index + 1, hierarchy);
                        }
                    }
                }
            }
        }
    }

    public static void test() {
        Map<Rule, Set<Rule>> childMap = new HashMap<Rule, Set<Rule>>();
        Rule a = new Rule(null, null, null, "a");
        Rule b = new Rule(null, null, null, "b");
        Rule c = new Rule(null, null, null, "c");
        Rule d = new Rule(null, null, null, "d");
        Rule e = new Rule(null, null, null, "e");
        childMap.put(a, new HashSet<Rule>());
        childMap.get(a).add(b);
        childMap.get(a).add(c);
        childMap.get(a).add(d);
        childMap.get(a).add(e);
        childMap.put(c, new HashSet<Rule>());
        childMap.get(c).add(d);
        childMap.get(c).add(e);

        HierarchicalSort hs = new HierarchicalSort();
        Map<Rule, Double> hierarchy = new HashMap<Rule, Double>();
        hs.propagate(childMap, a, 0, hierarchy);
        System.out.println(hierarchy);
    }

    public static void main(String args[]) {
        test();
    }
}
