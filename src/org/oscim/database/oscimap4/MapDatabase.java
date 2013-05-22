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
package org.oscim.database.oscimap4;

import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.database.IMapDataSink;
import org.oscim.database.IMapDatabase;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.database.common.LwHttp;
import org.oscim.layers.tile.MapTile;

import android.util.Log;

/**
 *
 *
 */
public class MapDatabase implements IMapDatabase {
	private static final String TAG = MapDatabase.class.getName();

	static final boolean USE_CACHE = false;

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author", null);
	// 'open' state
	private boolean mOpen = false;
	private LwHttp conn;
	private TileDecoder mTileDecoder;

	@Override
	public QueryResult executeQuery(MapTile tile, IMapDataSink sink) {
		QueryResult result = QueryResult.SUCCESS;

		try {
			InputStream is;
			if (!conn.sendRequest(tile)) {
				Log.d(TAG, tile + " Request Failed");
				result = QueryResult.FAILED;
			} else if ((is = conn.readHeader()) != null) {
				boolean win = mTileDecoder.decode(is, tile, sink);
				if (!win)
					Log.d(TAG, tile + " failed");
			} else {
				Log.d(TAG, tile + " Network Error");
				result = QueryResult.FAILED;
			}
		} catch (SocketException e) {
			Log.d(TAG, tile + " Socket exception: " + e.getMessage());
			result = QueryResult.FAILED;
		} catch (SocketTimeoutException e) {
			Log.d(TAG, tile + " Socket Timeout");
			result = QueryResult.FAILED;
		} catch (UnknownHostException e) {
			Log.d(TAG, tile + " No Network");
			result = QueryResult.FAILED;
		} catch (Exception e) {
			e.printStackTrace();
			result = QueryResult.FAILED;
		}

		conn.requestCompleted();

		if (result == QueryResult.SUCCESS) {

			//conn.cacheFinish(tile, f, true);
		} else {
			//conn.cacheFinish(tile, f, false);
			conn.close();
		}
		return result;
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
		return mOpen;
	}

	@Override
	public OpenResult open(MapOptions options) {
		String extension = ".vtm";

		if (mOpen)
			return OpenResult.SUCCESS;

		if (options == null || !options.containsKey("url"))
			return new OpenResult("No URL in MapOptions");

		conn = new LwHttp();

		if (!conn.setServer(options.get("url"), extension, false)) {
			return new OpenResult("invalid url: " + options.get("url"));
		}

		mOpen = true;
		mTileDecoder = new TileDecoder();

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpen = false;
		conn.close();
	}

	@Override
	public void cancel() {
	}
}
