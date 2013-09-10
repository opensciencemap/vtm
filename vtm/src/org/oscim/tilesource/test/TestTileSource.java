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
import org.oscim.tilesource.TileSource;

public class TestTileSource extends TileSource {

	private final MapInfo mMapInfo =
	        new MapInfo(new BoundingBox(-180, -90, 180, 90),
	                    new Byte((byte) 5), null, null, 0, 0, 0,
	                    "", "", "", null);

	// private boolean mOpenFile = false;

	@Override
	public ITileDataSource getDataSource() {
		return new TileDataSource();
	}

	@Override
	public OpenResult open() {
		// mOpenFile =true;
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		// mOpenFile = false;
	}

	@Override
	public MapInfo getMapInfo() {
		return mMapInfo;
	}

	static class TileDataSource implements ITileDataSource {

		private static final Tag[] mTags = {
		        new Tag("natural", "water")
		};
		private static final Tag[] mTagsWay = {
		        new Tag("highway", "primary"),
		        new Tag("name", "Highway Rd")
		};
		private static final Tag[] mTagsBoundary = {
		        new Tag("boundary", "administrative"),
		        new Tag("admin_level", "2")
		};

		private static final Tag[] mTagsPlace = {
		        new Tag("place", "city"),
		        null
		};

		private final MapElement mElem;

		public TileDataSource() {
			mElem = new MapElement();
		}

		private boolean renderWays = true;
		private boolean renderBoundary = true;
		private boolean renderPlace = false;

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

			e.setLayer(0);
			e.tags.set(mTags);
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

				// //e.setLayer(mTagsWay, 0);
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

				e.setLayer(1);
				e.tags.set(mTagsWay);
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

				e.setLayer(1);
				e.tags.set(mTagsBoundary);
				mapDataSink.process(e);
			}

			if (renderPlace) {
				e.clear();
				e.startPoints();
				e.addPoint(size / 2, size / 2);

				mTagsPlace[1] = new Tag("name", tile.toString());
				e.tags.set(mTagsPlace);
				mapDataSink.process(e);
			}

			mapDataSink.completed(true);

			return QueryResult.SUCCESS;
		}

		@Override
		public void destroy() {
		}
	}

}
