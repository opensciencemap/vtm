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
package org.oscim.database.pbmap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.database.OpenResult;
import org.oscim.database.QueryResult;
import org.oscim.generator.JobTile;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

/**
 *
 *
 */
public class MapDatabase implements IMapDatabase {
	private static final String TAG = "MapDatabase";

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author", null);

	private boolean mOpenFile = false;

	private static final boolean USE_CACHE = false;

	//	private static final boolean USE_APACHE_HTTP = false;
	//	private static final boolean USE_LW_HTTP = true;

	private static final String CACHE_DIRECTORY = "/Android/data/org.oscim.app/cache/";
	private static final String CACHE_FILE = "%d-%d-%d.tile";

	//	private static final String SERVER_ADDR = "city.informatik.uni-bremen.de";
	// private static final String URL =
	// "http://city.informatik.uni-bremen.de:8020/test/%d/%d/%d.osmtile";
	//private static final String URL = "http://city.informatik.uni-bremen.de/osmstache/test/%d/%d/%d.osmtile";
	//private static final String URL = "http://city.informatik.uni-bremen.de/osmstache/gis-live/%d/%d/%d.osmtile";

	// private static final String URL =
	// "http://city.informatik.uni-bremen.de/tiles/tiles.py///test/%d/%d/%d.osmtile";
	// private static final String URL =
	// "http://city.informatik.uni-bremen.de/osmstache/gis2/%d/%d/%d.osmtile";

	private final static float REF_TILE_SIZE = 4096.0f;

	private int MAX_TILE_TAGS = 100;
	private Tag[] curTags = new Tag[MAX_TILE_TAGS];
	private int mCurTagCnt;

	//	private HttpClient mClient;
	//	private HttpGet mRequest = null;

	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;
	private JobTile mTile;
	private FileOutputStream mCacheFile;

	private String mHost;
	private int mPort;
	private long mContentLenth;
	private InputStream mInputStream;

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

	@Override
	public QueryResult executeQuery(JobTile tile, IMapDatabaseCallback mapDatabaseCallback) {
		QueryResult result = QueryResult.SUCCESS;
		mCacheFile = null;

		mTile = tile;

		mMapGenerator = mapDatabaseCallback;
		mCurTagCnt = 0;

		// scale coordinates to tile size
		mScaleFactor = REF_TILE_SIZE / Tile.TILE_SIZE;

		File f = null;

		mBufferSize = 0;
		mBufferPos = 0;
		mReadPos = 0;

		if (USE_CACHE) {
			f = new File(cacheDir, String.format(CACHE_FILE,
					Integer.valueOf(tile.zoomLevel),
					Integer.valueOf(tile.tileX),
					Integer.valueOf(tile.tileY)));

			if (cacheRead(tile, f))
				return QueryResult.SUCCESS;
		}

		//		String url = null;
		//		HttpGet getRequest;

		//		if (!USE_LW_HTTP) {
		//			url = String.format(URL,
		//					Integer.valueOf(tile.zoomLevel),
		//					Integer.valueOf(tile.tileX),
		//					Integer.valueOf(tile.tileY));
		//		}

		//		if (USE_APACHE_HTTP) {
		//			getRequest = new HttpGet(url);
		//			mRequest = getRequest;
		//		}

		try {
			//			if (USE_LW_HTTP) {
			if (lwHttpSendRequest(tile) && lwHttpReadHeader() > 0) {
				cacheBegin(tile, f);
				decode();
			} else {
				result = QueryResult.FAILED;
			}

			//			}
			//			else if (USE_APACHE_HTTP) {
			//				HttpResponse response = mClient.execute(getRequest);
			//				final int statusCode = response.getStatusLine().getStatusCode();
			//				final HttpEntity entity = response.getEntity();
			//
			//				if (statusCode != HttpStatus.SC_OK) {
			//					Log.d(TAG, "Http response " + statusCode);
			//					entity.consumeContent();
			//					return QueryResult.FAILED;
			//				}
			//				if (!mTile.isLoading) {
			//					Log.d(TAG, "1 loading canceled " + mTile);
			//					entity.consumeContent();
			//
			//					return QueryResult.FAILED;
			//				}
			//
			//				InputStream is = null;
			//				// GZIPInputStream zis = null;
			//				try {
			//					is = entity.getContent();
			//
			//					mContentLenth = entity.getContentLength();
			//					mInputStream = is;
			//					cacheBegin(tile, f);
			//					// zis = new GZIPInputStream(is);
			//					decode();
			//				} finally {
			//					// if (zis != null)
			//					// zis.close();
			//					if (is != null)
			//						is.close();
			//					entity.consumeContent();
			//				}
			//			} else {
			//				HttpURLConnection urlConn =
			//						(HttpURLConnection) new URL(url).openConnection();
			//
			//				InputStream in = urlConn.getInputStream();
			//				try {
			//					decode();
			//				} finally {
			//					urlConn.disconnect();
			//				}
			//			}
		} catch (SocketException ex) {
			Log.d(TAG, "Socket exception: " + ex.getMessage());
			result = QueryResult.FAILED;
		} catch (SocketTimeoutException ex) {
			Log.d(TAG, "Socket Timeout exception: " + ex.getMessage());
			result = QueryResult.FAILED;
		} catch (UnknownHostException ex) {
			Log.d(TAG, "no network");
			result = QueryResult.FAILED;
		} catch (Exception ex) {
			ex.printStackTrace();
			result = QueryResult.FAILED;
		}

		mLastRequest = SystemClock.elapsedRealtime();

		//		if (USE_APACHE_HTTP)
		//			mRequest = null;

		cacheFinish(tile, f, result == QueryResult.SUCCESS);

		return result;
	}

