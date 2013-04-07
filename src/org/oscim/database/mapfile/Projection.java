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
package org.oscim.database.mapfile;

import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;

public class Projection {


	/**
	 * Converts a tile X number at a certain zoom level to a longitude
	 * coordinate.
	 *
	 * @param tileX
	 *            the tile X number that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the number should be converted.
	 * @return the longitude value of the tile X number.
	 */
	public static double tileXToLongitude(long tileX, int zoomLevel) {
		return MercatorProjection.pixelXToLongitude(tileX * Tile.TILE_SIZE, zoomLevel);
	}

	/**
	 * Converts a tile Y number at a certain zoom level to a latitude
	 * coordinate.
	 *
	 * @param tileY
	 *            the tile Y number that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the number should be converted.
	 * @return the latitude value of the tile Y number.
	 */
	public static double tileYToLatitude(long tileY, int zoomLevel) {
		return MercatorProjection.pixelYToLatitude(tileY * Tile.TILE_SIZE, zoomLevel);
	}

	/**
	 * Converts a latitude coordinate (in degrees) to a tile Y number at a
	 * certain zoom level.
	 *
	 * @param latitude
	 *            the latitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile Y number of the latitude value.
	 */
	public static long latitudeToTileY(double latitude, int zoomLevel) {
		return pixelYToTileY(MercatorProjection.latitudeToPixelY(latitude, zoomLevel), zoomLevel);
	}

	/**
	 * Converts a longitude coordinate (in degrees) to the tile X number at a
	 * certain zoom level.
	 *
	 * @param longitude
	 *            the longitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile X number of the longitude value.
	 */
	public static long longitudeToTileX(double longitude, int zoomLevel) {
		return pixelXToTileX(MercatorProjection.longitudeToPixelX(longitude, zoomLevel), zoomLevel);
	}

	/**
	 * Converts a pixel X coordinate to the tile X number.
	 *
	 * @param pixelX
	 *            the pixel X coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile X number.
	 */
	public static int pixelXToTileX(double pixelX, int zoomLevel) {
		return (int) Math.min(Math.max(pixelX / Tile.TILE_SIZE, 0),
				Math.pow(2, zoomLevel) - 1);
	}
	/**
	 * Converts a pixel Y coordinate to the tile Y number.
	 *
	 * @param pixelY
	 *            the pixel Y coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile Y number.
	 */
	public static int pixelYToTileY(double pixelY, int zoomLevel) {
		return (int) Math.min(Math.max(pixelY / Tile.TILE_SIZE, 0),
				Math.pow(2, zoomLevel) - 1);
	}
	private Projection(){

	}
}
