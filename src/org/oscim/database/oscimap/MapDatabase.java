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
package org.oscim.database.oscimap;

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

	private final int MAX_TILE_TAGS = 100;
	private Tag[] curTags = new Tag[MAX_TILE_TAGS];
	private int mCurTagCnt;

	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;
	private MapTile mTile;

	private final boolean debug = false;
	private LwHttp lwHttp;

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

			if (lwHttp.cacheRead(tile, f))
				return QueryResult.SUCCESS;
		}

		try {

			if (lwHttp.sendRequest(tile) && lwHttp.readHeader() >= 0) {
				lwHttp.cacheBegin(tile, f);
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

		lwHttp.mLastRequest = SystemClock.elapsedRealtime();

		if (result == QueryResult.SUCCESS) {

			lwHttp.cacheFinish(tile, f, true);
		} else {
			lwHttp.cacheFinish(tile, f, false);
			lwHttp.close();
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

		lwHttp = new LwHttp();

		if (!lwHttp.setServer(options.get("url"))) {
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

		lwHttp.close();

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

	private static final int TAG_TILE_NUM_TAGS = 1;
	private static final int TAG_TILE_TAG_KEYS = 2;
	private static final int TAG_TILE_TAG_VALUES = 3;

	private static final int TAG_TILE_LINE = 11;
	private static final int TAG_TILE_POLY = 12;
	private static final int TAG_TILE_POINT = 13;
	// private static final int TAG_TILE_LABEL = 21;
	// private static final int TAG_TILE_WATER = 31;

	private static final int TAG_ELEM_NUM_INDICES = 1;
	private static final int TAG_ELEM_TAGS = 11;
	private static final int TAG_ELEM_INDEX = 12;
	private static final int TAG_ELEM_COORDS = 13;
	private static final int TAG_ELEM_LAYER = 21;
	private static final int TAG_ELEM_HEIGHT = 31;
	private static final int TAG_ELEM_MIN_HEIGHT = 32;
	private static final int TAG_ELEM_PRIORITY = 41;

	private short[] mTmpKeys = new short[100];
	private final Tag[] mTmpTags = new Tag[20];
	private Tag[][] mElementTags;

	private void initDecorder() {
		// reusable tag set
		Tag[][] tags = new Tag[10][];
		for (int i = 0; i < 10; i++)
			tags[i] = new Tag[i + 1];
		mElementTags = tags;
	}

	private boolean decode() throws IOException {

		mCurTagCnt = 0;

		int val;
		int numTags = 0;

		while (lwHttp.hasData() && (val = decodeVarint32()) > 0) {
			// read tag and wire type
			int tag = (val >> 3);

			switch (tag) {
				case TAG_TILE_NUM_TAGS:
					numTags = decodeVarint32();
					if (numTags > curTags.length)
						curTags = new Tag[numTags];
					break;

				case TAG_TILE_TAG_KEYS:
					mTmpKeys = decodeShortArray(numTags, mTmpKeys);
					break;

				case TAG_TILE_TAG_VALUES:
					// this wastes one byte, as there is no packed string...
					decodeTileTags(mCurTagCnt++);
					break;

				case TAG_TILE_LINE:
				case TAG_TILE_POLY:
				case TAG_TILE_POINT:
					decodeTileElement(tag);
					break;

				default:
					Log.d(TAG, mTile + " invalid type for tile: " + tag);
					return false;
			}
		}
		return true;
	}

	private boolean decodeTileTags(int curTag) throws IOException {
		String tagString = decodeString();

		String key = Tags.keys[mTmpKeys[curTag]];
		Tag tag;

		if (key == Tag.TAG_KEY_NAME)
			tag = new Tag(key, tagString, false);
		else
			tag = new Tag(key, tagString, true);
		if (debug)
			Log.d(TAG, mTile + " add tag: " + curTag + " " + tag);
		curTags[curTag] = tag;

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

		int end = lwHttp.position() + bytes;
		int indexCnt = 1;

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

		while (lwHttp.position() < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_ELEM_TAGS:
					tags = decodeElementTags();
					break;

				case TAG_ELEM_NUM_INDICES:
					indexCnt = decodeVarint32();
					break;

				case TAG_ELEM_INDEX:
					coordCnt = decodeWayIndices(indexCnt);
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

				case TAG_ELEM_HEIGHT:
					mElem.height = decodeVarint32();
					break;

				case TAG_ELEM_MIN_HEIGHT:
					mElem.minHeight = decodeVarint32();
					break;

				case TAG_ELEM_PRIORITY:
					mElem.priority = decodeVarint32();
					break;

				default:
					Log.d(TAG, mTile + " invalid type for way: " + tag);
			}
		}

		if (fail || tags == null || indexCnt == 0) {
			Log.d(TAG, mTile + " failed reading way: bytes:" + bytes + " index:"
					+ (Arrays.toString(index)) + " tag:"
					+ (tags != null ? Arrays.deepToString(tags) : "null") + " "
					+ indexCnt + " " + coordCnt);
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

	private Tag[] decodeElementTags() throws IOException {
		int bytes = decodeVarint32();

		Tag[] tmp = mTmpTags;

		int cnt = 0;
		int end = lwHttp.position() + bytes;
		int max = mCurTagCnt;

		while (lwHttp.position() < end) {
			int tagNum = decodeVarint32();

			if (tagNum < 0) {
				Log.d(TAG, "NULL TAG: " + mTile + " invalid tag:" + tagNum + " " + cnt);
			} else if (tagNum < Tags.MAX) {
				tmp[cnt++] = Tags.tags[tagNum];
			} else {
				tagNum -= Tags.LIMIT;

				if (tagNum >= 0 && tagNum < max) {
					// Log.d(TAG, "variable tag: " + curTags[tagNum]);
					tmp[cnt++] = curTags[tagNum];
				} else {
					Log.d(TAG, "NULL TAG: " + mTile + " could not find tag:"
							+ tagNum + " " + cnt);
				}
			}
		}

		if (cnt == 0) {
			Log.d(TAG, "got no TAG!");
		}
		Tag[] tags;

		if (cnt < 11)
			tags = mElementTags[cnt - 1];
		else
			tags = new Tag[cnt];

		for (int i = 0; i < cnt; i++)
			tags[i] = tmp[i];

		return tags;
	}

	private final static int VARINT_LIMIT = 5;
	private final static int VARINT_MAX = 10;

	private int decodeWayCoordinates(boolean skip, int nodes) throws IOException {
		int bytes = decodeVarint32();

		lwHttp.readBuffer(bytes);

		if (skip) {
			lwHttp.bufferPos += bytes;
			return nodes;
		}

		int cnt = 0;

		int lastX = 0;
		int lastY = 0;
		boolean even = true;

		float scale = mScaleFactor;
		float[] coords = mElem.ensurePointSize(nodes, false);

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;
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

		lwHttp.bufferPos = pos;

		return cnt;
	}

	private short[] decodeShortArray(int num, short[] array) throws IOException {
		int bytes = decodeVarint32();

		if (array.length < num)
			array = new short[num];

		lwHttp.readBuffer(bytes);

		int cnt = 0;

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;
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

			array[cnt++] = (short) val;
		}

		lwHttp.bufferPos = pos;

		return array;
	}

	private int decodeVarint32() throws IOException {
		if (lwHttp.bufferPos + VARINT_MAX > lwHttp.bufferFill)
			lwHttp.readBuffer(4096);

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;
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

		lwHttp.bufferPos = pos;

		return val;
	}

	private String decodeString() throws IOException {
		final int size = decodeVarint32();
		lwHttp.readBuffer(size);
		final String result = mStringDecoder.decode(lwHttp.buffer, lwHttp.bufferPos, size);

		lwHttp.bufferPos += size;

		return result;

	}
}
