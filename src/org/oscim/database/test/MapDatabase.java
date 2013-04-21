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

import static org.oscim.core.MapElement.GEOM_LINE;
import static org.oscim.core.MapElement.GEOM_POINT;
import static org.oscim.core.MapElement.GEOM_POLY;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
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

	private final MapElement mElem;

	public MapDatabase() {
		mElem = new MapElement();
	}

	@Override
	public QueryResult executeQuery(JobTile tile,
			IMapDatabaseCallback mapDatabaseCallback) {

		int size = Tile.SIZE;
		MapElement e = mElem;

		float x1 = -1;
		float y1 = -1;
		float x2 = size + 1;
		float y2 = size + 1;

		// always clear geometry before starting
		// a different type.
		e.clear();
		e.startPolygon();
		e.addPoint(x1, y1);
		e.addPoint(x2, y1);
		e.addPoint(x2, y2);
		e.addPoint(x1, y2);

		y1 = 40;
		y2 = size - 40;
		x1 = 40;
		x2 = size - 40;

		e.startHole();
		e.addPoint(x1, y1);
		e.addPoint(x2, y1);
		e.addPoint(x2, y2);
		e.addPoint(x1, y2);

		e.set(mTags, 0, GEOM_POLY);
		mapDatabaseCallback.renderElement(e);

		//--------------
		e.clear();

		// middle horizontal
		e.startLine();
		e.addPoint(0, size / 2);
		e.addPoint(size, size / 2);

		// center up
		e.startLine();
		e.addPoint(size / 2, -size / 2);
		e.addPoint(size / 2, size / 2);

		// center down
		e.startLine();
		e.addPoint(size / 2, size / 2);
		e.addPoint(size / 2, size / 2 + size);

		e.set(mTagsWay, 0, GEOM_LINE);
		mapDatabaseCallback.renderElement(e);

		//--------------
		e.clear();
		// left-top to center
		e.startLine();
		e.addPoint(size / 2, size / 2);
		e.addPoint(10, 10);

		e.startLine();
		e.addPoint(0, 10);
		e.addPoint(size, 10);

		e.startLine();
		e.addPoint(10, 0);
		e.addPoint(10, size);

		e.set(mTagsWay, 1, GEOM_LINE);
		mapDatabaseCallback.renderElement(e);

		//--------------
		e.clear();
		e.startPolygon();
		float r = size / 2;

		for (int i = 0; i < 360; i += 4) {
			double d = Math.toRadians(i);
			e.addPoint(r + (float) Math.cos(d) * (r - 40),
					r + (float) Math.sin(d) * (r - 40));
		}

		e.set(mTagsBoundary, 1, GEOM_LINE);
		mapDatabaseCallback.renderElement(e);

		//--------------
		e.clear();
		e.startPoints();
		e.addPoint(size/2, size/2);

		mTagsPlace[1] = new Tag("name", tile.toString());
		e.set(mTagsPlace, 0, GEOM_POINT);
		mapDatabaseCallback.renderElement(e);

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
