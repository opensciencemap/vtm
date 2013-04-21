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

	private final Tag[] mTags = {
			new Tag("natural", "water")
	};
	private final Tag[] mTagsWay = {
			new Tag("highway", "primary"),
			new Tag("name", "Highway Rd")
	};
	private final Tag[] mTagsBoundary = {
			new Tag("boundary", "administrative"),
			new Tag("admin_level", "2")
	};
	private final Tag[] mTagsPlace = {
			 new Tag("place", "city"),
			 null
	};

	private final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 5), null, null, 0, 0, 0,
					"", "", "", null);

	private boolean mOpenFile = false;

	private final WayData mWay;
	private final GeometryBuffer mGeom;

	public MapDatabase() {
		mGeom = new GeometryBuffer(1, 1);
		mWay = new WayData();
		mWay.geom = mGeom;
	}

	@Override
	public QueryResult executeQuery(JobTile tile,
			IMapDatabaseCallback mapDatabaseCallback) {

		int size = Tile.SIZE;
		GeometryBuffer g = mGeom;

		float x1 = -1;
		float y1 = -1;
		float x2 = size + 1;
		float y2 = size + 1;
		g.clear();
		g.startPolygon();
		g.addPoint(x1, y1);
		g.addPoint(x2, y1);
		g.addPoint(x2, y2);
		g.addPoint(x1, y2);

		y1 = 40;
		y2 = size - 40;
		x1 = 40;
		x2 = size - 40;

		g.startHole();
		g.addPoint(x1, y1);
		g.addPoint(x2, y1);
		g.addPoint(x2, y2);
		g.addPoint(x1, y2);

		mWay.tags = mTags;
		mWay.layer = 0;
		mWay.closed = true;

		mapDatabaseCallback.renderWay(mWay);

		g.clear();

		// middle horizontal
		g.startLine();
		g.addPoint(0, size / 2);
		g.addPoint(size, size / 2);

		// center up
		g.startLine();
		g.addPoint(size / 2, -size / 2);
		g.addPoint(size / 2, size / 2);

		// center down
		g.startLine();
		g.addPoint(size / 2, size / 2);
		g.addPoint(size / 2, size / 2 + size);

		mWay.closed = false;
		mWay.layer = 0;
		mWay.tags = mTagsWay;
		mapDatabaseCallback.renderWay(mWay);

		g.clear();

		// left-top to center
		g.startLine();
		g.addPoint(size / 2, size / 2);
		g.addPoint(10, 10);

		g.startLine();
		g.addPoint(0, 10);
		g.addPoint(size, 10);

		g.startLine();
		g.addPoint(10, 0);
		g.addPoint(10, size);

		mWay.closed = false;
		mWay.layer = 1;
		mWay.tags = mTagsWay;
		mapDatabaseCallback.renderWay(mWay);

		g.clear();
		g.startPolygon();
		float r = size / 2;

		for (int i = 0; i < 360; i += 4) {
			double d = Math.toRadians(i);
			g.addPoint(r + (float) Math.cos(d) * (r - 40), r + (float) Math.sin(d) * (r - 40));
		}

		mWay.closed = true;
		mWay.layer = 1;
		mWay.tags = mTagsBoundary;
		mapDatabaseCallback.renderWay(mWay);



		g.clear();
		g.startPoints();
		g.addPoint(size/2, size/2);
		mTagsPlace[1] = new Tag("name", tile.toString());

		mapDatabaseCallback.renderPOI((byte)0, mTagsPlace, g);

		return QueryResult.SUCCESS;
	}

	@Override
	public String getMapProjection() {
		return null;
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
