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
package org.mapsforge.database.pbmap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.core.WebMercator;
import org.mapsforge.database.FileOpenResult;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.MapFileInfo;

import android.util.Log;

/**
 * 
 *
 */
public class MapDatabase implements IMapDatabase {
	private static final String TAG = "MapDatabase";

	private static final MapFileInfo mMapInfo =
			new MapFileInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 14), new GeoPoint(53.11, 8.85),
					WebMercator.NAME, 0, 0, 0, "de", "comment", "author");

	private boolean mOpenFile = false;

	// private static final String URL = "http://city.informatik.uni-bremen.de:8020/test/%d/%d/%d.osmtile";
	private static final String URL = "http://city.informatik.uni-bremen.de/osmstache/test/%d/%d/%d.osmtile";
	private static final Header encodingHeader =
			new BasicHeader("Accept-Encoding", "gzip");

	private static volatile HashMap<String, Tag> tagHash = new HashMap<String, Tag>(100);

	private Tag[] curTags = new Tag[1000];
	private int mCurTagCnt;

	// private AndroidHttpClient mClient;
	private HttpClient mClient;
	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;

	@Override
	public void executeQuery(Tile tile, IMapDatabaseCallback mapDatabaseCallback) {
		mCanceled = false;

		if (mClient == null)
			createClient();

		String url = String.format(URL, Integer.valueOf(tile.zoomLevel),
				Long.valueOf(tile.tileX), Long.valueOf(tile.tileY));

		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader(encodingHeader);
		mMapGenerator = mapDatabaseCallback;
		mCurTagCnt = 0;
		// using variable coordinate scalefactor to take advantage of
		// variable byte encoded integers
		mScaleFactor = 1 / 100f;
		if (tile.zoomLevel < 12)
			mScaleFactor = (float) Math.pow(2, (12 - tile.zoomLevel)) / 100f;

		try {
			HttpResponse response = mClient.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				Log.d(TAG, "Http response " + statusCode);
				return;
			}

			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				Log.d(TAG, "Somethings wrong? - no entity " + statusCode);
				return;
			}

			InputStream is = null;
			GZIPInputStream zis = null;
			try {
				// is = AndroidHttpClient.getUngzippedContent(entity);

				is = entity.getContent();
				zis = new GZIPInputStream(is);

				decode(zis);

			} finally {
				if (zis != null)
					zis.close();
				if (is != null)
					is.close();
				entity.consumeContent();
			}
		} catch (SocketException ex) {
			Log.d(TAG, "Socket exception: " + ex.getMessage());
		} catch (SocketTimeoutException ex) {
			Log.d(TAG, "Socket exception: " + ex.getMessage());
		} catch (Exception ex) {
			getRequest.abort();
			ex.printStackTrace();
		}
	}

	@Override
	public String getMapProjection() {
		return WebMercator.NAME;
	}

	@Override
	public MapFileInfo getMapFileInfo() {
		return mMapInfo;
	}

	@Override
	public boolean hasOpenFile() {
		return mOpenFile;
	}

	private void createClient() {
		// mClient = AndroidHttpClient.newInstance("Android");

		mOpenFile = true;
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);
		mClient = new DefaultHttpClient(params);
		HttpClientParams.setRedirecting(params, false);
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http",
				PlainSocketFactory.getSocketFactory(), 80));
	}

	@Override
	public FileOpenResult openFile(File mapFile) {
		createClient();

		return new FileOpenResult();
	}

	@Override
	public void closeFile() {
		mOpenFile = false;
		if (mClient != null)
			mClient.getConnectionManager().shutdown();
	}

	@Override
	public String readString(int position) {
		return null;
	}

	private static final int BUFFER_SIZE = 65536;

	private final byte[] buffer = new byte[BUFFER_SIZE];
	private int bufferPos;
	private int bufferSize;
	private int bytesRead;
	private InputStream inputStream;

	private static final int TAG_TILE_TAGS = 1;
	private static final int TAG_TILE_WAYS = 2;
	private static final int TAG_TILE_NODES = 3;
	private static final int TAG_WAY_TAGS = 1;
	private static final int TAG_WAY_INDEX = 2;
	private static final int TAG_WAY_COORDS = 3;
	private static final int TAG_WAY_LAYER = 4;

	// private static final int TAG_NODE_TAGS = 1;
	// private static final int TAG_NODE_COORDS = 2;

	private boolean decode(InputStream is) throws IOException {
		inputStream = is;
		bytesRead = 0;
		bufferSize = 0;
		bufferPos = 0;
		int val;

		while ((val = decodeVarint32()) > 0) {
			// read tag and wire type
			int tag = (val >> 3);
			// int wireType = (val & 7);
			// Log.d(TAG, "tile " + tag + " " + wireType);

			switch (tag) {
				case TAG_TILE_TAGS:
					decodeTileTags();
					break;

				case TAG_TILE_WAYS:
					decodeTileWays();
					break;

				case TAG_TILE_NODES:
					decodeTileNodes();
					break;

				default:
					Log.d(TAG, "invalid type for tile: " + tag);
					return false;
			}
		}
		return true;
	}

	private boolean decodeTileTags() throws IOException {
		String tagString = decodeString();

		Tag tag = tagHash.get(tagString);
		if (tag == null) {
			tag = new Tag(tagString);
			tagHash.put(tagString, tag);
		}
		curTags[mCurTagCnt++] = tag;

		// Log.d(TAG, "tag:" + tag);
		return true;
	}

	private boolean decodeTileWays() throws IOException {
		int bytes = decodeVarint32();

		int end = bytesRead + bytes;
		int indexCnt = 0;
		int tagCnt = 0;
		int coordCnt = 0;
		int layer = 0;

		while (bytesRead < end) {
			// read tag and wire type

			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);
			// int wireType = val & 7;

			// Log.d(TAG, "way " + tag + " " + wireType + " bytes:" + bytes);

			switch (tag) {
				case TAG_WAY_TAGS:
					tagCnt = decodeWayTags();
					break;

				case TAG_WAY_INDEX:
					indexCnt = decodeWayIndices();
					break;

				case TAG_WAY_COORDS:
					coordCnt = decodeWayCoordinates();
					break;

				case TAG_WAY_LAYER:
					layer = decodeVarint32();
					break;

				default:
					Log.d(TAG, "invalid type for way: " + tag);
			}
		}

		if (indexCnt == 0 || tagCnt == 0)
			return false;

		int[] index = new int[indexCnt];

		int sum = 0;
		for (int i = 0; i < indexCnt; i++) {
			index[i] = tmpIndices[i] * 2;
			sum += index[i];
		}

		Tag[] tags = new Tag[tagCnt];
		for (int i = 0; i < tagCnt; i++)
			tags[i] = curTags[tmpTags[i]];

		float[] coords = tmpCoords;
		int pos = 0;

		if (coordCnt != sum) {
			Log.d(TAG, "way length is wrong " + coordCnt + " " + sum);
			return false;
		}

		float z = mScaleFactor;
		for (int j = 0, m = indexCnt; j < m; j++) {
			float lastX = 0;
			float lastY = 0;

			for (int n = index[j] + pos; pos < n; pos += 2) {
				lastX = coords[pos] = (coords[pos] * z) + lastX;
				lastY = coords[pos + 1] = (coords[pos + 1] * z) + lastY;
			}

		}

		mMapGenerator.renderWay((byte) layer, tags, coords, index, true);
		return true;
	}

	private boolean decodeTileNodes() throws IOException {
		int bytes = decodeVarint32();
		Log.d(TAG, "way nodes " + bytes);
		return true;
	}

	private int MAX_WAY_COORDS = 32768;
	private int MAX_WAY_INDICES = 1000;
	private int[] tmpTags = new int[32];
	private int[] tmpIndices = new int[MAX_WAY_INDICES];
	private float[] tmpCoords = new float[MAX_WAY_COORDS];

	// private boolean ensureBufferSize(int size) throws IOException {
	// if (size > (bufferSize - bufferPos))
	// readBuffer(size - (bufferSize - bufferPos));
	//
	// return true;
	// }

	private int decodeWayTags() throws IOException {
		int bytes = decodeVarint32();
		// Log.d(TAG, "way tags: " + bytes);

		int cnt = 0;
		int end = bytesRead + bytes;

		while (bytesRead < end)
			tmpTags[cnt++] = decodeVarint32();

		return cnt;
	}

	private int decodeWayIndices() throws IOException {
		int bytes = decodeVarint32();
		// Log.d(TAG, "way indices: " + bytes);

		int cnt = 0;
		int end = bytesRead + bytes;

		while (bytesRead < end) {
			int val = decodeVarint32();
			if (cnt >= MAX_WAY_INDICES) {

				MAX_WAY_INDICES += 128;
				Log.d(TAG, "increase indices array " + MAX_WAY_INDICES);
				int[] tmp = new int[MAX_WAY_INDICES];
				System.arraycopy(tmpIndices, 0, tmp, 0, cnt);
				tmpIndices = tmp;
			}

			tmpIndices[cnt++] = val;

		}
		return cnt;
	}

	private int decodeWayCoordinates() throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);

		int pos = bufferPos;
		int end = pos + bytes;
		float[] coords = tmpCoords;
		byte[] buf = buffer;
		int cnt = 0;
		int result;

		// read repeated sint32
		while (pos < end) {
			if (cnt >= MAX_WAY_COORDS) {
				MAX_WAY_COORDS += 128;
				float[] tmp = new float[MAX_WAY_COORDS];
				System.arraycopy(coords, 0, tmp, 0, cnt);
				tmpCoords = coords = tmp;
			}

			byte tmp = buf[pos++];
			if (tmp >= 0) {
				result = tmp;
			} else {
				result = tmp & 0x7f;
				if ((tmp = buf[pos++]) >= 0) {
					result |= tmp << 7;
				} else {
					result |= (tmp & 0x7f) << 7;
					if ((tmp = buf[pos++]) >= 0) {
						result |= tmp << 14;
					} else {
						result |= (tmp & 0x7f) << 14;
						if ((tmp = buf[pos++]) >= 0) {
							result |= tmp << 21;
						} else {
							result |= (tmp & 0x7f) << 21;
							result |= (tmp = buf[pos++]) << 28;

							if (tmp < 0) {
								int i = 0;
								// Discard upper 32 bits.
								while (i++ < 5) {
									if (buf[pos++] >= 0)
										break;
								}

								if (i == 5)
									// FIXME throw some poo
									Log.d(TAG, "EEK malformedVarint");
							}
						}
					}
				}
			}
			coords[cnt++] = (result >>> 1) ^ -(result & 1);
		}

		bufferPos = pos;
		bytesRead += bytes;

		// while (bytesRead < end) {
		// int val = decodeZigZag32(decodeVarint32());
		// if (cnt >= MAX_WAY_COORDS) {
		// MAX_WAY_COORDS += 128;
		// Log.d(TAG, "increase coords array  " + MAX_WAY_COORDS);
		// float[] tmp = new float[MAX_WAY_COORDS];
		// System.arraycopy(tmpCoords, 0, tmp, 0, cnt);
		// tmpCoords = tmp;
		// }
		// tmpCoords[cnt++] = val;
		// }
		return cnt;
	}

	private void readBuffer() throws IOException {

		int len = inputStream.read(buffer, 0, BUFFER_SIZE);
		if (len < 0) {
			buffer[bufferPos] = 0;
			// Log.d(TAG, " nothing to read...  pos " + bufferPos + ", size "
			// + bufferSize + ", read " + bytesRead);
			return;
		}
		bufferSize = len;
		bufferPos = 0;

		// Log.d(TAG, "pos " + bufferPos + ", size " + bufferSize + ", read "
		// + bytesRead);
	}

	private void readBuffer(int size) throws IOException {
		if (size < (bufferSize - bufferPos))
			return;

		if (size > BUFFER_SIZE) {
			// FIXME throw exception for now, but frankly better
			// sanitize tile data on compilation.
			// this only happen with strings or coordinates larger than 64kb
			throw new IOException("EEEK requested size too large");
		}

		if ((size - bufferSize) + bufferPos > BUFFER_SIZE) {
			// copy bytes left to read from buffer to the beginning of buffer
			System.arraycopy(buffer, bufferPos, buffer, 0, bufferSize - bufferPos);
			bufferPos = 0;
		}

		while ((bufferSize - bufferPos) < size) {
			// read until requested size is available in buffer
			int len = inputStream.read(buffer, bufferSize, BUFFER_SIZE - bufferSize);
			if (len < 0) {
				buffer[bufferSize - 1] = 0; // FIXME is this needed?
				break;
			}
			bufferSize += len;
		}

		// Log.d(TAG, "needed " + size + " pos " + bufferPos + ", size "
		// + bufferSize
		// + ", read " + bytesRead);
	}

	/* All code below is taken from or based on Google's Protocol Buffers implementation: */

	// Protocol Buffers - Google's data interchange format
	// Copyright 2008 Google Inc. All rights reserved.
	// http://code.google.com/p/protobuf/
	//
	// Redistribution and use in source and binary forms, with or without
	// modification, are permitted provided that the following conditions are
	// met:
	//
	// * Redistributions of source code must retain the above copyright
	// notice, this list of conditions and the following disclaimer.
	// * Redistributions in binary form must reproduce the above
	// copyright notice, this list of conditions and the following disclaimer
	// in the documentation and/or other materials provided with the
	// distribution.
	// * Neither the name of Google Inc. nor the names of its
	// contributors may be used to endorse or promote products derived from
	// this software without specific prior written permission.
	//
	// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
	// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
	// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
	// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
	// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
	// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
	// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
	// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

	private byte readRawByte() throws IOException {
		if (bufferPos == bufferSize) {
			readBuffer();
		}
		bytesRead++;
		return buffer[bufferPos++];
	}

	private int decodeVarint32() throws IOException {
		byte tmp = readRawByte();
		if (tmp >= 0) {
			return tmp;
		}
		int result = tmp & 0x7f;
		if ((tmp = readRawByte()) >= 0) {
			return result | tmp << 7;
		}
		result |= (tmp & 0x7f) << 7;
		if ((tmp = readRawByte()) >= 0) {
			return result | tmp << 14;
		}
		result |= (tmp & 0x7f) << 14;
		if ((tmp = readRawByte()) >= 0) {
			return result | tmp << 21;
		}
		result |= (tmp & 0x7f) << 21;
		result |= (tmp = readRawByte()) << 28;

		if (tmp < 0) {
			// Discard upper 32 bits.
			for (int i = 0; i < 5; i++) {
				if (readRawByte() >= 0) {
					return result;
				}
			}
			Log.d(TAG, "EEK malformedVarint");
			// FIXME throw some poo
		}

		return result;
	}

	private String decodeString() throws IOException {
		final int size = decodeVarint32();

		readBuffer(size);

		final String result = new String(buffer, bufferPos, size, "UTF-8");
		bufferPos += size;
		bytesRead += size;
		return result;

	}

	public static int decodeZigZag32(final int n) {
		return (n >>> 1) ^ -(n & 1);
	}

	private boolean mCanceled;

	@Override
	public void cancel() {
		mCanceled = true;
		mClient.getConnectionManager().shutdown();
		mClient = null;
	}

}
