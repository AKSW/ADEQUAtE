package org.aksw.dependency.util;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SPARQLUtil {

	public static boolean isClass(String endpointURL, String uri) {
		Query query = QueryFactory.create(String.format("ASK {<%s> a <http://www.w3.org/2002/07/owl#Class>.}", uri));
		QueryEngineHTTP qe = new QueryEngineHTTP(endpointURL, query);

		return qe.execAsk();
	}

	public static boolean isProperty(String endpointURL, String uri) {
		Query query = QueryFactory.create(String.format("ASK {{<%s> a <http://www.w3.org/2002/07/owl#ObjectProperty>.}" +
				" UNION {<%s> a <http://www.w3.org/2002/07/owl#DatatypeProperty>.}}", uri, uri));
		QueryEngineHTTP qe = new QueryEngineHTTP(endpointURL, query);

		return qe.execAsk();
	}

	public static boolean isResource(String endpointURL, String uri) {
		return !isClass(endpointURL, uri) && !isProperty(endpointURL, uri);
	}

}
