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
package org.oscim.tiling.source.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.tiling.MapTile;
import org.oscim.tiling.source.ITileCache;
import org.oscim.tiling.source.ITileCache.TileReader;
import org.oscim.tiling.source.ITileCache.TileWriter;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.ITileDecoder;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlTileDataSource implements ITileDataSource {
	static final Logger log = LoggerFactory.getLogger(UrlTileDataSource.class);

	protected final LwHttp mConn;
	protected final ITileDecoder mTileDecoder;
	protected final UrlTileSource mTileSource;
	protected final boolean mUseCache;

	public UrlTileDataSource(UrlTileSource tileSource, ITileDecoder tileDecoder, LwHttp conn) {
		mTileDecoder = tileDecoder;
		mTileSource = tileSource;
		mUseCache = (tileSource.tileCache != null);
		mConn = conn;
	}

	@Override
	public QueryResult executeQuery(MapTile tile, ITileDataSink sink) {
		ITileCache cache = mTileSource.tileCache;

		if (mUseCache) {
			TileReader c = cache.getTile(tile);
			if (c != null) {
				InputStream is = c.getInputStream();
				try {
					if (mTileDecoder.decode(tile, sink, is))
						return QueryResult.SUCCESS;

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
			mConn.requestCompleted(success);

			if (cacheWriter != null)
				cacheWriter.complete(success);
		}
		return success ? QueryResult.SUCCESS : QueryResult.FAILED;
	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
