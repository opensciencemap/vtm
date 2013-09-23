package org.oscim.event;

public interface EventDispatcher {

	public void addListener(String type, EventListener listener);

	public void removeListener(String type, EventListener listener);
}
