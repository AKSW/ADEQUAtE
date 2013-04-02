package org.aksw.dependency.template;

import java.util.Collection;

import com.hp.hpl.jena.query.Query;

public class Template {

	private Query query;
	private Collection<Slot> slots;

	public Template(Query query, Collection<Slot> slots) {
		this.query = query;
		this.slots = slots;
	}
	
	public Query getQuery() {
		return query;
	}
	
	public Collection<Slot> getSlots() {
		return slots;
	}
	
	@Override
	public String toString() {
		return query.toString() + "\nSlots:\n" + slots.toString().replace(",", "\n");
	}

}
