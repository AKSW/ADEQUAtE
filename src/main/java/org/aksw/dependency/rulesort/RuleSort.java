/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.dependency.rulesort;

import java.util.List;
import java.util.Map;
import org.aksw.dependency.Rule;

/**
 *
 * @author ngonga
 */
public interface RuleSort {
    List<Rule> sortRule(Map<Rule, Double> rules);
}
