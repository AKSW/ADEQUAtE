/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.rulesort;

import com.google.common.collect.Lists;
import java.util.*;
import org.aksw.dependency.Rule;

/**
 *
 * @author ngonga
 */
public class FrequencySort implements RuleSort{

    public List<Rule> sortRule(Map<Rule, Double> rules) {
        Set<Double> values = new TreeSet<Double>(); 
        Map<Double, Set<Rule>> reversedMap = new HashMap<Double, Set<Rule>>();
        double frequency;
        for(Rule r: rules.keySet())
        {
            frequency = rules.get(r);
            values.add(frequency);
            if(!reversedMap.containsKey(frequency))
            {
                reversedMap.put(frequency, new HashSet<Rule>());
            }
            reversedMap.get(frequency).add(r);
        }
        List<Rule> result = new ArrayList<Rule>();
        for(Double f: values)
        {
            Set<Rule> rs = reversedMap.get(f);
            for(Rule r: rs)
                result.add(r);
        }
        return Lists.reverse(result);
    }
    
    public static void test()
    {
        Map<Rule, Double> rules = new HashMap<Rule, Double>();
        rules.put(new Rule(null, null, null, "A"), 5d);
        rules.put(new Rule(null, null, null, "B"), 2d);
        rules.put(new Rule(null, null, null, "C"), 10d);
        
        System.out.println((new FrequencySort()).sortRule(rules));
    }
    
    public static void main(String args[])
    {
        test();
    }
    
}
