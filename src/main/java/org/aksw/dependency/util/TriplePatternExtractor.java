package org.aksw.dependency.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;
import com.hp.hpl.jena.sparql.syntax.ElementVisitorBase;
import com.hp.hpl.jena.sparql.util.VarUtils;

public class TriplePatternExtractor extends ElementVisitorBase {
	
	private Set<Triple> triplePattern;
	
	private Set<Triple> candidates;
	
	private boolean inOptionalClause = false;
	
	/**
	 * Returns all triple patterns in given SPARQL query that have the given node in subject position, i.e. the outgoing
	 * triple patterns.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Set<Triple> extractOutgoingTriplePatterns(Query query, Node node){
		Set<Triple> triplePatterns = extractTriplePattern(query, false);
		//remove triple patterns not containing triple patterns with given node in subject position
		for (Iterator<Triple> iterator = triplePatterns.iterator(); iterator.hasNext();) {
			Triple triple = iterator.next();
			if(!triple.subjectMatches(node)){
				iterator.remove();
			}
		}
		return triplePatterns;
	}
	
	/**
	 * Returns all triple patterns in given SPARQL query that have the given node in object position, i.e. the ingoing
	 * triple patterns.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Set<Triple> extractIngoingTriplePatterns(Query query, Node node){
		Set<Triple> triplePatterns = extractTriplePattern(query, false);
		//remove triple patterns not containing triple patterns with given node in object position
		for (Iterator<Triple> iterator = triplePatterns.iterator(); iterator.hasNext();) {
			Triple triple = iterator.next();
			if(!triple.objectMatches(node)){
				iterator.remove();
			}
		}
		return triplePatterns;
	}
	
	/**
	 * Returns all triple patterns in given SPARQL query that have the given node either in subject or in object position, i.e. 
	 * the ingoing and outgoing triple patterns.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Set<Triple> extractTriplePatterns(Query query, Node node){
		Set<Triple> triplePatterns = new HashSet<Triple>();
		triplePatterns.addAll(extractIngoingTriplePatterns(query, node));
		triplePatterns.addAll(extractOutgoingTriplePatterns(query, node));
		return triplePatterns;
	}
	
	/**
	 * Returns triple patterns for each projection variable v such that v is either in subject or object position.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Map<Var,Set<Triple>> extractTriplePatternsForProjectionVars(Query query){
		Map<Var,Set<Triple>> var2TriplePatterns = new HashMap<Var,Set<Triple>>();
		for (Var var : query.getProjectVars()) {
			Set<Triple> triplePatterns = new HashSet<Triple>();
			triplePatterns.addAll(extractIngoingTriplePatterns(query, var));
			triplePatterns.addAll(extractOutgoingTriplePatterns(query, var));
			var2TriplePatterns.put(var, triplePatterns);
		}
		return var2TriplePatterns;
	}
	
	/**
	 * Returns triple patterns for each projection variable v such that v is in subject position.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Map<Var,Set<Triple>> extractOutgoingTriplePatternsForProjectionVars(Query query){
		Map<Var,Set<Triple>> var2TriplePatterns = new HashMap<Var,Set<Triple>>();
		for (Var var : query.getProjectVars()) {
			Set<Triple> triplePatterns = new HashSet<Triple>();
			triplePatterns.addAll(extractOutgoingTriplePatterns(query, var));
			var2TriplePatterns.put(var, triplePatterns);
		}
		return var2TriplePatterns;
	}
	
	/**
	 * Returns triple patterns for each projection variable v such that v is in object position.
	 * @param query The SPARQL query.
	 * @param node
	 * @return
	 */
	public Map<Var,Set<Triple>> extractIngoingTriplePatternsForProjectionVars(Query query){
		Map<Var,Set<Triple>> var2TriplePatterns = new HashMap<Var,Set<Triple>>();
		for (Var var : query.getProjectVars()) {
			Set<Triple> triplePatterns = new HashSet<Triple>();
			triplePatterns.addAll(extractIngoingTriplePatterns(query, var));
			var2TriplePatterns.put(var, triplePatterns);
		}
		return var2TriplePatterns;
	}
	
