package org.aksw.dependency.rule.clustering;

import java.util.Collection;
import java.util.Map;

import org.aksw.dependency.rule.Rule;

public interface RuleClustering {

	Map<Rule, Integer> clusterRules(Collection<Rule> rules);
	Map<Rule, Integer> clusterRules(Map<String, Collection<Rule>> questionWithRules);
}
