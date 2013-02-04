/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oscim.core;

/**
 *
 */
public class WebMercator {
	/**
	 *
	 */
	public static final String NAME = "SphericalMercator";

	// earth radius * pi, roughly
	public static final double f900913 = 20037508.342789244;
	private static final double f900913_2 = 20037508.342789244 * 2;

	/**
	 * @param lon
	 *            ...
	 * @param z
	 *            ...
	 * @param offset
	 *            ...
	 * @return ...
	 */
	public static float sphericalMercatorToPixelX(float lon, long z, long offset) {
		return (float) (((lon + f900913) / f900913_2) * z - offset);
	}

	/**
	 * @param lat
	 *            ...
	 * @param z
	 *            ...
	 * @param offset
	 *            ...
	 * @return ...
	 */
	public static float sphericalMercatorToPixelY(float lat, long z, long offset) {
		return (float) (((lat + f900913) / f900913_2) * z
		- (z - offset));
	}

	/**
	 * @param pixelY
	 *            ...
	 * @param z
	 *            ...
	 * @return ...
	 */
	public static double PixelYtoSphericalMercator(long pixelY, byte z) {
		long half = (Tile.TILE_SIZE << z) >> 1;
		return ((half - pixelY) / (double) half) * f900913;
	}

	/**
	 * @param pixelX
	 *            ...
	 * @param z
	 *            ...
	 * @return ...
	 */
	public static double PixelXtoSphericalMercator(long pixelX, byte z) {
		long half = (Tile.TILE_SIZE << z) >> 1;
		return ((pixelX - half) / (double) half) * f900913;
	}

	private static double radius = 6378137;
	private static double D2R = Math.PI / 180;
	private static double HALF_PI = Math.PI / 2;

	/**
	 * from http://pauldendulk.com/2011/04/projecting-from-wgs84-to.html
	 *
	 * @param lon
	 *            ...
	 * @param lat
	 *            ...
	 * @return ...
	 */
	public static float[] fromLonLat(double lon, double lat)
	{
		double lonRadians = (D2R * lon);
		double latRadians = (D2R * lat);

		double x = radius * lonRadians;
		double y = radius * Math.log(Math.tan(Math.PI * 0.25 + latRadians * 0.5));

		float[] result = { (float) x, (float) y };
		return result;
	}

	/**
	 * from http://pauldendulk.com/2011/04/projecting-from-wgs84-to.html
	 *
	 * @param x
	 *            ...
	 * @param y
	 *            ...
	 * @return ...
	 */
	public static float[] toLonLat(double x, double y)
	{
		double ts;
		ts = Math.exp(-y / (radius));
		double latRadians = HALF_PI - 2 * Math.atan(ts);

		double lonRadians = x / (radius);

		double lon = (lonRadians / D2R);
		double lat = (latRadians / D2R);

		float[] result = { (float) lon, (float) lat };
		return result;
	}

}