	public Set<Triple> extractTriplePattern(Query query){
		return extractTriplePattern(query, false);
	}
	
	public Set<Triple> extractTriplePattern(Query query, boolean ignoreOptionals){
		triplePattern = new HashSet<Triple>();
		candidates = new HashSet<Triple>();
		
		query.getQueryPattern().visit(this);
		
		//postprocessing: triplepattern in OPTIONAL clause
		if(!ignoreOptionals){
			if(query.isSelectType()){
				for(Triple t : candidates){
					if(Sets.intersection(VarUtils.getVars(t), new HashSet<Var>(query.getProjectVars())).size() >= 2){
						triplePattern.add(t);
					}
				}
			}
		}
		
		return triplePattern;
	}
	
	public Set<Triple> extractTriplePattern(ElementGroup group){
		return extractTriplePattern(group, false);
	}
	
	public Set<Triple> extractTriplePattern(ElementGroup group, boolean ignoreOptionals){
		triplePattern = new HashSet<Triple>();
		candidates = new HashSet<Triple>();
		
		group.visit(this);
		
		//postprocessing: triplepattern in OPTIONAL clause
		if(!ignoreOptionals){
			for(Triple t : candidates){
				triplePattern.add(t);
			}
		}
		
		return triplePattern;
	}
	
	@Override
	public void visit(ElementGroup el) {
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
		}
	}

	@Override
	public void visit(ElementOptional el) {
		inOptionalClause = true;
		el.getOptionalElement().visit(this);
		inOptionalClause = false;
	}

	@Override
	public void visit(ElementTriplesBlock el) {
		for (Iterator<Triple> iterator = el.patternElts(); iterator.hasNext();) {
			Triple t = iterator.next();
			if(inOptionalClause){
				candidates.add(t);
			} else {
				triplePattern.add(t);
			}
		}
	}

	@Override
	public void visit(ElementPathBlock el) {
		for (Iterator<TriplePath> iterator = el.patternElts(); iterator.hasNext();) {
			TriplePath tp = iterator.next();
			if(inOptionalClause){
				candidates.add(tp.asTriple());
			} else {
				triplePattern.add(tp.asTriple());
			}
		}
	}

	@Override
	public void visit(ElementUnion el) {
		for (Iterator<Element> iterator = el.getElements().iterator(); iterator.hasNext();) {
			Element e = iterator.next();
			e.visit(this);
		}
	}
	
	@Override
	public void visit(ElementFilter el) {
	}
	
	public static void main(String[] args) throws Exception {
		Query query = QueryFactory.create("BASE <http://example.org/> SELECT ?s WHERE {?s <p> ?o. ?s a <A>. ?o <p> ?o1. ?o1 <q> ?s.}");
		TriplePatternExtractor extractor = new TriplePatternExtractor();
		//outgoing
		Map<Var, Set<Triple>> var2TriplePatterns = extractor.extractOutgoingTriplePatternsForProjectionVars(query);
		for (Entry<Var, Set<Triple>> entry : var2TriplePatterns.entrySet()) {
			Var var = entry.getKey();
			Set<Triple> triplePatterns = entry.getValue();
			System.out.println(var);
			for (Triple triple : triplePatterns) {
				System.out.println(triple);
			}
		}
		//ingoing
		var2TriplePatterns = extractor.extractIngoingTriplePatternsForProjectionVars(query);
		for (Entry<Var, Set<Triple>> entry : var2TriplePatterns.entrySet()) {
			Var var = entry.getKey();
			Set<Triple> triplePatterns = entry.getValue();
			System.out.println(var);
			for (Triple triple : triplePatterns) {
				System.out.println(triple);
			}
		}
		//all
		var2TriplePatterns = extractor.extractTriplePatternsForProjectionVars(query);
		for (Entry<Var, Set<Triple>> entry : var2TriplePatterns.entrySet()) {
			Var var = entry.getKey();
			Set<Triple> triplePatterns = entry.getValue();
			System.out.println(var);
			for (Triple triple : triplePatterns) {
				System.out.println(triple);
			}
		}
	}

}
