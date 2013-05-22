/*
 * Copyright 2013
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
package org.oscim.database.mapnik;

import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.Tile;
import org.oscim.database.IMapDataSink;
import org.oscim.database.IMapDatabase;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.database.common.LwHttp;
import org.oscim.layers.tile.MapTile;

import android.util.Log;

public class MapDatabase implements IMapDatabase {
	private static final String TAG = MapDatabase.class.getName();

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author",
					new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }
			);

	// 'open' state
	private boolean mOpen = false;

	private LwHttp conn;
	private TileDecoder mTileDecoder;

	@Override
	public QueryResult executeQuery(MapTile tile, IMapDataSink mapDataSink) {
		QueryResult result = QueryResult.SUCCESS;

		try {
			InputStream is;
			if (conn.sendRequest(tile) && (is = conn.readHeader()) != null) {
				mTileDecoder.decode(is, tile, mapDataSink);
			} else {
				Log.d(TAG, tile + " Network Error");
				result = QueryResult.FAILED;
			}
		} catch (SocketException ex) {
			Log.d(TAG, tile + " Socket exception: " + ex.getMessage());
			result = QueryResult.FAILED;
		} catch (SocketTimeoutException ex) {
			Log.d(TAG, tile + " Socket Timeout exception: " + ex.getMessage());
			result = QueryResult.FAILED;
		} catch (UnknownHostException ex) {
			Log.d(TAG, tile + " no network");
			result = QueryResult.FAILED;
		} catch (Exception ex) {
			ex.printStackTrace();
			result = QueryResult.FAILED;
		}

		conn.requestCompleted();

		if (result != QueryResult.SUCCESS) {
			conn.close();
		}

		return result;
	}

	@Override
	public MapInfo getMapInfo() {
		return mMapInfo;
	}

	@Override
	public boolean isOpen() {
		return mOpen;
	}

	@Override
	public OpenResult open(MapOptions options) {
		String extension = ".vector.pbf";
		if (mOpen)
			return OpenResult.SUCCESS;

		if (options == null || !options.containsKey("url"))
			return new OpenResult("options missing");

		conn = new LwHttp() {

			@Override
			protected int formatTilePath(Tile tile, byte[] path, int pos) {
				// url formatter for mapbox streets
				byte[] hexTable = {
						'0', '1', '2', '3', '4', '5', '6', '7',
						'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

				path[pos++] = '/';
				path[pos++] = hexTable[(tile.tileX) % 16];
				path[pos++] = hexTable[(tile.tileY) % 16];
				path[pos++] = '/';
				pos = LwHttp.writeInt(tile.zoomLevel, pos, path);
				path[pos++] = '/';
				pos = LwHttp.writeInt(tile.tileX, pos, path);
				path[pos++] = '/';
				pos = LwHttp.writeInt(tile.tileY, pos, path);
				return pos;
			}
		};

		if (!conn.setServer(options.get("url"), extension, true)) {
			return new OpenResult("invalid url: " + options.get("url"));
		}

		mTileDecoder = new TileDecoder();

		mOpen = true;
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpen = false;

		mTileDecoder = null;
		conn.close();
		conn = null;
	}

	@Override
	public String getMapProjection() {
		return null;
	}

	@Override
	public void cancel() {
	}
}
