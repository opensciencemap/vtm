/*
 * Copyright 2012 osmdroid
 * Copyright 2013 OpenScienceMap
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

// Created by plusminus on 18:00:24 - 25.09.2008
package org.oscim.overlay;

/**
 * This class contains constants used by the map view.
 *
 * @author Nicolas Gramlich
 */
public interface MapViewConstants {
	// ===========================================================
	// Final Fields
	// ===========================================================

	public static final boolean DEBUGMODE = false;

	public static final int NOT_SET = Integer.MIN_VALUE;

	public static final int ANIMATION_SMOOTHNESS_LOW = 4;
	public static final int ANIMATION_SMOOTHNESS_DEFAULT = 10;
	public static final int ANIMATION_SMOOTHNESS_HIGH = 20;

	public static final int ANIMATION_DURATION_SHORT = 500;
	public static final int ANIMATION_DURATION_DEFAULT = 1000;
	public static final int ANIMATION_DURATION_LONG = 2000;

	/** Minimum Zoom Level */
	public static final int MINIMUM_ZOOMLEVEL = 0;

	/**
	 * Maximum Zoom Level - we use Integers to store zoom levels so overflow
	 * happens at 2^32 - 1,
	 * but we also have a tile size that is typically 2^8, so (32-1)-8-1 = 22
	 */
	public static final int MAXIMUM_ZOOMLEVEL = 22;
}
