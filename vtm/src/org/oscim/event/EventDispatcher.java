package org.oscim.event;

import org.oscim.utils.pool.LList;

/**
 * The Class EventDispatcher.
 * 
 * Events MUST be dispached from main-loop! To add events from other
 * threads use:
 * Map.post(new Runnable(){ public void run(tell(event,data);)};);
 * 
 * @param <T> the event source type
 * @param <E> the event 'data' type
 */
public class EventDispatcher<T, E> {

	/**
	 * Generic event-listener interface.
	 * 
	 * @param <E> the type of event 'data'
	 */
	public interface Listener<E> {

		/**
		 * The onEvent handler to be implemented by Listeners.
		 * 
		 * @param source the event source
		 * @param event the event object/type
		 * @param data, some object involved in this event
		 */
		public void onEvent(Object source, Event event, E data);
	}

	/**
	 * The Class Event to be subclassed by event-producers. Should be used
	 * to distinguish the type of event when more than one is produced.
	 */
	public static class Event {
		// nothing here
	}

	/** The event source object. */
	protected final T mSource;

	/** The list of listeners. */
	protected LList<Listener<E>> mListeners;

	/**
	 * Instantiates a new event dispatcher.
	 * 
	 * @param source the event-source
	 */
	public EventDispatcher(T source) {
		mSource = source;
	}

	/**
	 * Bind listener for event notifications.
	 */
	public void bind(Listener<E> listener) {
		if (LList.find(mListeners, listener) != null) {
			// throw some poo?
			return;
		}
		mListeners = LList.push(mListeners, new LList<Listener<E>>(listener));
	}

	/**
	 * Unbind listener.
	 */
	public void unbind(Listener<E> listener) {
		mListeners = LList.remove(mListeners, listener);
	}

	/**
	 * Tell listeners whats going on.
	 * 
	 * @param event the event
	 * @param data the data
	 */
	public void tell(Event event, E data) {
		for (LList<Listener<E>> l = mListeners; l != null; l = l.next) {
			l.data.onEvent(mSource, event, data);
		}
	}
}
