/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.rule.sort;

import com.google.common.collect.Lists;
import java.util.*;

import org.aksw.dependency.rule.Rule;

/**
 *
 * @author ngonga
 */
public class FrequencySort implements RuleSort{

    public <T extends Number> List<Rule>  sortRules(Map<Rule, T> rules) {
        Set<T> values = new TreeSet<T>(); 
        Map<T, Set<Rule>> reversedMap = new HashMap<T, Set<Rule>>();
        T frequency;
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
        for(T f: values)
        {
            Set<Rule> rs = reversedMap.get(f);
            for(Rule r: rs)
                result.add(r);
        }
        return Lists.reverse(result);
    }
    

	@Override
	public List<Rule> sortRules(Collection<Rule> rules) {
		return new ArrayList<>(rules);
	}
    
    public static void test()
    {
        Map<Rule, Double> rules = new HashMap<Rule, Double>();
        rules.put(new Rule(null, null, null, "A"), 5d);
        rules.put(new Rule(null, null, null, "B"), 2d);
        rules.put(new Rule(null, null, null, "C"), 10d);
        
        System.out.println((new FrequencySort()).sortRules(rules));
    }
    
    public static void main(String args[])
    {
        test();
    }

    
}
