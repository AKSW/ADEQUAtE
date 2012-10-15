package org.aksw.dependency.graph;

import org.jgrapht.graph.DefaultEdge;

public class ColoredEdge extends DefaultEdge{
	
	private String label;
	private String color;
	
	public ColoredEdge(String label, String color) {
		this.label = label;
		this.color = color;
	}
	
	public String getColor() {
		return color;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String toString() {
        return label;
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
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
		ColoredEdge other = (ColoredEdge) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	

}