	private static File cacheDir;

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
		return mOpenFile;
	}

	//	private void createClient() {
	//		mOpenFile = true;
	//		HttpParams params = new BasicHttpParams();
	//		HttpConnectionParams.setStaleCheckingEnabled(params, false);
	//		HttpConnectionParams.setTcpNoDelay(params, true);
	//		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
	//		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
	//		HttpConnectionParams.setSocketBufferSize(params, 32768);
	//		HttpClientParams.setRedirecting(params, false);
	//
	//		DefaultHttpClient client = new DefaultHttpClient(params);
	//		client.removeRequestInterceptorByClass(RequestAddCookies.class);
	//		client.removeResponseInterceptorByClass(ResponseProcessCookies.class);
	//		client.removeRequestInterceptorByClass(RequestUserAgent.class);
	//		client.removeRequestInterceptorByClass(RequestExpectContinue.class);
	//		client.removeRequestInterceptorByClass(RequestTargetAuthentication.class);
	//		client.removeRequestInterceptorByClass(RequestProxyAuthentication.class);
	//
	//		mClient = client;
	//
	//		SchemeRegistry schemeRegistry = new SchemeRegistry();
	//		schemeRegistry.register(new Scheme("http",
	//				PlainSocketFactory.getSocketFactory(), 80));
	//	}

	@Override
	public OpenResult open(MapOptions options) {

		//if (USE_APACHE_HTTP)
		//	createClient();

		if (mOpenFile)
			return OpenResult.SUCCESS;

		if (options == null || !options.containsKey("url"))
			return new OpenResult("options missing");

		URL url;
		try {
			url = new URL(options.get("url"));
		} catch (MalformedURLException e) {

			e.printStackTrace();
			return new OpenResult("invalid url: " + options.get("url"));
		}

		int port = url.getPort();
		if (port < 0)
			port = 80;

		String host = url.getHost();
		String path = url.getPath();
		Log.d(TAG, "open oscim database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();
		REQUEST_GET_END = (".osmtile HTTP/1.1\n" +
				"Host: " + host + "\n" +
				"Connection: Keep-Alive\n\n").getBytes();

		mHost = host;
		mPort = port;

		//mSockAddr = new InetSocketAddress(host, port);

		mRequestBuffer = new byte[1024];
		System.arraycopy(REQUEST_GET_START, 0,
				mRequestBuffer, 0, REQUEST_GET_START.length);

		if (USE_CACHE) {
			if (cacheDir == null) {
				String externalStorageDirectory = Environment
						.getExternalStorageDirectory()
						.getAbsolutePath();
				String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
				cacheDir = createDirectory(cacheDirectoryPath);
			}
		}

		mOpenFile = true;

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpenFile = false;
		//		if (USE_APACHE_HTTP) {
		//			if (mClient != null) {
		//				mClient.getConnectionManager().shutdown();
		//				mClient = null;
		//			}
		//		}

		//		if (USE_LW_HTTP) {

		mSockAddr = null;

		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mSocket = null;
		}
		//		}
		if (USE_CACHE) {
			cacheDir = null;
		}
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

	// /////////////// hand sewed tile protocol buffers decoder ////////////////
	private static final int BUFFER_SIZE = 65536;

	private final byte[] mReadBuffer = new byte[BUFFER_SIZE];

	// position in read buffer
	private int mBufferPos;
	// bytes available in read buffer
	private int mBufferSize;
	// overall bytes of content processed
	private int mBytesProcessed;

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

	private boolean decode() throws IOException {
		mBytesProcessed = 0;
		int val;

		while (mBytesProcessed < mContentLenth && (val = decodeVarint32()) > 0) {
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

		int end = mBytesProcessed + bytes;
		int indexCnt = 0;
		int tagCnt = 0;
		int coordCnt = 0;
		int layer = 5;
		Tag[] tags = null;
		short[] index = null;

		boolean skip = false;
		boolean fail = false;

		while (mBytesProcessed < end) {
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
					if (coordCnt == 0)
						skip = true;

					int cnt = decodeWayCoordinates(skip, coordCnt);

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
			Log.d(TAG, "failed reading way: bytes:" + bytes + " index:"
					+ (index == null ? "null" : index.toString()) + " tag:"
					+ (tags != null ? tags.toString() : "...") + " "
					+ indexCnt + " " + coordCnt + " " + tagCnt);
			return false;
		}

		float[] coords = tmpCoords;

		// FIXME, remove all tiles from cache then remove this below
		if (layer == 0)
			layer = 5;

		mMapGenerator.renderWay((byte) layer, tags, coords, index, polygon, 0);
		return true;
	}

	private boolean decodeTileNodes() throws IOException {
		int bytes = decodeVarint32();

		int end = mBytesProcessed + bytes;
		int tagCnt = 0;
		int coordCnt = 0;
		byte layer = 0;
		Tag[] tags = null;

		while (mBytesProcessed < end) {
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
		int end = mBytesProcessed + bytes;
		float scale = mScaleFactor;
		// read repeated sint32
		int lastX = 0;
		int lastY = 0;
		while (mBytesProcessed < end && cnt < numNodes) {
			int lon = decodeZigZag32(decodeVarint32());
			int lat = decodeZigZag32(decodeVarint32());
			lastX = lon + lastX;
			lastY = lat + lastY;

			mMapGenerator.renderPointOfInterest(layer,
					tags, Tile.TILE_SIZE - lastY / scale, lastX / scale);
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
		int end = mBytesProcessed + bytes;
		int max = mCurTagCnt;

		while (mBytesProcessed < end) {
			int tagNum = decodeVarint32();

			if (tagNum < 0 || cnt == tagCnt) {
				Log.d(TAG, "NULL TAG: " + mTile + " invalid tag:" + tagNum + " "
						+ tagCnt + "/" + cnt);
			} else {
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

		readBuffer(bytes);

		int cnt = 0;
		// int end = bytesRead + bytes;

		int pos = mBufferPos;
		int end = pos + bytes;
		byte[] buf = mReadBuffer;
		int result;

		while (pos < end) {
			// int val = decodeVarint32();

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

				pos += 4;
				int i = 0;

				while (buf[pos++] < 0 && i < 10)
					i++;

				if (i == 10)
					throw new IOException("X malformed VarInt32");

			}

			index[cnt++] = (short) (result * 2);

			// if (cnt < indexCnt)
			// index[cnt++] = (short) (val * 2);
			// else DEBUG...

		}

		mBufferPos = pos;
		mBytesProcessed += bytes;

		index[indexCnt] = -1;

		return index;
	}

	private int decodeWayCoordinates(boolean skip, int nodes) throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);

		if (skip) {
			mBufferPos += bytes;
			return nodes;
		}

		int pos = mBufferPos;
		int end = pos + bytes;
		float[] coords = tmpCoords;
		byte[] buf = mReadBuffer;
		int cnt = 0;
		int result;

		int x, lastX = 0;
		int y, lastY = 0;
		boolean even = true;

		float scale = mScaleFactor;

		if (nodes * 2 > coords.length) {
			Log.d(TAG, "increase way coord buffer " + mTile + " to " + (nodes * 2));
			float[] tmp = new float[nodes * 2];
			tmpCoords = coords = tmp;
		}

		// read repeated sint32
		while (pos < end) {

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
				coords[cnt++] = Tile.TILE_SIZE - lastY / scale;
				even = true;
			}
		}

		mBufferPos = pos;
		mBytesProcessed += bytes;

		return cnt;
	}

	int mReadPos;

	private int readBuffer(int size) throws IOException {
		int read = 0;

		if (mBufferPos + size < mBufferSize)
			return mBufferSize - mBufferPos;

		if (mReadPos == mContentLenth)
			return mBufferSize - mBufferPos;

		if (size > BUFFER_SIZE) {
			// FIXME throw exception for now, but frankly better
			// sanitize tile data on compilation. this should only
			// happen with strings or one ways coordinates are
			// larger than 64kb
			throw new IOException("X requested size too large " + mTile);
		}

		if (mBufferSize == mBufferPos) {
			mBufferPos = 0;
			mBufferSize = 0;
		} else if (mBufferPos + size > BUFFER_SIZE) {
			//Log.d(TAG, "wrap buffer" + (size - mBufferSize) + " " + mBufferPos);
			// copy bytes left to read to the beginning of buffer
			mBufferSize -= mBufferPos;
			System.arraycopy(mReadBuffer, mBufferPos, mReadBuffer, 0, mBufferSize);
			mBufferPos = 0;
		}

		int max = BUFFER_SIZE - mBufferSize;

		while ((mBufferSize - mBufferPos) < size && max > 0) {

			max = BUFFER_SIZE - mBufferSize;
			if (max > mContentLenth - mReadPos)
				max = (int) (mContentLenth - mReadPos);

			// read until requested size is available in buffer
			int len = mInputStream.read(mReadBuffer, mBufferSize, max);

			if (len < 0) {
				// finished reading, mark end
				mReadBuffer[mBufferSize] = 0;
				break;
			}

			read += len;
			mReadPos += len;

			if (mCacheFile != null)
				mCacheFile.write(mReadBuffer, mBufferSize, len);

			//			if (USE_LW_HTTP) {
			if (mReadPos == mContentLenth)
				break;
			//			}

			mBufferSize += len;
		}

		return read;
	}

	@Override
	public void cancel() {
		//		if (mRequest != null) {
		//			mRequest.abort();
		//			mRequest = null;
		//		}
	}

	private int decodeVarint32() throws IOException {
		int pos = mBufferPos;

		if (pos + 10 > mBufferSize) {
			readBuffer(4096);
			pos = mBufferPos;
		}

		byte[] buf = mReadBuffer;

		if (buf[pos] >= 0) {
			mBufferPos += 1;
			mBytesProcessed += 1;
			return buf[pos];
		} else if (buf[pos + 1] >= 0) {
			mBufferPos += 2;
			mBytesProcessed += 2;
			return (buf[pos] & 0x7f)
					| (buf[pos + 1]) << 7;

		} else if (buf[pos + 2] >= 0) {
			mBufferPos += 3;
			mBytesProcessed += 3;
			return (buf[pos] & 0x7f)
					| (buf[pos + 1] & 0x7f) << 7
					| (buf[pos + 2]) << 14;
		} else if (buf[pos + 3] >= 0) {
			mBufferPos += 4;
			mBytesProcessed += 4;
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

		int read = 5;
		pos += 4;

		// 'Discard upper 32 bits' - the original comment.
		// havent found this in any document but the code provided by google.
		while (buf[pos++] < 0 && read < 10)
			read++;

		if (read == 10)
			throw new IOException("X malformed VarInt32");

		mBufferPos += read;
		mBytesProcessed += read;

		return result;
	}

	// ///////////////////////// Lightweight HttpClient //////////////////////
	// would have written simple tcp server/client for this...

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private InputStream mResponseStream;
	private long mLastRequest = 0;
	private SocketAddress mSockAddr;

	private final static byte[] RESPONSE_HTTP_OK = "HTTP/1.1 200 OK".getBytes();
	private final static byte[] RESPONSE_CONTENT_LEN = "Content-Length: ".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static int RESPONSE_EXPECTED_TIMEOUT = 10000;

	private byte[] REQUEST_GET_START;// = "GET /osmstache/test/".getBytes();
	private byte[] REQUEST_GET_END;
	// = (".osmtile HTTP/1.1\n" +
	//"Host: " + SERVER_ADDR + "\n" +
	//"Connection: Keep-Alive\n\n").getBytes();

	private byte[] mRequestBuffer;

	int lwHttpReadHeader() throws IOException {
		InputStream is = mResponseStream;

		byte[] buf = mReadBuffer;

		int read = 0;
		int pos = 0;
		int end = 0;
		// int max_req = 0;
		int resp_len = 0;
		boolean first = true;

		for (int len = 0; pos < read
				|| (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0; len = 0) {
			read += len;

			while (end < read && (buf[end] != '\n'))
				end++;

			if (buf[end] == '\n') {

				if (first) {
					// check for OK
					for (int i = 0; i < 15 && pos + i < end; i++)
						if (buf[pos + i] != RESPONSE_HTTP_OK[i])
							return -1;
					first = false;
				} else if (end - pos == 1) {
					// check empty line (header end)
					end += 1;
					break;
				}
				else {
					// parse Content-Length, TODO just encode this with message
					for (int i = 0; pos + i < end - 1; i++) {
						if (i < 16) {
							if (buf[pos + i] == RESPONSE_CONTENT_LEN[i])
								continue;

							break;
						}

						// read int value
						resp_len = resp_len * 10 + (buf[pos + i]) - '0';
					}
				}

				// String line = new String(buf, pos, end - pos - 1);
				// Log.d(TAG, ">" + line + "< " + resp_len);

				pos += (end - pos) + 1;
				end = pos;
			}
		}

		mContentLenth = resp_len;

		// start of content
		mBufferPos = end;

		// bytes of content already read into buffer
		mReadPos = read - end;

		// buffer fill
		mBufferSize = read;

		mInputStream = mResponseStream;

		return resp_len;
	}

	private boolean lwHttpSendRequest(Tile tile) throws IOException {
		if (mSockAddr == null) {
			mSockAddr = new InetSocketAddress(mHost, mPort);
		}

		if (mSocket != null && ((mMaxReq-- <= 0)
				|| (SystemClock.elapsedRealtime() - mLastRequest
				> RESPONSE_EXPECTED_TIMEOUT))) {

			try {
				mSocket.close();
			} catch (IOException e) {

			}

			// Log.d(TAG, "not alive  - recreate connection " + mMaxReq);
			mSocket = null;
		}

		if (mSocket == null) {
			lwHttpConnect();
			// we know our server
			mMaxReq = RESPONSE_EXPECTED_LIVES;
			// Log.d(TAG, "create connection");
		} else {
			// should not be needed
			int avail = mResponseStream.available();
			if (avail > 0) {
				Log.d(TAG, "Consume left-over bytes: " + avail);
				mResponseStream.read(mReadBuffer, 0, avail);
			}
		}

		byte[] request = mRequestBuffer;
		int pos = REQUEST_GET_START.length;

		pos = writeInt(tile.zoomLevel, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileX, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileY, pos, request);

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

		// this does the same but with a few more allocations:
		// byte[] request = String.format(REQUEST,
		// Integer.valueOf(tile.zoomLevel),
		// Integer.valueOf(tile.tileX), Integer.valueOf(tile.tileY)).getBytes();

		try {
			mCommandStream.write(request, 0, len);
			mCommandStream.flush();
			return true;
		} catch (IOException e) {
			Log.d(TAG, "retry - recreate connection");
		}

		lwHttpConnect();

		mCommandStream.write(request, 0, len);
		mCommandStream.flush();

		return true;
	}

	private boolean lwHttpConnect() throws IOException {
		//		if (mRequestBuffer == null) {
		//			mRequestBuffer = new byte[1024];
		//			System.arraycopy(REQUEST_GET_START,
		//					0, mRequestBuffer, 0,
		//					REQUEST_GET_START.length);
		//		}

		mSocket = new Socket();
		mSocket.connect(mSockAddr, 30000);
		mSocket.setTcpNoDelay(true);
		// mCmdBuffer = new PrintStream(mSocket.getOutputStream());
		mCommandStream = new BufferedOutputStream(mSocket.getOutputStream());
		mResponseStream = mSocket.getInputStream();
		return true;
	}

	// write (positive) integer as char sequence to buffer
	private static int writeInt(int val, int pos, byte[] buf) {
		if (val == 0) {
			buf[pos] = '0';
			return pos + 1;
		}

		int i = 0;
		for (int n = val; n > 0; n = n / 10, i++)
			buf[pos + i] = (byte) ('0' + n % 10);

		// reverse bytes
		for (int j = pos, end = pos + i - 1, mid = pos + i / 2; j < mid; j++, end--) {
			byte tmp = buf[j];
			buf[j] = buf[end];
			buf[end] = tmp;
		}

		return pos + i;

	}

	// //////////////////////////// Tile cache ///////////////////////////////

	private boolean cacheRead(Tile tile, File f) {
		if (f.exists() && f.length() > 0) {
			FileInputStream in;

			try {
				in = new FileInputStream(f);

				mContentLenth = f.length();
				Log.d(TAG, tile + " using cache: " + mContentLenth);
				mInputStream = in;

				decode();
				in.close();

				return true;

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			f.delete();
			return false;
		}

		return false;
	}

	private boolean cacheBegin(Tile tile, File f) {
		if (USE_CACHE) {
			try {
				Log.d(TAG, "writing cache: " + tile);
				mCacheFile = new FileOutputStream(f);

				if (mReadPos > 0) {
					try {
						mCacheFile.write(mReadBuffer, mBufferPos,
								mBufferSize - mBufferPos);

					} catch (IOException e) {
						e.printStackTrace();
						mCacheFile = null;
						return false;
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mCacheFile = null;
				return false;
			}
		}
		return true;
	}

	private void cacheFinish(Tile tile, File file, boolean success) {
		if (USE_CACHE) {
			if (success) {
				try {
					mCacheFile.flush();
					mCacheFile.close();
					Log.d(TAG, tile + " cache written " + file.length());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				file.delete();
			}
		}
		mCacheFile = null;
	}

	/*
	 * All code below is taken from or based on Google's Protocol Buffers
	 * implementation:
	 */

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

		final String result = new String(mReadBuffer, mBufferPos, size, "UTF-8");

		mBufferPos += size;
		mBytesProcessed += size;
		return result;

	}

	private static int decodeZigZag32(final int n) {
		return (n >>> 1) ^ -(n & 1);
	}

}
