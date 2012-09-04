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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.database.FileOpenResult;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.MapFileInfo;
import org.mapsforge.database.QueryResult;

import android.os.Environment;
import android.util.Log;

/**
 * 
 *
 */
public class MapDatabase implements IMapDatabase {
	private static final String TAG = "MapDatabase";

	private static final MapFileInfo mMapInfo =
			new MapFileInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author");

	private boolean mOpenFile = false;

	private static final boolean USE_CACHE = false;

	private static final String CACHE_DIRECTORY = "/Android/data/org.mapsforge.app/cache/";
	private static final String CACHE_FILE = "%d-%d-%d.tile";

	// private static final String URL = "http://city.informatik.uni-bremen.de:8020/test/%d/%d/%d.osmtile";
	private static final String URL = "http://city.informatik.uni-bremen.de/osmstache/test/%d/%d/%d.osmtile";
	// private static final String URL = "http://city.informatik.uni-bremen.de/osmstache/gis2/%d/%d/%d.osmtile";
	// private static final Header encodingHeader =
	// new BasicHeader("Accept-Encoding", "gzip");

	private static final int MAX_TAGS_CACHE = 100;

	private static Map<String, Tag> tagHash = Collections
			.synchronizedMap(new LinkedHashMap<String, Tag>(
					MAX_TAGS_CACHE, 0.75f, true) {

				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Entry<String, Tag> e) {
					if (size() < MAX_TAGS_CACHE)
						return false;
					return true;
				}
			});

	private final static float REF_TILE_SIZE = 4096.0f;

	private int MAX_TILE_TAGS = 100;
	private Tag[] curTags = new Tag[MAX_TILE_TAGS];
	private int mCurTagCnt;

	private HttpClient mClient;
	private HttpGet mRequest = null;

	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;
	private Tile mTile;

	private FileOutputStream mCacheFile;

	@Override
	public QueryResult executeQuery(Tile tile, IMapDatabaseCallback mapDatabaseCallback) {
		// mCanceled = false;
		mCacheFile = null;

		// just used for debugging ....
		mTile = tile;

		// Log.d(TAG, "get tile >> : " + tile);

		mMapGenerator = mapDatabaseCallback;
		mCurTagCnt = 0;
		mScaleFactor = REF_TILE_SIZE / Tile.TILE_SIZE;
		File f;

		if (USE_CACHE) {
			f = new File(cacheDir, String.format(CACHE_FILE,
					Integer.valueOf(tile.zoomLevel),
					Long.valueOf(tile.tileX),
					Long.valueOf(tile.tileY)));

			if (f.exists()) {
				FileInputStream in;
				Log.d(TAG, "using cache: " + tile);

				try {
					in = new FileInputStream(f);
					decode(in);
					in.close();
					return QueryResult.SUCCESS;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		String url = String.format(URL,
				Integer.valueOf(tile.zoomLevel),
				Long.valueOf(tile.tileX),
				Long.valueOf(tile.tileY));

		HttpGet getRequest = new HttpGet(url);
		mRequest = getRequest;

		try {

			// HttpURLConnection urlConn = (HttpURLConnection) new URL(url).openConnection();
			// // urlConn.setUseCaches(false);
			//
			// InputStream in = urlConn.getInputStream();
			// try {
			// decode(in);
			// } finally {
			// urlConn.disconnect();
			// }

			HttpResponse response = mClient.execute(getRequest);
			final int statusCode = response.getStatusLine().getStatusCode();
			final HttpEntity entity = response.getEntity();

			if (statusCode != HttpStatus.SC_OK) {
				Log.d(TAG, "Http response " + statusCode);
				entity.consumeContent();
				return QueryResult.FAILED;
			}
			if (mTile.isCanceled) {
				Log.d(TAG, "1 loading canceled " + mTile);
				entity.consumeContent();

				return QueryResult.FAILED;
			}

			InputStream is = null;
			// GZIPInputStream zis = null;
			try {
				is = entity.getContent();

				if (USE_CACHE) {
					try {
						Log.d(TAG, "writing cache: " + tile);
						mCacheFile = new FileOutputStream(f);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}

				// zis = new GZIPInputStream(is);

				decode(is);

			} finally {
				// if (zis != null)
				// zis.close();
				if (is != null)
					is.close();
				entity.consumeContent();
			}
		} catch (SocketException ex) {
			Log.d(TAG, "Socket exception: " + ex.getMessage());
			// f.delete();
			return QueryResult.FAILED;
		} catch (SocketTimeoutException ex) {
			Log.d(TAG, "Socket Timeout exception: " + ex.getMessage());
			// f.delete();
			return QueryResult.FAILED;
		} catch (UnknownHostException ex) {
			Log.d(TAG, "no network");
			// f.delete();
			return QueryResult.FAILED;
		} catch (Exception ex) {
			// f.delete();
			ex.printStackTrace();
			return QueryResult.FAILED;
		}

		mRequest = null;

		if (mTile.isCanceled) {
			Log.d(TAG, "2 loading canceled " + mTile);
			return QueryResult.FAILED;
		}

		if (USE_CACHE) {
			try {
				mCacheFile.flush();
				mCacheFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mCacheFile = null;
		}

		return QueryResult.SUCCESS;
	}

	private static File cacheDir;

	@Override
	public String getMapProjection() {
		return null;
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
		mOpenFile = true;
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setStaleCheckingEnabled(params, false);

		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 32768);
		HttpClientParams.setRedirecting(params, false);
		// HttpClientParams.setCookiePolicy(params, CookiePolicy.ACCEPT_NONE);

		mClient = new DefaultHttpClient(params);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http",
				PlainSocketFactory.getSocketFactory(), 80));
	}

	@Override
	public FileOpenResult openFile(File mapFile) {

		createClient();

		if (USE_CACHE) {
			if (cacheDir == null) {
				// cacheDir = mapFile;

				String externalStorageDirectory = Environment
						.getExternalStorageDirectory()
						.getAbsolutePath();
				String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
				cacheDir = createDirectory(cacheDirectoryPath);

				Log.d(TAG, "cache dir: " + cacheDir);
			}
		}

		return new FileOpenResult();
	}

	@Override
	public void closeFile() {
		mOpenFile = false;
		if (mClient != null) {
			mClient.getConnectionManager().shutdown();
			mClient = null;
		}
	}

	@Override
	public String readString(int position) {
		return null;
	}

	private static File createDirectory(String pathName) {
		File file = new File(pathName);
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalArgumentException("could not create directory: " + file);
		} else if (!file.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + file);
		} else if (!file.canRead()) {
			throw new IllegalArgumentException("cannot read directory: " + file);
		} else if (!file.canWrite()) {
			throw new IllegalArgumentException("cannot write directory: " + file);
		}
		return file;
	}

	// // // hand sewed tile protocol buffers decoder // // //
	private static final int BUFFER_SIZE = 65536;

	private final byte[] buffer = new byte[BUFFER_SIZE];
	// position in read buffer
	private int bufferPos;
	// bytes available in read buffer
	private int bufferSize;
	// (bytesRead - bufferPos) + bufferSize
	// private int bufferLimit;
	// bytes processed
	private int bytesRead;
	private InputStream inputStream;

	private static final int TAG_TILE_TAGS = 1;
	private static final int TAG_TILE_WAYS = 2;
	private static final int TAG_TILE_POLY = 3;
	private static final int TAG_TILE_NODES = 4;
	private static final int TAG_WAY_TAGS = 11;
	private static final int TAG_WAY_INDEX = 12;
	private static final int TAG_WAY_COORDS = 13;
	private static final int TAG_WAY_LAYER = 21;
	private static final int TAG_WAY_NUM_TAGS = 1;
	private static final int TAG_WAY_NUM_INDICES = 2;
	private static final int TAG_WAY_NUM_COORDS = 3;

	private static final int TAG_NODE_TAGS = 11;
	private static final int TAG_NODE_COORDS = 12;
	private static final int TAG_NODE_LAYER = 21;
	private static final int TAG_NODE_NUM_TAGS = 1;
	private static final int TAG_NODE_NUM_COORDS = 2;

	private boolean decode(InputStream is) throws IOException {
		inputStream = is;
		bytesRead = 0;
		bufferSize = 0;
		bufferPos = 0;
		int val;

		while ((val = decodeVarint32()) > 0) {
			// read tag and wire type
			int tag = (val >> 3);

			switch (tag) {
				case TAG_TILE_TAGS:
					decodeTileTags();
					break;

				case TAG_TILE_WAYS:
					decodeTileWays(false);
					break;

				case TAG_TILE_POLY:
					decodeTileWays(true);
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
		// Log.d(TAG, "tag>" + tagString + "<");

		if (tagString == null || tagString.length() == 0) {

			curTags[mCurTagCnt++] = new Tag(Tag.TAG_KEY_NAME, "...");
			return false;
		}
		Tag tag = tagHash.get(tagString);

		if (tag == null) {
			if (tagString.startsWith(Tag.TAG_KEY_NAME))
				tag = new Tag(Tag.TAG_KEY_NAME, tagString.substring(5), false);
			else
				tag = new Tag(tagString);

			tagHash.put(tagString, tag);
		}

		if (mCurTagCnt >= MAX_TILE_TAGS) {
			MAX_TILE_TAGS = mCurTagCnt + 10;
			Tag[] tmp = new Tag[MAX_TILE_TAGS];
			System.arraycopy(curTags, 0, tmp, 0, mCurTagCnt);
			curTags = tmp;
		}
		curTags[mCurTagCnt++] = tag;

		return true;
	}

	private boolean decodeTileWays(boolean polygon) throws IOException {
		int bytes = decodeVarint32();

		int end = bytesRead + bytes;
		int indexCnt = 0;
		int tagCnt = 0;
		int coordCnt = 0;
		int layer = 5;
		Tag[] tags = null;
		short[] index = null;

		boolean skip = false;
		boolean fail = false;

		while (bytesRead < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_WAY_TAGS:
					tags = decodeWayTags(tagCnt);
					break;

				case TAG_WAY_INDEX:
					index = decodeWayIndices(indexCnt);
					break;

				case TAG_WAY_COORDS:
					int cnt = decodeWayCoordinates(skip);
					if (cnt != coordCnt) {
						Log.d(TAG, "X wrong number of coordintes");
						fail = true;
					}
					break;

				case TAG_WAY_LAYER:
					layer = decodeVarint32();
					break;

				case TAG_WAY_NUM_TAGS:
					tagCnt = decodeVarint32();
					break;

				case TAG_WAY_NUM_INDICES:
					indexCnt = decodeVarint32();
					break;

				case TAG_WAY_NUM_COORDS:
					coordCnt = decodeVarint32();
					break;

				default:
					Log.d(TAG, "X invalid type for way: " + tag);
			}
		}

		if (fail || index == null || tags == null || indexCnt == 0 || tagCnt == 0) {
			Log.d(TAG, "failed reading way: bytes:" + bytes + " index:" + index + " tag:"
					+ (tags != null ? tags[0] : "...") + " "
					+ indexCnt + " " + coordCnt + " " + tagCnt);
			return false;
		}

		float[] coords = tmpCoords;

		// FIXME, remove all tiles from cache then remove this below
		if (layer == 0)
			layer = 5;

		mMapGenerator.renderWay((byte) layer, tags, coords, index, polygon);
		return true;
	}

	private boolean decodeTileNodes() throws IOException {
		int bytes = decodeVarint32();

		int end = bytesRead + bytes;
		int tagCnt = 0;
		int coordCnt = 0;
		byte layer = 0;
		Tag[] tags = null;

		while (bytesRead < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_NODE_TAGS:
					tags = decodeWayTags(tagCnt);
					break;

				case TAG_NODE_COORDS:
					int cnt = decodeNodeCoordinates(coordCnt, layer, tags);
					if (cnt != coordCnt) {
						Log.d(TAG, "X wrong number of coordintes");
						return false;
					}
					break;

				case TAG_NODE_LAYER:
					layer = (byte) decodeVarint32();
					break;

				case TAG_NODE_NUM_TAGS:
					tagCnt = decodeVarint32();
					break;

				case TAG_NODE_NUM_COORDS:
					coordCnt = decodeVarint32();
					break;

				default:
					Log.d(TAG, "X invalid type for node: " + tag);
			}
		}

		return true;
	}

	private int decodeNodeCoordinates(int numNodes, byte layer, Tag[] tags)
			throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);
		int cnt = 0;
		int end = bufferPos + bytes;
		float scale = mScaleFactor;
		// read repeated sint32
		int lastX = 0;
		int lastY = 0;
		while (bufferPos < end && cnt < numNodes) {
			int lon = decodeZigZag32(decodeVarint32());
			int lat = decodeZigZag32(decodeVarint32());
			lastX = lon + lastX;
			lastY = lat + lastY;

			mMapGenerator.renderPointOfInterest(layer,
					lastY / scale, lastX / scale, tags);
			cnt += 2;
		}
		return cnt;
	}

	private int MAX_WAY_COORDS = 32768;
	private float[] tmpCoords = new float[MAX_WAY_COORDS];

	private Tag[] decodeWayTags(int tagCnt) throws IOException {
		int bytes = decodeVarint32();

		Tag[] tags = new Tag[tagCnt];

		int cnt = 0;
		int end = bytesRead + bytes;
		int max = mCurTagCnt;

		while (bytesRead < end) {
			int tagNum = decodeVarint32();

			if (tagNum < 0 || cnt == tagCnt) {
				Log.d(TAG, "NULL TAG: " + mTile + " invalid tag:" + tagNum + " "
						+ tagCnt + "/" + cnt);
				break;
			}
			if (tagNum < Tags.MAX)
				tags[cnt++] = Tags.tags[tagNum];
			else {
				tagNum -= Tags.LIMIT;

				if (tagNum >= 0 && tagNum < max) {
					// Log.d(TAG, "variable tag: " + curTags[tagNum]);
					tags[cnt++] = curTags[tagNum];
				} else {
					Log.d(TAG, "NULL TAG: " + mTile + " could find tag:"
							+ tagNum + " " + tagCnt + "/" + cnt);
				}
			}
		}
		if (tagCnt != cnt)
			Log.d(TAG, "NULL TAG: " + mTile + " ...");

		return tags;
	}

	private short[] mIndices = new short[10];

	private short[] decodeWayIndices(int indexCnt) throws IOException {
		int bytes = decodeVarint32();

		short[] index = mIndices;
		if (index.length < indexCnt + 1) {
			index = mIndices = new short[indexCnt + 1];
		}

		int cnt = 0;
		int end = bytesRead + bytes;

		while (bytesRead < end) {
			int val = decodeVarint32();
			if (cnt < indexCnt)
				index[cnt++] = (short) (val * 2);
			// else DEBUG...

		}

		index[indexCnt] = -1;

		return index;
	}

	private int decodeWayCoordinates(boolean skip) throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);
		if (skip) {
			bufferPos += bytes;
			return 0;
		}

		int pos = bufferPos;
		int end = pos + bytes;
		float[] coords = tmpCoords;
		byte[] buf = buffer;
		int cnt = 0;
		int result;

		int x, lastX = 0;
		int y, lastY = 0;
		boolean even = true;

		float scale = mScaleFactor;

		// read repeated sint32
		while (pos < end) {
			if (cnt >= MAX_WAY_COORDS) {
				Log.d(TAG, "increase way coord buffer " + mTile);

				MAX_WAY_COORDS += 128;
				float[] tmp = new float[MAX_WAY_COORDS];
				System.arraycopy(coords, 0, tmp, 0, cnt);
				tmpCoords = coords = tmp;
			}

			if (buf[pos] >= 0) {
				result = buf[pos++];
			} else if (buf[pos + 1] >= 0) {
				result = (buf[pos] & 0x7f)
						| buf[pos + 1] << 7;
				pos += 2;
			} else if (buf[pos + 2] >= 0) {
				result = (buf[pos] & 0x7f)
						| (buf[pos + 1] & 0x7f) << 7
						| (buf[pos + 2]) << 14;
				pos += 3;
			} else if (buf[pos + 3] >= 0) {
				result = (buf[pos] & 0x7f)
						| (buf[pos + 1] & 0x7f) << 7
						| (buf[pos + 2] & 0x7f) << 14
						| (buf[pos + 3]) << 21;
				pos += 4;
			} else {
				result = (buf[pos] & 0x7f)
						| (buf[pos + 1] & 0x7f) << 7
						| (buf[pos + 2] & 0x7f) << 14
						| (buf[pos + 3] & 0x7f) << 21
						| (buf[pos + 4]) << 28;

				Log.d(TAG, "Stuffs too large " + mTile);

				pos += 4;
				int i = 0;

				while (buf[pos++] < 0 && i < 10)
					i++;

				if (i == 10)
					throw new IOException("X malformed VarInt32");

			}
			if (even) {
				x = ((result >>> 1) ^ -(result & 1));
				lastX = lastX + x;
				coords[cnt++] = lastX / scale;
				even = false;
			} else {
				y = ((result >>> 1) ^ -(result & 1));
				lastY = lastY + y;
				coords[cnt++] = lastY / scale;
				even = true;
			}
		}

		bufferPos = pos;
		bytesRead += bytes;

		return cnt;
	}

	private int readBuffer(int size) throws IOException {
		int read = 0;

		if (bufferPos + size < bufferSize)
			return 0;

		if (size > BUFFER_SIZE) {
			// FIXME throw exception for now, but frankly better
			// sanitize tile data on compilation.
			// this only happen with strings or coordinates larger than 64kb
			throw new IOException("X requested size too large");
		}

		if (bufferSize == bufferPos) {
			bufferPos = 0;
			bufferSize = 0;
		} else if (bufferPos + (size - bufferSize) > BUFFER_SIZE) {
			Log.d(TAG, "wrap buffer" + (size - bufferSize) + " " + bufferPos);
			// copy bytes left to read to the beginning of buffer
			bufferSize -= bufferPos;
			System.arraycopy(buffer, bufferPos, buffer, 0, bufferSize);
			bufferPos = 0;
		}

		while ((bufferSize - bufferPos) < size) {

			// read until requested size is available in buffer
			int len = inputStream.read(buffer, bufferSize, BUFFER_SIZE - bufferSize);

			if (len < 0) {
				// finished reading, mark end
				buffer[bufferSize] = 0;
				break;
			}

			read += len;

			if (mCacheFile != null)
				mCacheFile.write(buffer, bufferSize, len);

			bufferSize += len;
		}
		return read;
	}

	@Override
	public void cancel() {
		if (mRequest != null) {
			mRequest.abort();
			mRequest = null;
		}
	}

	private int decodeVarint32() throws IOException {
		int pos = bufferPos;

		if (pos + 10 > bufferSize) {
			readBuffer(8192);
			pos = bufferPos;
		}

		byte[] buf = buffer;

		if (buf[pos] >= 0) {
			bufferPos += 1;
			bytesRead += 1;
			return buf[pos];
		} else if (buf[pos + 1] >= 0) {
			bufferPos += 2;
			bytesRead += 2;
			return (buf[pos] & 0x7f)
					| (buf[pos + 1]) << 7;

		} else if (buf[pos + 2] >= 0) {
			bufferPos += 3;
			bytesRead += 3;
			return (buf[pos] & 0x7f)
					| (buf[pos + 1] & 0x7f) << 7
					| (buf[pos + 2]) << 14;
		} else if (buf[pos + 3] >= 0) {
			bufferPos += 4;
			bytesRead += 4;
			return (buf[pos] & 0x7f)
					| (buf[pos + 1] & 0x7f) << 7
					| (buf[pos + 2] & 0x7f) << 14
					| (buf[pos + 3]) << 21;
		}

		int result = (buf[pos] & 0x7f)
				| (buf[pos + 1] & 0x7f) << 7
				| (buf[pos + 2] & 0x7f) << 14
				| (buf[pos + 3] & 0x7f) << 21
				| (buf[pos + 4]) << 28;

		Log.d(TAG, "got a big number, eh?");

		int read = 5;
		pos += 4;

		// 'Discard upper 32 bits' - the original comment.
		// havent found this in any document but the code provided by google.
		// no idea what this is for, just seems fsckin stupid...
		while (buf[pos++] < 0 && read < 10)
			read++;

		if (read == 10)
			throw new IOException("X malformed VarInt32");

		bufferPos += read;
		bytesRead += read;

		return result;
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

	private String decodeString() throws IOException {
		final int size = decodeVarint32();

		readBuffer(size);

		final String result = new String(buffer, bufferPos, size, "UTF-8");
		// Log.d(TAG, "read string " + read + " " + size + " " + bufferPos + " " + result);

		bufferPos += size;
		bytesRead += size;
		return result;

	}

	private static int decodeZigZag32(final int n) {
		return (n >>> 1) ^ -(n & 1);
	}

}
