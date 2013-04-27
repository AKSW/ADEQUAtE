/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.rule.sort;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.aksw.dependency.rule.Rule;

/**
 *
 * @author ngonga
 */
public interface RuleSort {
	<T extends Number> List<Rule> sortRules(Map<Rule, T> rules);
	List<Rule> sortRules(Collection<Rule> rules);
}
