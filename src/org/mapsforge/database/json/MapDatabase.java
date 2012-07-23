/*
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
package org.mapsforge.database.json;

import java.io.File;

import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.database.FileOpenResult;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.MapFileInfo;

/**
 * 
 *
 */
public class MapDatabase implements IMapDatabase {

	private float[] mCoords = new float[20];
	private int[] mIndex = new int[1];
	// private Tag[] mTags = { new Tag("boundary", "administrative"), new Tag("admin_level", "2") };
	private Tag[] mTags = { new Tag("building", "yes") };
	private final MapFileInfo mMapInfo =
			new MapFileInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 0), null, "Mercator", 0, 0, 0, "de", "yo!", "by me");

	private boolean mOpenFile = false;

	private static double radius = 6378137;
	private static double D2R = Math.PI / 180;

	// private static double HALF_PI = Math.PI / 2;

	@Override
	public void executeQuery(Tile tile, IMapDatabaseCallback mapDatabaseCallback) {

		long cx = tile.pixelX + (Tile.TILE_SIZE >> 1);
		long cy = tile.pixelY + (Tile.TILE_SIZE >> 1);
		// float lon1 = (float) MercatorProjection.pixelXToLongitude(cx - 100, tile.zoomLevel) * 1000000;
		// float lon2 = (float) MercatorProjection.pixelXToLongitude(cx + 100, tile.zoomLevel) * 1000000;
		// float lat1 = (float) MercatorProjection.pixelYToLatitude(cy - 100, tile.zoomLevel) * 1000000;
		// float lat2 = (float) MercatorProjection.pixelYToLatitude(cy + 100, tile.zoomLevel) * 1000000;

		float lon1 = (float) MercatorProjection.pixelXToLongitude(cx - 100,
				tile.zoomLevel);
		float lon2 = (float) MercatorProjection.pixelXToLongitude(cx + 100,
				tile.zoomLevel);
		float lat1 = (float) MercatorProjection
				.pixelYToLatitude(cy - 100, tile.zoomLevel);
		float lat2 = (float) MercatorProjection
				.pixelYToLatitude(cy + 100, tile.zoomLevel);

		double lonRadians = (D2R * lon1);
		double latRadians = (D2R * lat1);

		// spherical mercator projection
		// lon1 = (float) (radius * lonRadians);
		// lat1 = (float) (radius * Math.log(Math.tan(Math.PI * 0.25 + latRadians * 0.5)));
		//
		// lonRadians = (D2R * lon2);
		// latRadians = (D2R * lat2);
		//
		// lon2 = (float) (radius * lonRadians);
		// lat2 = (float) (radius * Math.log(Math.tan(Math.PI * 0.25 + latRadians * 0.5)));
		//
		// mCoords[0] = lon1;
		// mCoords[1] = lat1;
		//
		// mCoords[2] = lon2;
		// mCoords[3] = lat1;
		//
		// mCoords[4] = lon2;
		// mCoords[5] = lat2;
		//
		// mCoords[6] = lon1;
		// mCoords[7] = lat2;
		//
		// mCoords[8] = lon1;
		// mCoords[9] = lat1;
		//
		// mIndex[0] = 10;

		lon1 = (float) MercatorProjection.pixelXToLongitude(cx - 80, tile.zoomLevel) * 1000000;
		lon2 = (float) MercatorProjection.pixelXToLongitude(cx + 80, tile.zoomLevel) * 1000000;
		lat1 = (float) MercatorProjection.pixelYToLatitude(cy - 80, tile.zoomLevel) * 1000000;
		lat2 = (float) MercatorProjection.pixelYToLatitude(cy + 80, tile.zoomLevel) * 1000000;

		mCoords[0] = lon1;
		mCoords[1] = lat1;

		mCoords[2] = lon2;
		mCoords[3] = lat1;

		mCoords[4] = lon2;
		mCoords[5] = lat2;

		mCoords[6] = lon1;
		mCoords[7] = lat2;

		mCoords[8] = lon1;
		mCoords[9] = lat1;

		mIndex[0] = 10;

		mapDatabaseCallback.renderWay((byte) 0, mTags, mCoords, mIndex, true);
	}

	@Override
	public MapFileInfo getMapFileInfo() {
		return mMapInfo;
	}

	@Override
	public boolean hasOpenFile() {
		return mOpenFile;
	}

	@Override
	public FileOpenResult openFile(File mapFile) {
		mOpenFile = true;
		return new FileOpenResult();
	}

	@Override
	public void closeFile() {
		mOpenFile = false;
	}

	@Override
	public String readString(int position) {
		return null;
	}

}
