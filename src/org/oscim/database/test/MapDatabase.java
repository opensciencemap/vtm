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

import org.oscim.core.BoundingBox;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.IMapDatabaseCallback.WayData;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.database.OpenResult;
import org.oscim.database.QueryResult;
import org.oscim.generator.JobTile;

/**
 *
 *
 */
public class MapDatabase implements IMapDatabase {

	private final static String PROJECTION = "Mercator";
	//private float[] mCoords = new float[20];
	//private short[] mIndex = new short[4];

	GeometryBuffer mGeom = new GeometryBuffer(new float[20], new short[4]);

	// private Tag[] mTags = { new Tag("boundary", "administrative"), new
	// Tag("admin_level", "2") };
	private final Tag[] mTags = { new Tag("natural", "water") };
	private final Tag[] mTagsWay = { new Tag("highway", "primary"), new Tag("name", "Highway Rd") };

	// private Tag[] mNameTags;

	private final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 5), null, PROJECTION, 0, 0, 0, "de", "yo!", "by me",
					null);

	private boolean mOpenFile = false;
	private final WayData mWay = new WayData();

	@Override
	public QueryResult executeQuery(JobTile tile, IMapDatabaseCallback mapDatabaseCallback) {

		int size = Tile.TILE_SIZE;
		float[] points = mGeom.points;
		short[] index = mGeom.index;

		float lat1 = -1;
		float lon1 = -1;
		float lat2 = size + 1;
		float lon2 = size + 1;

		points[0] = lon1;
		points[1] = lat1;

		points[2] = lon2;
		points[3] = lat1;

		points[4] = lon2;
		points[5] = lat2;

		points[6] = lon1;
		points[7] = lat2;

		points[8] = lon1;
		points[9] = lat1;

		index[0] = 10;
		index[1] = 0;

		lon1 = 40;
		lon2 = size - 40;
		lat1 = 40;
		lat2 = size - 40;

		points[10] = lon1;
		points[11] = lat1;

		points[12] = lon2;
		points[13] = lat1;

		points[14] = lon2;
		points[15] = lat2;

		points[16] = lon1;
		points[17] = lat2;

		points[18] = lon1;
		points[19] = lat1;

		index[2] = 10;
		index[3] = 0;

		mWay.geom = mGeom;
		mWay.tags = mTags;
		mWay.layer = (byte)0;
		mWay.closed = true;

		mapDatabaseCallback.renderWay(mWay);

		index[0] = 4;
		index[1] = -1;

		// middle horizontal
		points[0] = 0;
		points[1] = size / 2;
		points[2] = size;
		points[3] = size / 2;
		mapDatabaseCallback.renderWay(mWay);

		// center up
		points[0] = size / 2;
		points[1] = -size / 2;
		points[2] = size / 2;
		points[3] = size / 2;
		mapDatabaseCallback.renderWay(mWay);

		// center down
		points[0] = size / 2;
		points[1] = size / 2;
		points[2] = size / 2;
		points[3] = size / 2 + size;
		mapDatabaseCallback.renderWay(mWay);

		mWay.layer = (byte)1;
		// left-top to center
		points[0] = size / 2;
		points[1] = size / 2;
		points[2] = 10;
		points[3] = 10;
		mapDatabaseCallback.renderWay(mWay);

		// middle horizontal
		points[0] = 0;
		points[1] = 10;
		points[2] = size;
		points[3] = 10;
		mapDatabaseCallback.renderWay(mWay);

		// middle horizontal
		points[0] = 10;
		points[1] = 0;
		points[2] = 10;
		points[3] = size;
		mapDatabaseCallback.renderWay(mWay);

		// lon1 = size / 2;
		// lat1 = size / 2;

		// mNameTags = new Tag[2];
		// mNameTags[0] = new Tag("place", "city");
		// mNameTags[1] = new Tag("name", tile.toString());
		// mapDatabaseCallback.renderPointOfInterest((byte) 0, mNameTags, (int)
		// lat1,
		// (int) lon1);

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
	public OpenResult open(MapOptions options) {
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
