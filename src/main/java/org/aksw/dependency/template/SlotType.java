package org.aksw.dependency.template;

public enum SlotType {
	LITERAL("lit"),
	CLASS("cls"),
	RESOURCE("res"),
	PROPERTY("p");
	
	private String placeHolder;

	SlotType(String placeHolder){
		this.placeHolder = placeHolder;
	}
	
	public String getPlaceHolder() {
		return placeHolder;
	}
}