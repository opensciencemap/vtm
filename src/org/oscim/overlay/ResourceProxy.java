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
package org.oscim.overlay;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public interface ResourceProxy {

	public static enum string {

		// tile sources
		mapnik, cyclemap, public_transport, base, topo, hills, cloudmade_small, cloudmade_standard, mapquest_osm, mapquest_aerial, bing,

		// overlays
		fiets_nl, base_nl, roads_nl,

		// other stuff
		unknown, format_distance_meters, format_distance_kilometers, format_distance_miles, format_distance_nautical_miles, format_distance_feet, online_mode, offline_mode, my_location, compass, map_mode,

	}

	public static enum bitmap {

		/**
		 * For testing - the image doesn't exist.
		 */
		unknown,

		center, direction_arrow, marker_default, marker_default_focused_base, navto_small, next, previous, person,

		/**
		 * Menu icons
		 */
		ic_menu_offline, ic_menu_mylocation, ic_menu_compass, ic_menu_mapmode
	}

	String getString(string pResId);

	/**
	 * Use a string resource as a format definition, and format using the
	 * supplied format arguments.
	 *
	 * @param pResId
	 *            ...
	 * @param formatArgs
	 *            ...
	 * @return ...
	 */
	String getString(string pResId, Object... formatArgs);

	Bitmap getBitmap(bitmap pResId);

	/**
	 * Get a bitmap as a {@link Drawable}
	 *
	 * @param pResId
	 *            ...
	 * @return ...
	 */
	Drawable getDrawable(bitmap pResId);

	/**
	 * Gets the density from the current screen's DisplayMetrics
	 *
	 * @return the screen's density
	 */
	float getDisplayMetricsDensity();
}
