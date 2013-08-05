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
package org.oscim.tilesource.common;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.Log;
import org.oscim.layers.tile.MapTile;
import org.oscim.tilesource.ITileDataSink;
import org.oscim.tilesource.ITileDataSource;

/**
 *
 *
 */
public abstract class PbfTileDataSource implements ITileDataSource {
	private static final String TAG = PbfTileDataSource.class.getName();

	protected LwHttp mConn;
	protected final PbfDecoder mTileDecoder;

	public PbfTileDataSource(PbfDecoder tileDecoder) {
		mTileDecoder = tileDecoder;
	}

	private ITileDataSink mSink;
	private MapTile mTile;

	@Override
	public QueryResult executeQuery(MapTile tile, ITileDataSink sink) {
		QueryResult result = QueryResult.SUCCESS;
		mTile = tile;
		mSink = sink;
		try {
			mConn.sendRequest(tile, this);
		} catch (Exception e) {
			e.printStackTrace();
			result = QueryResult.FAILED;
		}

		return result;
	}

	public void process(InputStream is, int length) {

		boolean win = false;
		if (length >= 0) {
			try {
				win = mTileDecoder.decode(mTile, mSink, is, length);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (!win)
			Log.d(TAG, mTile + " failed");

		mConn.requestCompleted();
		mSink.completed(win);
	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
