/*
 * Copyright 2013
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
package org.oscim.database.mapnik;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.Pool;

import android.os.SystemClock;
import android.util.Log;

public class MapDatabase implements IMapDatabase {
	private static final String TAG = MapDatabase.class.getName();

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author",
					new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }
			);

	private final static float REF_TILE_SIZE = 4096.0f;

	// 'open' state
	private boolean mOpen = false;

	private IMapDatabaseCallback mMapGenerator;
	private float mScaleFactor;
	private MapTile mTile;

	private LwHttp lwHttp;

	private final UTF8Decoder mStringDecoder;
	private final String mLocale = "de";

	//private final MapElement mElem;

	public MapDatabase() {
		mStringDecoder = new UTF8Decoder();
		//mElem = new MapElement();
	}

	@Override
	public QueryResult executeQuery(MapTile tile, IMapDatabaseCallback mapDatabaseCallback) {
		QueryResult result = QueryResult.SUCCESS;

		mTile = tile;

		mMapGenerator = mapDatabaseCallback;

		// scale coordinates to tile size
		mScaleFactor = REF_TILE_SIZE / Tile.SIZE;

		try {

			if (lwHttp.sendRequest(tile) && lwHttp.readHeader() >= 0) {
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

		if (result != QueryResult.SUCCESS) {
			lwHttp.close();
		}
		Log.d(TAG, ">>> " + result + " >>> " + mTile);
		return result;
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

		mOpen = true;
		initDecorder();

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		mOpen = false;
		lwHttp.close();
	}

	@Override
	public String getMapProjection() {
		return null;
	}

	@Override
	public void cancel() {
	}

	private static final int TAG_TILE_LAYERS = 3;

	private static final int TAG_LAYER_VERSION = 15;
	private static final int TAG_LAYER_NAME = 1;
	private static final int TAG_LAYER_FEATURES = 2;
	private static final int TAG_LAYER_KEYS = 3;
	private static final int TAG_LAYER_VALUES = 4;
	private static final int TAG_LAYER_EXTENT = 5;

	private static final int TAG_FEATURE_ID = 1;
	private static final int TAG_FEATURE_TAGS = 2;
	private static final int TAG_FEATURE_TYPE = 3;
	private static final int TAG_FEATURE_GEOMETRY = 4;

	private static final int TAG_VALUE_STRING = 1;
	private static final int TAG_VALUE_FLOAT = 2;
	private static final int TAG_VALUE_DOUBLE = 3;
	private static final int TAG_VALUE_LONG = 4;
	private static final int TAG_VALUE_UINT = 5;
	private static final int TAG_VALUE_SINT = 6;
	private static final int TAG_VALUE_BOOL = 7;

	private static final int TAG_GEOM_UNKNOWN = 0;
	private static final int TAG_GEOM_POINT = 1;
	private static final int TAG_GEOM_LINE = 2;
	private static final int TAG_GEOM_POLYGON = 3;

	private short[] mTmpTags = new short[1024];

	private void initDecorder() {
	}

	private boolean decode() throws IOException {

		int val;

		while (lwHttp.hasData() && (val = decodeVarint32()) > 0) {
			// read tag and wire type
			int tag = (val >> 3);

			switch (tag) {
				case TAG_TILE_LAYERS:
					decodeLayer();
					break;

				default:
					Log.d(TAG, mTile + " invalid type for tile: " + tag);
					return false;
			}
		}
		return true;
	}

	private boolean decodeLayer() throws IOException {

		int version = 0;
		int extent = 4096;

		int bytes = decodeVarint32();

		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> values = new ArrayList<String>();

		String name = null;
		int numFeatures = 0;
		ArrayList<Feature> features = new ArrayList<MapDatabase.Feature>();

		int end = lwHttp.position() + bytes;
		while (lwHttp.position() < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_LAYER_KEYS:
					keys.add(decodeString());
					break;

				case TAG_LAYER_VALUES:
					values.add(decodeValue());
					break;

				case TAG_LAYER_FEATURES:
					numFeatures++;
					decodeFeature(features);
					break;

				case TAG_LAYER_VERSION:
					version = decodeVarint32();
					break;

				case TAG_LAYER_NAME:
					name = decodeString();
					break;

				case TAG_LAYER_EXTENT:
					extent = decodeVarint32();
					break;

				default:
					Log.d(TAG, mTile + " invalid type for layer: " + tag);
					break;
			}

		}

		Tag layerTag = new Tag(name, Tag.VALUE_YES);

		if (numFeatures == 0)
			return true;

		int[] ignoreLocal = new int[20];
		int numIgnore = 0;

		int fallBackLocal = -1;
		int matchedLocal = -1;

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			if (!key.startsWith(Tag.TAG_KEY_NAME))
				continue;
			int len = key.length();
			if (len == 4) {
				fallBackLocal = i;
				continue;
			}
			if (len < 7) {
				ignoreLocal[numIgnore++] = i;
				continue;
			}

			if (mLocale.equals(key.substring(5))) {
				//Log.d(TAG, "found local " + key);
				matchedLocal = i;
			} else
				ignoreLocal[numIgnore++] = i;

		}

		for (Feature f : features) {
			//Log.d(TAG, "geom: " + f.elem.type + " " + f.elem.pointPos + " tags:" + f.numTags + " "
			//		+ name);

			if (f.elem.type == GeometryType.NONE)
				continue;

			mTagSet.clear();
			mTagSet.add(layerTag);

			boolean hasName = false;
			String fallbackName = null;

			tagLoop: for (int j = 0; j < (f.numTags << 1); j += 2) {
				int keyIdx = f.tags[j];
				for (int i = 0; i < numIgnore; i++)
					if (keyIdx == ignoreLocal[i])
						continue tagLoop;

				if (keyIdx == fallBackLocal) {
					fallbackName = values.get(f.tags[j + 1]);
					continue;
				}

				String key;
				String val = values.get(f.tags[j + 1]);

				if (keyIdx == matchedLocal) {
					hasName = true;
					mTagSet.add(new Tag(Tag.TAG_KEY_NAME, val, false));

				} else {
					key = keys.get(keyIdx);
					mTagSet.add(new Tag(key, val));
				}
			}

			if (!hasName && fallbackName != null)
				mTagSet.add(new Tag(Tag.TAG_KEY_NAME, fallbackName, false));

			// FIXME extract layer tag here
			f.elem.set(mTagSet.asArray(), 5);
			mMapGenerator.renderElement(f.elem);
			mFeaturePool.release(f);
		}

		return true;
	}

	private final TagSet mTagSet = new TagSet();
	private final Pool<Feature> mFeaturePool = new Pool<Feature>() {
		int count;

		@Override
		protected Feature createItem() {
			count++;
			return new Feature();
		}

		@Override
		protected boolean clearItem(Feature item) {
			if (count > 100) {
				count--;
				return false;
			}

			item.elem.tags = null;
			item.elem.clear();
			item.tags = null;
			item.type = 0;
			item.numTags = 0;

			return true;
		}
	};

	static class Feature extends Inlist<Feature> {
		short[] tags;
		int numTags;
		int type;

		final MapElement elem;

		Feature() {
			elem = new MapElement();
		}

		boolean match(short otherTags[], int otherNumTags, int otherType) {
			if (numTags != otherNumTags)
				return false;

			if (type != otherType)
				return false;

			for (int i = 0; i < numTags << 1; i++) {
				if (tags[i] != otherTags[i])
					return false;
			}
			return true;
		}

	}

	private void decodeFeature(ArrayList<Feature> features) throws IOException {
		int bytes = decodeVarint32();
		int end = lwHttp.position() + bytes;

		int type = 0;
		long id;

		lastX = 0;
		lastY = 0;

		mTmpTags[0] = -1;

		Feature curFeature = null;
		int numTags = 0;

		//Log.d(TAG, "start feature");
		while (lwHttp.position() < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >>> 3);

			switch (tag) {
				case TAG_FEATURE_ID:
					id = decodeVarint32();
					break;

				case TAG_FEATURE_TAGS:
					mTmpTags = decodeShortArray(mTmpTags);

					for (; numTags < mTmpTags.length && mTmpTags[numTags] >= 0;)
						numTags += 2;

					numTags >>= 1;

					break;

				case TAG_FEATURE_TYPE:
					type = decodeVarint32();

					//Log.d(TAG, "got type " + type);

					break;

				case TAG_FEATURE_GEOMETRY:

					for (Feature f : features) {
						if (f.match(mTmpTags, numTags, type)) {
							curFeature = f;
							break;
						}
					}

					if (curFeature == null) {
						curFeature = mFeaturePool.get();
						curFeature.tags = new short[numTags << 1];
						System.arraycopy(mTmpTags, 0, curFeature.tags, 0, numTags << 1);
						curFeature.numTags = numTags;
						curFeature.type = type;

						features.add(curFeature);
					}

					decodeCoordinates(type, curFeature);
					break;

				default:
					Log.d(TAG, mTile + " invalid type for feature: " + tag);
					break;
			}
		}
	}

	private final static int CLOSE_PATH = 0x07;
	private final static int MOVE_TO = 0x01;
	//private final static int LINE_TO = 0x02;

	private int lastX, lastY;
	private final int pixel = 7;

	private int decodeCoordinates(int type, Feature feature) throws IOException {
		int bytes = decodeVarint32();
		lwHttp.readBuffer(bytes);

		if (feature == null) {
			lwHttp.bufferPos += bytes;
			return 0;
		}

		MapElement elem = feature.elem;

		boolean isPoint = false;
		boolean isPoly = false;
		boolean isLine = false;

		if (type == TAG_GEOM_LINE) {
			elem.startLine();
			isLine = true;
		}
		else if (type == TAG_GEOM_POLYGON) {
			elem.startPolygon();
			isPoly = true;
		} else if (type == TAG_GEOM_POINT) {
			isPoint = true;
			elem.startPoints();
		} else if (type == TAG_GEOM_UNKNOWN)
			elem.startPoints();

		boolean even = true;

		float scale = mScaleFactor;

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;
		lwHttp.bufferPos += bytes;

		int end = pos + bytes;
		int val;

		int curX = 0;
		int curY = 0;
		int prevX = 0;
		int prevY = 0;

		int cmd = 0;
		int num = 0;

		boolean first = true;
		boolean lastClip = false;

		// test bbox for outer..
		boolean isOuter = mTile.zoomLevel < 14;

		int xmin = Integer.MAX_VALUE, xmax = Integer.MIN_VALUE;
		int ymin = Integer.MAX_VALUE, ymax = Integer.MIN_VALUE;

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

			if (num == 0) {
				num = val >>> 3;
				cmd = val & 0x07;

				if (isLine && lastClip) {
					elem.addPoint(curX / scale, curY / scale);
					lastClip = false;
				}

				if (cmd == CLOSE_PATH) {
					num = 0;
					continue;
				}
				if (first) {
					first = false;
					continue;
				}
				if (cmd == MOVE_TO) {
					if (type == TAG_GEOM_LINE)
						elem.startLine();
					else if (type == TAG_GEOM_POLYGON) {
						isOuter = false;
						elem.startHole();
					}
				}
				continue;
			}
			// zigzag decoding
			int s = ((val >>> 1) ^ -(val & 1));

			if (even) {
				// get x coordinate
				even = false;
				curX = lastX = lastX + s;
				continue;
			}
			// get y coordinate and add point to geometry
			num--;

			even = true;
			curY = lastY = lastY + s;

			int dx = (curX - prevX);
			int dy = (curY - prevY);

			if ((isPoint || cmd == MOVE_TO)
					|| (dx > pixel || dx < -pixel)
					|| (dy > pixel || dy < -pixel)
					// dont clip at tile boundaries
					|| (curX <= 0 || curX >= 4095)
					|| (curY <= 0 || curY >= 4095)) {

				prevX = curX;
				prevY = curY;
				elem.addPoint(curX / scale, curY / scale);
				lastClip = false;

				if (isOuter) {
					if (curX < xmin)
						xmin = curX;
					if (curX > xmax)
						xmax = curX;

					if (curY < ymin)
						ymin = curY;
					if (curY > ymax)
						ymax = curY;
				}

				continue;
			}
			lastClip = true;
		}

		if (isPoly && isOuter && !testBBox(xmax - xmin, ymax - ymin)) {
			//Log.d(TAG, "skip small poly "+ elem.indexPos + " > "
			// +  (xmax - xmin) * (ymax - ymin));
			elem.pointPos -= elem.index[elem.indexPos];
			if (elem.indexPos > 0) {
				elem.indexPos -= 3;
				elem.index[elem.indexPos + 1] = -1;
			} else {
				elem.type = GeometryType.NONE;
			}
			return 0;
		}

		if (isLine && lastClip)
			elem.addPoint(curX / scale, curY / scale);

		return 1;
	}

	private static boolean testBBox(int dx, int dy) {
		return dx * dy > 64 * 64;
	}

	private String decodeValue() throws IOException {
		int bytes = decodeVarint32();

		String value = null;

		int end = lwHttp.position() + bytes;

		while (lwHttp.position() < end) {
			// read tag and wire type
			int val = decodeVarint32();
			if (val == 0)
				break;

			int tag = (val >> 3);

			switch (tag) {
				case TAG_VALUE_STRING:
					value = decodeString();
					break;

				case TAG_VALUE_UINT:
					value = String.valueOf(decodeVarint32());
					break;

				case TAG_VALUE_SINT:
					value = String.valueOf(decodeVarint32());
					break;

				case TAG_VALUE_LONG:
					value = String.valueOf(decodeVarint32());
					break;

				case TAG_VALUE_FLOAT:
					value = String.valueOf(decodeFloat());
					break;

				case TAG_VALUE_DOUBLE:
					value = String.valueOf(decodeDouble());
					break;

				case TAG_VALUE_BOOL:
					value = decodeBool() ? "yes" : "no";
					break;
				default:
					break;
			}

		}
		return value;
	}

	private final static int VARINT_LIMIT = 7;
	private final static int VARINT_MAX = 10;

	private short[] decodeShortArray(short[] array) throws IOException {
		int bytes = decodeVarint32();
		int arrayLength = array.length;

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

			if (arrayLength <= cnt) {
				arrayLength = cnt + 16;
				short[] tmp = array;
				array = new short[arrayLength];
				System.arraycopy(tmp, 0, array, 0, cnt);
			}

			array[cnt++] = (short) val;
		}

		lwHttp.bufferPos = pos;

		if (arrayLength > cnt)
			array[cnt] = -1;

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
				val = (buf[pos++] & 0x7f
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21);
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

	private float decodeFloat() throws IOException {
		if (lwHttp.bufferPos + 4 > lwHttp.bufferFill)
			lwHttp.readBuffer(4096);

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;

		int val = (buf[pos++] & 0xFF
				| (buf[pos++] & 0xFF) << 8
				| (buf[pos++] & 0xFF) << 16
				| (buf[pos++] & 0xFF) << 24);

		lwHttp.bufferPos += 4;

		return Float.intBitsToFloat(val);
	}

	private double decodeDouble() throws IOException {
		if (lwHttp.bufferPos + 8 > lwHttp.bufferFill)
			lwHttp.readBuffer(4096);

		byte[] buf = lwHttp.buffer;
		int pos = lwHttp.bufferPos;

		long val = (buf[pos++] & 0xFF
				| (buf[pos++] & 0xFF) << 8
				| (buf[pos++] & 0xFF) << 16
				| (buf[pos++] & 0xFF) << 24
				| (buf[pos++] & 0xFF) << 32
				| (buf[pos++] & 0xFF) << 40
				| (buf[pos++] & 0xFF) << 48
				| (buf[pos++] & 0xFF) << 56);

		lwHttp.bufferPos += 8;

		return Double.longBitsToDouble(val);
	}

	private boolean decodeBool() throws IOException {
		if (lwHttp.bufferPos + 1 > lwHttp.bufferFill)
			lwHttp.readBuffer(4096);

		boolean val = lwHttp.buffer[lwHttp.bufferPos++] != 0;

		return val;
	}

	private String decodeString() throws IOException {
		final int size = decodeVarint32();
		lwHttp.readBuffer(size);
		final String result = mStringDecoder.decode(lwHttp.buffer, lwHttp.bufferPos, size);

		//Log.d(TAG, "string:  " + result);

		lwHttp.bufferPos += size;

		return result;

	}
}
