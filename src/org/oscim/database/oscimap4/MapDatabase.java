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

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.layers.tile.MapTile;
import org.oscim.utils.UTF8Decoder;

import android.os.Environment;
import android.os.SystemClock;
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

	private static final String CACHE_DIRECTORY = "/Android/data/org.oscim.app/cache/";
	private static final String CACHE_FILE = "%d-%d-%d.tile";

	private final static float REF_TILE_SIZE = 4096.0f;

	// 'open' state
	private boolean mOpen = false;
	private static File cacheDir;


	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;
	private MapTile mTile;

	//private final boolean debug = false;
	private LwHttp conn;

	private final UTF8Decoder mStringDecoder;
	private final MapElement mElem;

	public MapDatabase() {
		mStringDecoder = new UTF8Decoder();
		mElem = new MapElement();
	}

	@Override
	public QueryResult executeQuery(MapTile tile, IMapDatabaseCallback mapDatabaseCallback) {
		QueryResult result = QueryResult.SUCCESS;

		mTile = tile;

		mMapGenerator = mapDatabaseCallback;

		// scale coordinates to tile size
		mScaleFactor = REF_TILE_SIZE / Tile.SIZE;

		File f = null;

		if (USE_CACHE) {
			f = new File(cacheDir, String.format(CACHE_FILE,
					Integer.valueOf(tile.zoomLevel),
					Integer.valueOf(tile.tileX),
					Integer.valueOf(tile.tileY)));

			if (conn.cacheRead(tile, f))
				return QueryResult.SUCCESS;
		}

		try {

			if (conn.sendRequest(tile) && conn.readHeader() >= 0) {
				conn.cacheBegin(tile, f);
				decode();
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

		conn.mLastRequest = SystemClock.elapsedRealtime();

		if (result == QueryResult.SUCCESS) {

			conn.cacheFinish(tile, f, true);
		} else {
			conn.cacheFinish(tile, f, false);
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
		if (mOpen)
			return OpenResult.SUCCESS;

		if (options == null || !options.containsKey("url"))
			return new OpenResult("options missing");

		conn = new LwHttp();

		if (!conn.setServer(options.get("url"))) {
			return new OpenResult("invalid url: " + options.get("url"));
		}

		if (USE_CACHE) {
			if (cacheDir == null) {
				String externalStorageDirectory = Environment
						.getExternalStorageDirectory()
						.getAbsolutePath();
				String cacheDirectoryPath = externalStorageDirectory + CACHE_DIRECTORY;
				cacheDir = createDirectory(cacheDirectoryPath);
			}
		}

		mOpen = true;
		initDecorder();

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpen = false;

		conn.close();

		if (USE_CACHE) {
			cacheDir = null;
		}
	}

	@Override
	public void cancel() {
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

	// /////////////// hand sewed tile protocol buffers decoder ///////////////

	private static final int TAG_TILE_VERSION = 1;
	//private static final int TAG_TILE_TIMESTAMP = 2;
	//private static final int TAG_TILE_ISWATER = 3;


	private static final int TAG_TILE_NUM_TAGS = 11;
	private static final int TAG_TILE_NUM_KEYS = 12;
	private static final int TAG_TILE_NUM_VALUES = 13;

	private static final int TAG_TILE_TAG_KEYS = 14;
	private static final int TAG_TILE_TAG_VALUES = 15;
	private static final int TAG_TILE_TAGS = 16;

	private static final int TAG_TILE_LINE = 21;
	private static final int TAG_TILE_POLY = 22;
	private static final int TAG_TILE_POINT = 23;


	private static final int TAG_ELEM_NUM_INDICES = 1;
	private static final int TAG_ELEM_NUM_TAGS = 2;
	//private static final int TAG_ELEM_HAS_ELEVATION = 3;
	private static final int TAG_ELEM_TAGS = 11;
	private static final int TAG_ELEM_INDEX = 12;
	private static final int TAG_ELEM_COORDS = 13;
	private static final int TAG_ELEM_LAYER = 21;
	//private static final int TAG_ELEM_HEIGHT = 31;
	//private static final int TAG_ELEM_MIN_HEIGHT = 32;
	//private static final int TAG_ELEM_PRIORITY = 41;

	private short[] mTmpShortArray = new short[100];
	private Tag[][] mElementTags;

	private final TagSet curTags = new TagSet(100);
	//private int mCurTagCnt;

	private void initDecorder() {
		// reusable tag set
		Tag[][] tags = new Tag[10][];
		for (int i = 0; i < 10; i++)
			tags[i] = new Tag[i + 1];
		mElementTags = tags;
	}

	private boolean decode() throws IOException {

		//mCurTagCnt = 0;
		curTags.clear(true);
		int version = -1;

		int val;
		int numTags = 0;
		int numKeys= -1;
		int numValues = -1;

		int curKey = 0;
		int curValue = 0;

		String [] keys = null;
		String [] values = null;

		while (conn.hasData() && (val = decodeVarint32()) > 0) {
			// read tag and wire type
			int tag = (val >> 3);

			switch (tag) {
				case TAG_TILE_LINE:
				case TAG_TILE_POLY:
				case TAG_TILE_POINT:
					decodeTileElement(tag);
					break;

				case TAG_TILE_TAG_KEYS:
					if (keys == null || curKey >= numKeys){
						Log.d(TAG, mTile + " wrong number of keys " + numKeys);
						return false;
					}
					keys[curKey++] = decodeString();
					break;

				case TAG_TILE_TAG_VALUES:
					if (values == null || curValue >= numValues){
						Log.d(TAG, mTile + " wrong number of values " + numValues);
						return false;
					}
					values[curValue++] = decodeString();
					break;

				case TAG_TILE_NUM_TAGS:
					numTags = decodeVarint32();
					//Log.d(TAG, "num tags " + numTags);
					break;

				case TAG_TILE_NUM_KEYS:
					numKeys = decodeVarint32();
					//Log.d(TAG, "num keys " + numKeys);
					keys = new String[numKeys];
					break;

				case TAG_TILE_NUM_VALUES:
					numValues = decodeVarint32();
					//Log.d(TAG, "num values " + numValues);
					values = new String[numValues];
					break;

				case TAG_TILE_TAGS:
					mTmpShortArray = decodeShortArray(numTags, mTmpShortArray);
					if (!decodeTileTags(numTags, mTmpShortArray, keys, values)){
						Log.d(TAG, mTile + " invalid tags");
						return false;
					}
					break;

				case TAG_TILE_VERSION:
					version = decodeVarint32();
					if (version != 4){
						Log.d(TAG, mTile + " invalid version "+ version);
						return false;
					}
					break;

				default:
					Log.d(TAG, mTile + " invalid type for tile: " + tag);
					return false;
			}
		}
		return true;
	}

	private boolean decodeTileTags(int numTags, short[] tagIdx, String[] keys, String[] vals) {
		Tag tag;

		for (int i = 0; i < numTags*2; i += 2){
			int k = tagIdx[i];
			int v = tagIdx[i+1];
			String key, val;

			if (k < Tags.ATTRIB_OFFSET){
				if (k > Tags.MAX_KEY)
					return false;
				key = Tags.keys[k];
			} else {
				k -= Tags.ATTRIB_OFFSET;
				if (k >= keys.length)
					return false;
				key = keys[k];
			}

			if (v < Tags.ATTRIB_OFFSET){
				if (v > Tags.MAX_VALUE)
					return false;
				val = Tags.values[v];
			} else {
				v -= Tags.ATTRIB_OFFSET;
				if (v >= vals.length)
					return false;
				val = vals[v];
			}

			// FIXME filter out all variable tags
			// might depend on theme though
			if (key == Tag.TAG_KEY_NAME || key == Tag.KEY_HEIGHT || key == Tag.KEY_MIN_HEIGHT)
				tag = new Tag(key, val, false);
			else
				tag = new Tag(key, val, true);

			curTags.add(tag);
		}

		return true;
	}

	private int decodeWayIndices(int indexCnt) throws IOException {
		mElem.index = decodeShortArray(indexCnt, mElem.index);

		short[] index = mElem.index;
		int coordCnt = 0;

		for (int i = 0; i < indexCnt; i++)
			coordCnt += index[i] *= 2;

		// set end marker
		if (indexCnt < index.length)
			index[indexCnt] = -1;

		return coordCnt;
	}

	private boolean decodeTileElement(int type) throws IOException {

		int bytes = decodeVarint32();
		Tag[] tags = null;
		short[] index = null;

		int end = conn.position() + bytes;
		int numIndices = 1;
		int numTags = 1;

		boolean skip = false;
		boolean fail = false;

		int coordCnt = 0;
		if (type == TAG_TILE_POINT) {
			coordCnt = 2;
			mElem.index[0] = 2;
		}

		mElem.layer = 5;
		mElem.priority = 0;
		mElem.height = 0;
		mElem.minHeight = 0;

		while (conn.position() < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_ELEM_TAGS:
					tags = decodeElementTags(numTags);
					break;

				case TAG_ELEM_NUM_INDICES:
					numIndices = decodeVarint32();
					break;

				case TAG_ELEM_NUM_TAGS:
					numTags = decodeVarint32();
					break;

				case TAG_ELEM_INDEX:
					coordCnt = decodeWayIndices(numIndices);
					break;

				case TAG_ELEM_COORDS:
					if (coordCnt == 0) {
						Log.d(TAG, mTile + " no coordinates");
						skip = true;
					}
					int cnt = decodeWayCoordinates(skip, coordCnt);

					if (cnt != coordCnt) {
						Log.d(TAG, mTile + " wrong number of coordintes");
						fail = true;
					}
					break;

				case TAG_ELEM_LAYER:
					mElem.layer = decodeVarint32();
					break;

//				case TAG_ELEM_HEIGHT:
//					mElem.height = decodeVarint32();
//					break;
//
//				case TAG_ELEM_MIN_HEIGHT:
//					mElem.minHeight = decodeVarint32();
//					break;
//
//				case TAG_ELEM_PRIORITY:
//					mElem.priority = decodeVarint32();
//					break;

				default:
					Log.d(TAG, mTile + " invalid type for way: " + tag);
			}
		}

		if (fail || tags == null || numIndices == 0) {
			Log.d(TAG, mTile + " failed reading way: bytes:" + bytes + " index:"
					+ (Arrays.toString(index)) + " tag:"
					+ (tags != null ? Arrays.deepToString(tags) : "null") + " "
					+ numIndices + " " + coordCnt);
			return false;
		}

		mElem.tags = tags;
		switch (type) {
			case TAG_TILE_LINE:
				mElem.type = GeometryType.LINE;
				break;
			case TAG_TILE_POLY:
				mElem.type = GeometryType.POLY;
				break;
			case TAG_TILE_POINT:
				mElem.type = GeometryType.POINT;
				break;
		}

		mMapGenerator.renderElement(mElem);

		return true;
	}

	private Tag[] decodeElementTags(int numTags) throws IOException {
		short[] tagIds = mTmpShortArray = decodeShortArray(numTags, mTmpShortArray);

		Tag[] tags;

		if (numTags < 11)
			tags = mElementTags[numTags - 1];
		else
			tags = new Tag[numTags];

		int max = curTags.numTags;

		for (int i = 0; i < numTags; i++){
			int idx = tagIds[i];

			if (idx < 0 || idx > max) {
				Log.d(TAG, mTile + " invalid tag:" + idx + " " + i);
				return null;
			}

			tags[i] = curTags.tags[idx];
		}

		return tags;
	}

	private final static int VARINT_LIMIT = 5;
	private final static int VARINT_MAX = 10;

	private int decodeWayCoordinates(boolean skip, int nodes) throws IOException {
		int bytes = decodeVarint32();

		conn.readBuffer(bytes);

		if (skip) {
			conn.bufferPos += bytes;
			return nodes;
		}

		int cnt = 0;

		int lastX = 0;
		int lastY = 0;
		boolean even = true;

		float scale = mScaleFactor;
		float[] coords = mElem.ensurePointSize(nodes, false);

		byte[] buf = conn.buffer;
		int pos = conn.bufferPos;
		int end = pos + bytes;
		int val;

		while (pos < end) {
			if (buf[pos] >= 0) {
				val = buf[pos++];

			} else if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| buf[pos++] << 7;

			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;

			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;

			} else {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos]) << 28;

				int max = pos + VARINT_LIMIT;
				while (pos < max)
					if (buf[pos++] >= 0)
						break;

				if (pos == max)
					throw new IOException("malformed VarInt32 in " + mTile);
			}

			// zigzag decoding
			int s = ((val >>> 1) ^ -(val & 1));

			if (even) {
				lastX = lastX + s;
				coords[cnt++] = lastX / scale;
				even = false;
			} else {
				lastY = lastY + s;
				coords[cnt++] = lastY / scale;
				even = true;
			}
		}

		conn.bufferPos = pos;

		return cnt;
	}

	private short[] decodeShortArray(int num, short[] array) throws IOException {
		int bytes = decodeVarint32();

		if (array.length < num)
			array = new short[num];

		conn.readBuffer(bytes);

		int cnt = 0;

		byte[] buf = conn.buffer;
		int pos = conn.bufferPos;
		int end = pos + bytes;
		int val;

		while (pos < end) {
			if (buf[pos] >= 0) {
				val = buf[pos++];
			} else if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| buf[pos++] << 7;
			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;
			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;
			} else  if (buf[pos + 4] >= 0){
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos++]) << 28;
			} else
				throw new IOException("malformed VarInt32 in " + mTile);

			array[cnt++] = (short) val;
		}

		conn.bufferPos = pos;

		return array;
	}

	private int decodeVarint32() throws IOException {
		if (conn.bufferPos + VARINT_MAX > conn.bufferFill)
			conn.readBuffer(4096);

		byte[] buf = conn.buffer;
		int pos = conn.bufferPos;
		int val;

		if (buf[pos] >= 0) {
			val = buf[pos++];
		} else {

			if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++]) << 7;

			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;

			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;
			} else {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos]) << 28;

				// 'Discard upper 32 bits'
				int max = pos + VARINT_LIMIT;
				while (pos < max)
					if (buf[pos++] >= 0)
						break;

				if (pos == max)
					throw new IOException("malformed VarInt32 in " + mTile);
			}
		}

		conn.bufferPos = pos;

		return val;
	}

	private String decodeString() throws IOException {
		final int size = decodeVarint32();
		conn.readBuffer(size);
		final String result = mStringDecoder.decode(conn.buffer, conn.bufferPos, size);

		conn.bufferPos += size;

		return result;

	}
}
