package org.oscim.event;

import org.oscim.utils.pool.LList;

/**
 * The Class EventDispatcher.
 * <p/>
 * Events MUST be dispatched from main-loop! To add events from other
 * threads use:
 * Map.post(new Runnable(){ public void run(tell(event,data);)};);
 *
 * @param <T> the event source type
 * @param <E> the event 'data' type
 */
public abstract class EventDispatcher<E extends EventListener, T> {

    /**
     * The list of listeners.
     */
    protected LList<E> mListeners;

    /**
     * Bind listener for event notifications.
     */
    public void bind(E listener) {
        if (LList.find(mListeners, listener) != null) {
            return;
        }
        mListeners = LList.push(mListeners, new LList<E>(listener));
    }

    /**
     * Remove listener.
     */
    public void unbind(E listener) {
        mListeners = LList.remove(mListeners, listener);
    }

    /**
     * Tell listeners whats going on.
     *
     * @param event the event
     * @param data  the data
     */
    public abstract void tell(E listener, Event event, T data);

    public void fire(Event event, T data) {
        for (LList<E> l = mListeners; l != null; l = l.next) {
            tell(l.data, event, data);
        }
    }
}
