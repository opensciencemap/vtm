/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.core.MapPosition;

final class TileScheduler {

	static long time;
	static MapPosition mapPosition;

	/**
	 * Calculates the priority for the given tile based on the current position and zoom level of the supplied MapView.
	 * The smaller the distance from the tile center to the MapView center, the higher its priority. If the zoom level
	 * of a tile differs from the zoom level of the MapView, its priority decreases.
	 * 
	 * @param mapGeneratorJob
	 *            the tile whose priority should be calculated.
	 * @param mapView
	 *            the MapView whose current position and zoom level define the priority of the tile.
	 * @return the current priority of the tile. A smaller number means a higher priority.
	 */
	static double getPriority(MapGeneratorJob mapGeneratorJob, MapView mapView) {
		MapTile tile = mapGeneratorJob.tile;

		// if (tile.isDrawn) {
		// long diff = time - tile.loadTime;
		//
		// // check.. just in case
		// if (diff > 0.0) {
		// return (10000.0f / diff) * (tile.isVisible ? 1 : 5); // * tile.distance;
		// }
		// }

		// // calculate the center coordinates of the tile
		// double tileCenterLongitude = MercatorProjection.pixelXToLongitude(tile.getCenterX(), tileZoomLevel);
		// double tileCenterLatitude = MercatorProjection.pixelYToLatitude(tile.getCenterY(), tileZoomLevel);
		//
		// // calculate the Euclidian distance from the MapView center to the tile
		// // center
		// GeoPoint geoPoint = mapPosition.geoPoint;
		// double longitudeDiff = geoPoint.getLongitude() - tileCenterLongitude;
		// double latitudeDiff = geoPoint.getLatitude() - tileCenterLatitude;
		//
		// return Math.sqrt(longitudeDiff * longitudeDiff + latitudeDiff * latitudeDiff)
		// * (tile.visible ? 1.0 : 1000.0);
		// }

		return tile.distance / 1000.0;
	}

	private TileScheduler() {
		throw new IllegalStateException();
	}
}
