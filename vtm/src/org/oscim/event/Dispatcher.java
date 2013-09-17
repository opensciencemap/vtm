package org.oscim.event;

import java.util.ArrayList;
import java.util.List;

public abstract class Dispatcher<T> {
	protected List<T> listeners = new ArrayList<T>();

	public void addListener(T l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	public void removeListener(T l) {
		listeners.remove(l);
	}

	public abstract void dispatch();
}
