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
package org.oscim.tiling.source;

import static org.oscim.tiling.ITileDataSink.QueryResult.FAILED;
import static org.oscim.tiling.ITileDataSink.QueryResult.SUCCESS;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.layers.tile.LoadDelayTask;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlTileDataSource implements ITileDataSource {
	static final Logger log = LoggerFactory.getLogger(UrlTileDataSource.class);

	protected final LwHttp mConn;
	protected final ITileDecoder mTileDecoder;
	protected final UrlTileSource mTileSource;

	public UrlTileDataSource(UrlTileSource tileSource, ITileDecoder tileDecoder, HttpEngine conn) {
		mTileSource = tileSource;
		mTileDecoder = tileDecoder;
		mConn = (LwHttp) conn;
	}

	UrlTileSource getTileSource() {
		return mTileSource;
	}

	private ITileDataSink mSink;
	private MapTile mTile;

	@Override
	public void query(MapTile tile, ITileDataSink sink) {
		mTile = tile;
		mSink = sink;
		try {
			mConn.sendRequest(tile, this);
		} catch (Exception e) {
			///e.printStackTrace();
			log.error("{} {}", mTile, e.getMessage());
			sink.completed(FAILED);
		}
	}

	public void process(final InputStream is) {
		TileLoader.postLoadDelay(new LoadDelayTask<InputStream>(mTile, mSink, is) {

			@Override
			public void continueLoading() {

				if (tile.state(MapTile.State.LOADING)) {
					boolean win = false;
					if (is != null) {
						try {
							win = mTileDecoder.decode(tile, sink, data);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (!win)
						log.debug("{} failed", tile);

					// FIXME
					mConn.requestCompleted(true);

					sink.completed(win ? SUCCESS : FAILED);
				} else {
					// FIXME
					mConn.requestCompleted(false);
					sink.completed(FAILED);
				}

				mTile = null;
				mSink = null;
			}
		});

	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
