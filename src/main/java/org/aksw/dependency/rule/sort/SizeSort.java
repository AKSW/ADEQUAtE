package org.aksw.dependency.rule.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.aksw.dependency.rule.Rule;

import com.google.common.primitives.Ints;

public class SizeSort implements RuleSort{
	
	public enum Ordering implements Comparator<Rule> {
	    DESCENDING {
	        @Override
	        public int compare(final Rule a, final Rule b) {
	            return Ints.compare(b.getSource().vertexSet().size(), a.getSource().vertexSet().size());
	        }
	    },
	    ASCENDING {
	        @Override
	        public int compare(final Rule a, final Rule b) {
	            return Ints.compare(a.getSource().vertexSet().size(), b.getSource().vertexSet().size());
	        }
	    },
	}
	
	private Ordering ordering = Ordering.DESCENDING;
	
	public SizeSort() {
	}
	
	public SizeSort(Ordering ordering) {
		this.ordering = ordering;
	}

	@Override
	public <T extends Number> List<Rule> sortRules(Map<Rule, T> rules) {
		  List<Rule> result = new ArrayList<Rule>(rules.keySet());
		  Collections.sort(result, ordering);
		  return result;
	}
	
	@Override
	public List<Rule> sortRules(Collection<Rule> rules) {
		  List<Rule> result = new ArrayList<Rule>(rules);
		  Collections.sort(result, ordering);
		  return result;
	}
}
