/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileCache;
import org.oscim.tiling.ITileCache.TileReader;
import org.oscim.tiling.ITileCache.TileWriter;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlTileDataSource implements ITileDataSource {
	static final Logger log = LoggerFactory.getLogger(UrlTileDataSource.class);

	protected final HttpEngine mConn;
	protected final ITileDecoder mTileDecoder;
	protected final UrlTileSource mTileSource;
	protected final boolean mUseCache;

	public UrlTileDataSource(UrlTileSource tileSource, ITileDecoder tileDecoder, HttpEngine conn) {
		mTileDecoder = tileDecoder;
		mTileSource = tileSource;
		mUseCache = (tileSource.tileCache != null);
		mConn = conn;
	}

	@Override
	public void query(MapTile tile, ITileDataSink sink) {
		ITileCache cache = mTileSource.tileCache;

		if (mUseCache) {
			TileReader c = cache.getTile(tile);
			if (c != null) {
				InputStream is = c.getInputStream();
				try {
					if (mTileDecoder.decode(tile, sink, is)) {
						sink.completed(SUCCESS);
						return;
					}
				} catch (IOException e) {
					log.debug("{} Cache read: {}", tile, e);
				} finally {
					IOUtils.closeQuietly(is);
				}
			}
		}

		boolean success = false;
		TileWriter cacheWriter = null;
		try {
			InputStream is;
			if (!mConn.sendRequest(mTileSource, tile)) {
				log.debug("{} Request failed", tile);
			} else if ((is = mConn.read()) == null) {
				log.debug("{} Network Error", tile);
			} else {
				if (mUseCache) {
					cacheWriter = cache.writeTile(tile);
					mConn.setCache(cacheWriter.getOutputStream());
				}
				success = mTileDecoder.decode(tile, sink, is);
			}
		} catch (SocketException e) {
			log.debug("{} Socket exception: {}", tile, e.getMessage());
		} catch (SocketTimeoutException e) {
			log.debug("{} Socket Timeout", tile);
		} catch (UnknownHostException e) {
			log.debug("{} Unknown host: {}", tile, e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			success = mConn.requestCompleted(success);
			if (cacheWriter != null)
				cacheWriter.complete(success);
		}
		sink.completed(success ? SUCCESS : FAILED);
	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
