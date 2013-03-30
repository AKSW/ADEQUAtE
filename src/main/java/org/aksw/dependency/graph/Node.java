package org.aksw.dependency.graph;

import java.util.Set;

public class Node {
	
	protected String id;
	protected String label;
	protected Set<Integer> tags;
	
	public Node(String id) {
		this(id, id);
	}
	
	public Node(String id, String label) {
		this.label = label;
		this.id = id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getId() {
		return id;
	}
	
	public String toString() {
		return label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	public Node asGeneralizedNode(){return this;}
	
	public boolean tag(int tagId){
		return tags.add(tagId);
	}
	
	public boolean isTagged(int tagId){
		return tags.contains(tagId);
	}
	
	

}
