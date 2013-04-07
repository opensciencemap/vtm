/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.core;

/**
 * An implementation of the spherical Mercator projection.
 */
public final class MercatorProjection {
	/**
	 * The circumference of the earth at the equator in meters.
	 */
	public static final double EARTH_CIRCUMFERENCE = 40075016.686;

	/**
	 * Maximum possible latitude coordinate of the map.
	 */
	public static final double LATITUDE_MAX = 85.05112877980659;

	/**
	 * Minimum possible latitude coordinate of the map.
	 */
	public static final double LATITUDE_MIN = -LATITUDE_MAX;

	/**
	 * Maximum possible longitude coordinate of the map.
	 */
	public static final double LONGITUDE_MAX = 180;

	/**
	 * Minimum possible longitude coordinate of the map.
	 */
	public static final double LONGITUDE_MIN = -LONGITUDE_MAX;

	/**
	 * Calculates the distance on the ground that is represented by a single
	 * pixel on the map.
	 *
	 * @param latitude
	 *            the latitude coordinate at which the resolution should be
	 *            calculated.
	 * @param zoomLevel
	 *            the zoom level at which the resolution should be calculated.
	 * @return the ground resolution at the given latitude and zoom level.
	 */
	public static double calculateGroundResolution(double latitude, int zoomLevel) {
		return Math.cos(latitude * (Math.PI / 180)) * EARTH_CIRCUMFERENCE
				/ ((long) Tile.TILE_SIZE << zoomLevel);
	}

	/**
	 * Projects a longitude coordinate (in degrees) to the range [0.0,1.0]
	 *
	 * @param latitude
	 *            the latitude coordinate that should be converted.
	 * @return the position .
	 */
	public static double latitudeToY(double latitude) {
		double sinLatitude = Math.sin(latitude * (Math.PI / 180));
		return 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);
	}

	public static double toLatitude(double y) {
		return 90 - 360 * Math.atan(Math.exp((y - 0.5) * (2 * Math.PI))) / Math.PI;
	}

	/**
	 * Projects a longitude coordinate (in degrees) to the range [0.0,1.0]
	 *
	 * @param longitude
	 *            the longitude coordinate that should be converted.
	 * @return the position .
	 */
	public static double longitudeToX(double longitude) {
		return (longitude + 180) / 360;
	}

	public static double toLongitude(double x) {
		return 360 * (x - 0.5);
	}

	/**
	 * @param latitude
	 *            the latitude value which should be checked.
	 * @return the given latitude value, limited to the possible latitude range.
	 */
	public static double limitLatitude(double latitude) {
		return Math.max(Math.min(latitude, LATITUDE_MAX), LATITUDE_MIN);
	}

	/**
	 * @param longitude
	 *            the longitude value which should be checked.
	 * @return the given longitude value, limited to the possible longitude
	 *         range.
	 */
	public static double limitLongitude(double longitude) {
		return Math.max(Math.min(longitude, LONGITUDE_MAX), LONGITUDE_MIN);
	}

	public static double wrapLongitude(double longitude) {
		if (longitude < -180)
			return Math.max(Math.min(360 + longitude, LONGITUDE_MAX), LONGITUDE_MIN);
		else if (longitude > 180)
			return Math.max(Math.min(longitude - 360, LONGITUDE_MAX), LONGITUDE_MIN);

		return longitude;
	}

	/**
	 * Converts a pixel X coordinate at a certain zoom level to a longitude
	 * coordinate.
	 *
	 * @param pixelX
	 *            the pixel X coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the longitude value of the pixel X coordinate.
	 */
	public static double pixelXToLongitude(double pixelX, int zoomLevel) {
		return 360 * ((pixelX / ((long) Tile.TILE_SIZE << zoomLevel)) - 0.5);
	}

	/**
	 * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a
	 * certain zoom level.
	 *
	 * @param longitude
	 *            the longitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the pixel X coordinate of the longitude value.
	 */
	public static double longitudeToPixelX(double longitude, int zoomLevel) {
		return (longitude + 180) / 360 * ((long) Tile.TILE_SIZE << zoomLevel);
	}

	/**
	 * Converts a pixel Y coordinate at a certain zoom level to a latitude
	 * coordinate.
	 *
	 * @param pixelY
	 *            the pixel Y coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the latitude value of the pixel Y coordinate.
	 */
	public static double pixelYToLatitude(double pixelY, int zoomLevel) {
		double y = 0.5 - (pixelY / ((long) Tile.TILE_SIZE << zoomLevel));
		return 90 - 360 * Math.atan(Math.exp(-y * (2 * Math.PI))) / Math.PI;
	}

	/**
	 * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a
	 * certain zoom level.
	 *
	 * @param latitude
	 *            the latitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the pixel Y coordinate of the latitude value.
	 */
	public static double latitudeToPixelY(double latitude, int zoomLevel) {
		double sinLatitude = Math.sin(latitude * (Math.PI / 180));
		return (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI))
				* ((long) Tile.TILE_SIZE << zoomLevel);
	}

	private MercatorProjection() {
	}
}
