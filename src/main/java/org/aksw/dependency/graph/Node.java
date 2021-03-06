package org.aksw.dependency.graph;

import java.io.Serializable;
import java.util.Set;

public class Node implements Serializable{
	
	private static final long serialVersionUID = 4149467628737116410L;
	
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
	
	public Node(Node node){
		this.id = node.getId();
		this.label = node.getLabel();
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getId() {
		return id;
	}
	
	public String toString() {
		return id;
//		return label + "(" + id + ")";
	}

	@Override
	public int hashCode() {
		return id.hashCode();
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((id == null) ? 0 : id.hashCode());
//		return result;
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
