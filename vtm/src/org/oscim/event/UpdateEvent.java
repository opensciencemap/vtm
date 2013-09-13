package org.oscim.event;


public class UpdateEvent extends MapEvent {

	private static final long serialVersionUID = 1L;
	public static final String TYPE = "UpdateEvent";

	public UpdateEvent(Object source) {
		super(source);
	}

	public boolean positionChanged;
	public boolean clearMap;

}
