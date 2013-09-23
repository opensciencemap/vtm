/*
 * Copyright 2012 osmdroid
 * Copyright 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

// Created by plusminus on 00:02:58 - 03.10.2008
package org.oscim.layers.marker;

import org.oscim.core.GeoPoint;

/**
 * Immutable class describing a GeoPoint with a Title and a Description.
 * 
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
 */
public class MarkerItem {

	public static final int ITEM_STATE_FOCUSED_MASK = 4;
	public static final int ITEM_STATE_PRESSED_MASK = 1;
	public static final int ITEM_STATE_SELECTED_MASK = 2;

	/**
	 * Indicates a hotspot for an area. This is where the origin (0,0) of a
	 * point will be located relative to the area. In otherwords this acts as an
	 * offset. NONE indicates that no adjustment should be made.
	 */
	public enum HotspotPlace {
		NONE, CENTER, BOTTOM_CENTER,
		TOP_CENTER, RIGHT_CENTER, LEFT_CENTER,
		UPPER_RIGHT_CORNER, LOWER_RIGHT_CORNER,
		UPPER_LEFT_CORNER, LOWER_LEFT_CORNER
	}

	public final String mUid;
	public final String mTitle;
	public final String mDescription;
	public final GeoPoint mGeoPoint;
	protected MarkerSymbol mMarker;
	protected HotspotPlace mHotspotPlace;

	/**
	 * @param title
	 *            this should be <b>singleLine</b> (no <code>'\n'</code> )
	 * @param description
	 *            a <b>multiLine</b> description ( <code>'\n'</code> possible)
	 * @param geoPoint
	 *            ...
	 */
	public MarkerItem(String title, String description, GeoPoint geoPoint) {
		this(null, title, description, geoPoint);
	}

	public MarkerItem(String uid, String title, String description,
	        GeoPoint geoPoint) {
		mTitle = title;
		mDescription = description;
		mGeoPoint = geoPoint;
		mUid = uid;
	}

	public String getUid() {
		return mUid;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getSnippet() {
		return mDescription;
	}

	public GeoPoint getPoint() {
		return mGeoPoint;
	}

	public MarkerSymbol getMarker() {
		return mMarker;
	}

	public void setMarker(MarkerSymbol marker) {
		mMarker = marker;
	}

	public void setMarkerHotspot(HotspotPlace place) {
		mHotspotPlace = (place == null) ? HotspotPlace.BOTTOM_CENTER : place;
	}

	public HotspotPlace getMarkerHotspot() {
		return mHotspotPlace;
	}
}
