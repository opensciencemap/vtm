/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.tiling.source.oscimap2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.common.LwHttp;
import org.oscim.tiling.source.common.PbfDecoder;
import org.oscim.tiling.source.common.PbfTileDataSource;
import org.oscim.tiling.source.common.UrlTileSource;

import org.oscim.backend.Log;

public class OSciMap2TileSource extends UrlTileSource {

	@Override
	public ITileDataSource getDataSource() {
		return new TileDataSource(mUrl);
	}

	class TileDataSource extends PbfTileDataSource {
		public TileDataSource(URL url) {
			super(new TileDecoder());
			mConn = new LwHttp(url, "application/osmtile", "osmtile", false);
		}
	}

	static class TileDecoder extends PbfDecoder {
		private final static String TAG = TileDecoder.class.getName();
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

		private short[] mSArray;
		private final TagSet mTileTags;
		private final MapElement mElem;

		private Tile mTile;

		private ITileDataSink mMapDataSink;

		// scale coordinates to tile size
		private final static float REF_TILE_SIZE = 4096.0f;
		private float mScale;

		TileDecoder() {
			mElem = new MapElement();
			mTileTags = new TagSet(20);

			// temp array for decoding shorts
			mSArray = new short[100];
		}

		@Override
		public boolean decode(Tile tile, ITileDataSink sink, InputStream is, int contentLength)
		        throws IOException {

			int byteCount = readUnsignedInt(is, buffer);
			//Log.d(TAG, tile + " contentLength:" + byteCount);
			if (byteCount < 0) {
				Log.d(TAG, tile + " invalid content length: " + byteCount);
				return false;
			}

			setInputStream(is, byteCount);

			mTile = tile;
			mMapDataSink = sink;

			mScale = REF_TILE_SIZE / Tile.SIZE;

			mTileTags.clear();

			int val;
			int numTags = 0;

			while (hasData() && (val = decodeVarint32()) > 0) {
				// read tag and wire type
				int tag = (val >> 3);

				switch (tag) {
					case TAG_TILE_NUM_TAGS:
						numTags = decodeVarint32();
						break;

					case TAG_TILE_TAG_KEYS:
						int len = numTags;
						if (mSArray.length < len)
							mSArray = new short[len];

						decodeVarintArray(numTags, mSArray);
						break;

					case TAG_TILE_TAG_VALUES:
						// this wastes one byte, as there is no packed string...
						decodeTileTags();
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

		private boolean decodeTileTags() throws IOException {
			String tagString = decodeString();

			int curTag = mTileTags.numTags;

			String key = Tags.keys[mSArray[curTag]];
			Tag tag;

			if (key == Tag.TAG_KEY_NAME)
				tag = new Tag(key, tagString, false);
			else
				tag = new Tag(key, tagString, true);
			if (debug)
				Log.d(TAG, mTile + " add tag: " + curTag + " " + tag);

			mTileTags.add(tag);

			return true;
		}

		private int decodeWayIndices(int indexCnt) throws IOException {
			mElem.ensureIndexSize(indexCnt, false);
			decodeVarintArray(indexCnt, mElem.index);

			short[] index = mElem.index;
			int coordCnt = 0;

			for (int i = 0; i < indexCnt; i++) {
				coordCnt += index[i];
				index[i] *= 2;
			}
			// set end marker
			if (indexCnt < index.length)
				index[indexCnt] = -1;

			return coordCnt;
		}

		private boolean decodeTileElement(int type) throws IOException {

			int bytes = decodeVarint32();
			short[] index = null;

			int end = position() + bytes;
			int indexCnt = 1;

			boolean fail = false;

			int coordCnt = 0;
			if (type == TAG_TILE_POINT) {
				coordCnt = 1;
				mElem.index[0] = 2;
			}

			mElem.layer = 5;
			mElem.priority = 0;
			mElem.height = 0;
			mElem.minHeight = 0;

			while (position() < end) {
				// read tag and wire type
				int val = decodeVarint32();
				if (val == 0)
					break;

				int tag = (val >> 3);

				switch (tag) {
					case TAG_ELEM_TAGS:
						if (!decodeElementTags())
							return false;
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
						}

						mElem.ensurePointSize(coordCnt, false);
						int cnt = decodeInterleavedPoints(mElem.points, mScale);

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

			if (fail || indexCnt == 0) {
				Log.d(TAG, mTile + " failed reading way: bytes:" + bytes + " index:"
				        + (Arrays.toString(index)) + " tag:"
				        + (mElem.tags.numTags > 0 ? Arrays.deepToString(mElem.tags.tags) : "null")
				        + " " + indexCnt + " " + coordCnt);
				return false;
			}

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

			mMapDataSink.process(mElem);

			return true;
		}

		private boolean decodeElementTags() throws IOException {
			int bytes = decodeVarint32();

			mElem.tags.clear();

			int cnt = 0;
			int end = position() + bytes;
			int max = mTileTags.numTags - 1;

			for (; position() < end; cnt++) {
				int tagNum = decodeVarint32();

				if (tagNum < 0) {
					Log.d(TAG, "NULL TAG: " + mTile
					        + " invalid tag:"
					        + tagNum + " " + cnt);
					return false;
				}

				if (tagNum < Tags.MAX) {
					mElem.tags.add(Tags.tags[tagNum]);
					continue;
				}
				tagNum -= Tags.LIMIT;

				if (tagNum < 0 || tagNum > max) {
					Log.d(TAG, "NULL TAG: " + mTile
					        + " could not find tag:"
					        + tagNum + " " + cnt);
					return false;
				}

				mElem.tags.add(mTileTags.tags[tagNum]);
			}

			if (cnt == 0) {
				Log.d(TAG, "got no TAG!");
				return false;
			}
			return true;
		}
	}
}
