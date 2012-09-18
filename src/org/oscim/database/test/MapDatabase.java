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
package org.oscim.database.test;

import java.util.Map;

import org.oscim.core.BoundingBox;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.MapInfo;
import org.oscim.database.OpenResult;
import org.oscim.database.QueryResult;
import org.oscim.view.generator.JobTile;

/**
 * 
 *
 */
public class MapDatabase implements IMapDatabase {

	private final static String PROJECTION = "Mercator";
	private float[] mCoords = new float[20];
	private short[] mIndex = new short[4];
	// private Tag[] mTags = { new Tag("boundary", "administrative"), new Tag("admin_level", "2") };
	private Tag[] mTags = { new Tag("natural", "water") };
	private Tag[] mNameTags;

	private final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 0), null, PROJECTION, 0, 0, 0, "de", "yo!", "by me", null);

	private boolean mOpenFile = false;

	@Override
	public QueryResult executeQuery(JobTile tile, IMapDatabaseCallback mapDatabaseCallback) {

		float lat1 = -0.5f;
		float lon1 = -0.5f;
		float lat2 = Tile.TILE_SIZE - 0.5f;
		float lon2 = Tile.TILE_SIZE - 0.5f;

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

		mIndex[0] = 8;
		mIndex[1] = 2;

		lon1 = 40;
		lon2 = Tile.TILE_SIZE - 40;
		lat1 = 40;
		lat2 = Tile.TILE_SIZE - 40;

		mCoords[10] = lon1;
		mCoords[11] = lat1;

		mCoords[12] = lon2;
		mCoords[13] = lat1;

		mCoords[14] = lon2;
		mCoords[15] = lat2;

		mCoords[16] = lon1;
		mCoords[17] = lat2;

		mCoords[18] = lon1;
		mCoords[19] = lat1;

		mIndex[2] = 8;
		mIndex[3] = 2;

		mapDatabaseCallback.renderWay((byte) 0, mTags, mCoords, mIndex, true);

		lon1 = Tile.TILE_SIZE / 2;
		lat1 = Tile.TILE_SIZE / 2;

		mNameTags = new Tag[2];
		mNameTags[0] = new Tag("place", "city");
		mNameTags[1] = new Tag("name", tile.toString());
		mapDatabaseCallback.renderPointOfInterest((byte) 0, mNameTags, (int) lat1,
				(int) lon1);

		return QueryResult.SUCCESS;
	}

	@Override
	public String getMapProjection() {
		return null; // PROJECTION;
	}

	@Override
	public MapInfo getMapInfo() {
		return mMapInfo;
	}

	@Override
	public boolean isOpen() {
		return mOpenFile;
	}

	@Override
	public OpenResult open(Map<String, String> options) {
		mOpenFile = true;
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpenFile = false;
	}

	@Override
	public void cancel() {
	}

}
