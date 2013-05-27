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
package org.oscim.tilesource.test;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.tilesource.ITileDataSink;
import org.oscim.tilesource.ITileDataSource;
import org.oscim.tilesource.MapInfo;

/**
 *
 *
 */
public class TestTileSource implements ITileDataSource {

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

	private final boolean mOpenFile = false;

	private final MapElement mElem;

	public TestTileSource() {
		mElem = new MapElement();
	}

	private final boolean renderWays = false;
	private final boolean renderBoundary = false;
	private final boolean renderPlace = false;

	@Override
	public QueryResult executeQuery(MapTile tile,
			ITileDataSink mapDataSink) {

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

		y1 = 5;
		y2 = size - 5;
		x1 = 5;
		x2 = size - 5;

		e.startHole();
		e.addPoint(x1, y1);
		e.addPoint(x2, y1);
		e.addPoint(x2, y2);
		e.addPoint(x1, y2);

		e.set(mTags, 0);
		mapDataSink.process(e);

		if (renderWays) {
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

			e.set(mTagsWay, 0);
			mapDataSink.process(e);

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

			e.set(mTagsWay, 1);
			mapDataSink.process(e);
		}

		if (renderBoundary) {
			e.clear();
			e.startPolygon();
			float r = size / 2;

			for (int i = 0; i < 360; i += 4) {
				double d = Math.toRadians(i);
				e.addPoint(r + (float) Math.cos(d) * (r - 40),
						r + (float) Math.sin(d) * (r - 40));
			}

			e.set(mTagsBoundary, 1);
			mapDataSink.process(e);
		}

		if (renderPlace) {
			e.clear();
			e.startPoints();
			e.addPoint(size / 2, size / 2);

			mTagsPlace[1] = new Tag("name", tile.toString());
			e.set(mTagsPlace, 0);
			mapDataSink.process(e);
		}
		return QueryResult.SUCCESS;
	}


//	@Override
//	public boolean isOpen() {
//		return mOpenFile;
//	}
//
//	@Override
//	public OpenResult open(MapOptions options) {
//		mOpenFile = true;
//		return OpenResult.SUCCESS;
//	}
//
//	@Override
//	public void close() {
//		mOpenFile = false;
//	}
//
//	@Override
//	public void cancel() {
//	}


	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
