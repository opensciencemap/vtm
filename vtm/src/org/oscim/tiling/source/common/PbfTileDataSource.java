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
package org.oscim.tiling.source.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.tiling.MapTile;
import org.oscim.tiling.source.ITileCache;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public abstract class PbfTileDataSource implements ITileDataSource {
	static final Logger log = LoggerFactory.getLogger(PbfTileDataSource.class);

	protected LwHttp mConn;
	protected final PbfDecoder mTileDecoder;
	protected final ITileCache mTileCache;

	public PbfTileDataSource(PbfDecoder tileDecoder, ITileCache tileCache) {
		mTileDecoder = tileDecoder;
		mTileCache = tileCache;
	}

	@Override
	public QueryResult executeQuery(MapTile tile, ITileDataSink sink) {
		boolean success = true;

		ITileCache.TileWriter cacheWriter = null;

		if (mTileCache != null) {
			ITileCache.TileReader c = mTileCache.getTile(tile);
			if (c == null) {
				// create new cache entry
				cacheWriter = mTileCache.writeTile(tile);
				mConn.setOutputStream(cacheWriter.getOutputStream());
			} else {
				try {
					InputStream is = c.getInputStream();
					if (mTileDecoder.decode(tile, sink, is, c.getBytes())) {
						IOUtils.closeQuietly(is);
						return QueryResult.SUCCESS;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				log.debug(tile + " Cache read failed");
			}
		}

		try {
			InputStream is;
			if (!mConn.sendRequest(tile)) {
				log.debug(tile + " Request failed");
				success = false;
			} else if ((is = mConn.readHeader()) != null) {
				int bytes = mConn.getContentLength();
				success = mTileDecoder.decode(tile, sink, is, bytes);
				if (!success)
					log.debug(tile + " Decoding failed");
			} else {
				log.debug(tile + " Network Error");
				success = false;
			}
		} catch (SocketException e) {
			log.debug(tile + " Socket exception: " + e.getMessage());
			success = false;
		} catch (SocketTimeoutException e) {
			log.debug(tile + " Socket Timeout");
			success = false;
		} catch (UnknownHostException e) {
			log.debug(tile + " No Network");
			success = false;
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}

		mConn.requestCompleted();

		if (cacheWriter != null)
			cacheWriter.complete(success);

		if (success)
			mConn.close();

		return success ? QueryResult.SUCCESS : QueryResult.FAILED;
	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
