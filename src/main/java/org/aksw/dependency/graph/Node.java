package org.aksw.dependency.graph;

public class Node {
	
	private String label;
	private String posTag;
	
	public Node(String label) {
		this(label, null);
	}
	
	public Node(String label, String posTag) {
		this.label = label;
		this.posTag = posTag;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getPosTag() {
		return posTag;
	}
	
	public String toString() {
		return label + (posTag != null ? ("/" + posTag) : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
	

}
