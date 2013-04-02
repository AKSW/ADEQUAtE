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

}
