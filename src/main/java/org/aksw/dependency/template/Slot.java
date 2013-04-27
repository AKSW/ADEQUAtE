package org.aksw.dependency.template;

public class Slot {

	private String token;
	private String anchor;
	private SlotType slotType;

	public Slot(String anchor, String token, SlotType slotType) {
		this.anchor = anchor;
		this.token = token;
		this.slotType = slotType;
	}
	
	public String getAnchor() {
		return anchor;
	}
	
	public String getToken() {
		return token;
	}
	
	public SlotType getSlotType() {
		return slotType;
	}
	
	@Override
	public String toString() {
		return slotType + "(?" + anchor + "): " + token;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((anchor == null) ? 0 : anchor.hashCode());
		result = prime * result + ((slotType == null) ? 0 : slotType.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		Slot other = (Slot) obj;
		if (anchor == null) {
			if (other.anchor != null)
				return false;
		} else if (!anchor.equals(other.anchor))
			return false;
		if (slotType != other.slotType)
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}

}
