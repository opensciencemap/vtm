package org.osmdroid.overlays;

import org.oscim.core.GeoPoint;

/**
 * Interface for objects that need to handle map events thrown by a
 * MapEventsOverlay.
 *
 * @author M.Kergall
 */
public interface MapEventsReceiver {

    /**
     * @param p the position where the event occurred.
     * @return true if the event has been "consumed" and should not be handled
     * by other objects.
     */
    boolean singleTapUpHelper(GeoPoint p);

    /**
     * @param p the position where the event occurred.
     * @return true if the event has been "consumed" and should not be handled
     * by other objects.
     */
    boolean longPressHelper(GeoPoint p);

    /**
     * @param p1 p2
     *           the position where the event occurred for 2 finger.
     * @return true if the event has been "consumed" and should not be handled
     * by other objects.
     */
    boolean longPressHelper(GeoPoint p1, GeoPoint p2);
}
