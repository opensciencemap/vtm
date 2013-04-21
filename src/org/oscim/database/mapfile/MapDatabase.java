/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.database.mapfile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.MapOptions;
import org.oscim.database.mapfile.header.MapFileHeader;
import org.oscim.database.mapfile.header.MapFileInfo;
import org.oscim.database.mapfile.header.SubFileParameter;
import org.oscim.generator.JobTile;

import android.util.Log;

/**
 * A class for reading binary map files.
 * <p>
 * This class is not thread-safe. Each thread should use its own instance.
 *
 * @see <a
 *      href="http://code.google.com/p/mapsforge/wiki/SpecificationBinaryMapFile">Specification</a>
 */
public class MapDatabase implements IMapDatabase {
	/**
	 * Bitmask to extract the block offset from an index entry.
	 */
	private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

	/**
	 * Bitmask to extract the water information from an index entry.
	 */
	private static final long BITMASK_INDEX_WATER = 0x8000000000L;

	/**
	 * Debug message prefix for the block signature.
	 */
	private static final String DEBUG_SIGNATURE_BLOCK = "block signature: ";

	/**
	 * Debug message prefix for the POI signature.
	 */
	// private static final String DEBUG_SIGNATURE_POI = "POI signature: ";

	/**
	 * Debug message prefix for the way signature.
	 */
	private static final String DEBUG_SIGNATURE_WAY = "way signature: ";

	/**
	 * Amount of cache blocks that the index cache should store.
	 */
	private static final int INDEX_CACHE_SIZE = 64;

	/**
	 * Error message for an invalid first way offset.
	 */
	private static final String INVALID_FIRST_WAY_OFFSET = "invalid first way offset: ";

	private static final String TAG = MapDatabase.class.getName();

	/**
	 * Maximum way nodes sequence length which is considered as valid.
	 */
	private static final int MAXIMUM_WAY_NODES_SEQUENCE_LENGTH = 8192;

	/**
	 * Maximum number of map objects in the zoom table which is considered as
	 * valid.
	 */
	private static final int MAXIMUM_ZOOM_TABLE_OBJECTS = 65536;

	/**
	 * Bitmask for the optional POI feature "elevation".
	 */
	private static final int POI_FEATURE_ELEVATION = 0x20;

	/**
	 * Bitmask for the optional POI feature "house number".
	 */
	private static final int POI_FEATURE_HOUSE_NUMBER = 0x40;

	/**
	 * Bitmask for the optional POI feature "name".
	 */
	private static final int POI_FEATURE_NAME = 0x80;

	/**
	 * Bitmask for the POI layer.
	 */
	private static final int POI_LAYER_BITMASK = 0xf0;

	/**
	 * Bit shift for calculating the POI layer.
	 */
	private static final int POI_LAYER_SHIFT = 4;

	/**
	 * Bitmask for the number of POI tags.
	 */
	private static final int POI_NUMBER_OF_TAGS_BITMASK = 0x0f;

	private static final String READ_ONLY_MODE = "r";

	/**
	 * Length of the debug signature at the beginning of each block.
	 */
	private static final byte SIGNATURE_LENGTH_BLOCK = 32;

	/**
	 * Length of the debug signature at the beginning of each POI.
	 */
	private static final byte SIGNATURE_LENGTH_POI = 32;

	/**
	 * Length of the debug signature at the beginning of each way.
	 */
	private static final byte SIGNATURE_LENGTH_WAY = 32;

	/**
	 * Bitmask for the optional way data blocks byte.
	 */
	private static final int WAY_FEATURE_DATA_BLOCKS_BYTE = 0x08;

	/**
	 * Bitmask for the optional way double delta encoding.
	 */
	private static final int WAY_FEATURE_DOUBLE_DELTA_ENCODING = 0x04;

	/**
	 * Bitmask for the optional way feature "house number".
	 */
	private static final int WAY_FEATURE_HOUSE_NUMBER = 0x40;

	/**
	 * Bitmask for the optional way feature "label position".
	 */
	private static final int WAY_FEATURE_LABEL_POSITION = 0x10;

	/**
	 * Bitmask for the optional way feature "name".
	 */
	private static final int WAY_FEATURE_NAME = 0x80;

	/**
	 * Bitmask for the optional way feature "reference".
	 */
	private static final int WAY_FEATURE_REF = 0x20;

	/**
	 * Bitmask for the way layer.
	 */
	private static final int WAY_LAYER_BITMASK = 0xf0;

	/**
	 * Bit shift for calculating the way layer.
	 */
	private static final int WAY_LAYER_SHIFT = 4;

	/**
	 * Bitmask for the number of way tags.
	 */
	private static final int WAY_NUMBER_OF_TAGS_BITMASK = 0x0f;

	private static IndexCache sDatabaseIndexCache;
	private static MapFileHeader sMapFileHeader;
	private static int instances = 0;

	private long mFileSize;
	private boolean mDebugFile;
	private RandomAccessFile mInputFile;
	private ReadBuffer mReadBuffer;
	private String mSignatureBlock;
	private String mSignaturePoi;
	private String mSignatureWay;
	private int mTileLatitude;
	private int mTileLongitude;
	private int[] mIntBuffer;

	//private final GeometryBuffer mElem = new GeometryBuffer(1 << 14, 1 << 8);
	//private final WayData mWay = new WayData();
	private final MapElement mElem = new MapElement();

	private int minLat, minLon;
	private Tile mTile;
	private static boolean sMapExperimental;

	/*
	 * (non-Javadoc)
	 * @see org.oscim.map.reader.IMapDatabase#executeQuery(org.oscim.core.Tile,
	 * org.oscim.map.reader.MapDatabaseCallback)
	 */
	@Override
	public QueryResult executeQuery(JobTile tile, IMapDatabaseCallback mapDatabaseCallback) {

		if (sMapFileHeader == null)
			return QueryResult.FAILED;

		if (mIntBuffer == null)
			mIntBuffer = new int[MAXIMUM_WAY_NODES_SEQUENCE_LENGTH * 2];

		try {
			mTile = tile;

			QueryParameters queryParameters = new QueryParameters();
			queryParameters.queryZoomLevel = sMapFileHeader
					.getQueryZoomLevel(tile.zoomLevel);
			// get and check the sub-file for the query zoom level
			SubFileParameter subFileParameter = sMapFileHeader
					.getSubFileParameter(queryParameters.queryZoomLevel);
			if (subFileParameter == null) {
				Log.w(TAG, "no sub-file for zoom level: "
						+ queryParameters.queryZoomLevel);
				return QueryResult.FAILED;
			}

			QueryCalculations.calculateBaseTiles(queryParameters, tile, subFileParameter);
			QueryCalculations.calculateBlocks(queryParameters, subFileParameter);
			processBlocks(mapDatabaseCallback, queryParameters, subFileParameter);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return QueryResult.FAILED;
		}
		return QueryResult.SUCCESS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.map.reader.IMapDatabase#getMapFileInfo()
	 */
	@Override
	public MapFileInfo getMapInfo() {
		if (sMapFileHeader == null) {
			throw new IllegalStateException("no map file is currently opened");
		}
		return sMapFileHeader.getMapFileInfo();
	}

	@Override
	public String getMapProjection() {
		return "WSG84";
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.map.reader.IMapDatabase#hasOpenFile()
	 */
	@Override
	public boolean isOpen() {
		return mInputFile != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.map.reader.IMapDatabase#openFile(java.io.File)
	 */
	@Override
	public OpenResult open(MapOptions options) {
		try {
			if (options.get("file") == null) {
				throw new IllegalArgumentException("'file' must not be null");
			}

			// make sure to close any previously opened file first
			//close();

			File file = new File(options.get("file"));

			Log.d(TAG, file.getAbsolutePath());

			// File file = new File(options.get("mapfile"));

			// check if the file exists and is readable
			if (!file.exists()) {
				return new OpenResult("file does not exist: " + file);
			} else if (!file.isFile()) {
				return new OpenResult("not a file: " + file);
			} else if (!file.canRead()) {
				return new OpenResult("cannot read file: " + file);
			}

			// open the file in read only mode
			mInputFile = new RandomAccessFile(file, READ_ONLY_MODE);
			mFileSize = mInputFile.length();
			mReadBuffer = new ReadBuffer(mInputFile);

			Log.d(TAG, "open instance " + instances + " " + file.getAbsolutePath());
			if (instances++ > 0) {
				return OpenResult.SUCCESS;
			}

			sMapFileHeader = new MapFileHeader();
			OpenResult openResult = sMapFileHeader.readHeader(mReadBuffer,
					mFileSize);
			if (!openResult.isSuccess()) {
				close();
				return openResult;
			}

			sDatabaseIndexCache = new IndexCache(mInputFile, INDEX_CACHE_SIZE);
			sMapExperimental = sMapFileHeader.getMapFileInfo().fileVersion == 4;

			Log.d(TAG, "File version: " + sMapFileHeader.getMapFileInfo().fileVersion);
			return OpenResult.SUCCESS;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			// make sure that the file is closed
			close();
			return new OpenResult(e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.oscim.map.reader.IMapDatabase#closeFile()
	 */
	@Override
	public void close() {
		if (instances == 0)
			return;

		instances--;
		Log.d(TAG, "close instance " + instances);

		if (instances > 0) {
			mReadBuffer = null;
			return;
		}

		try {
			sMapFileHeader = null;

			if (sDatabaseIndexCache != null) {
				sDatabaseIndexCache.destroy();
				sDatabaseIndexCache = null;
			}

			if (mInputFile != null) {
				mInputFile.close();
				mInputFile = null;
			}

			mReadBuffer = null;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	/**
	 * Logs the debug signatures of the current way and block.
	 */
	private void logDebugSignatures() {
		if (mDebugFile) {
			Log.w(TAG, DEBUG_SIGNATURE_WAY + mSignatureWay);
			Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
		}
	}

	/**
	 * Processes a single block and executes the callback functions on all map
	 * elements.
	 *
	 * @param queryParameters
	 *            the parameters of the current query.
	 * @param subFileParameter
	 *            the parameters of the current map file.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 */
	private void processBlock(QueryParameters queryParameters,
			SubFileParameter subFileParameter,
			IMapDatabaseCallback mapDatabaseCallback) {
		if (!processBlockSignature()) {
			return;
		}

		int[][] zoomTable = readZoomTable(subFileParameter);
		if (zoomTable == null) {
			return;
		}
		int zoomTableRow = queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin;
		int poisOnQueryZoomLevel = zoomTable[zoomTableRow][0];
		int waysOnQueryZoomLevel = zoomTable[zoomTableRow][1];

		// get the relative offset to the first stored way in the block
		int firstWayOffset = mReadBuffer.readUnsignedInt();
		if (firstWayOffset < 0) {
			Log.w(TAG, INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			if (mDebugFile) {
				Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
			}
			return;
		}

		// add the current buffer position to the relative first way offset
		firstWayOffset += mReadBuffer.getBufferPosition();
		if (firstWayOffset > mReadBuffer.getBufferSize()) {
			Log.w(TAG, INVALID_FIRST_WAY_OFFSET + firstWayOffset);
			if (mDebugFile) {
				Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
			}
			return;
		}

		if (!processPOIs(mapDatabaseCallback, poisOnQueryZoomLevel)) {
			return;
		}

		// finished reading POIs, check if the current buffer position is valid
		if (mReadBuffer.getBufferPosition() > firstWayOffset) {
			Log.w(TAG, "invalid buffer position: " + mReadBuffer.getBufferPosition());
			if (mDebugFile) {
				Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
			}
			return;
		}

		// move the pointer to the first way
		mReadBuffer.setBufferPosition(firstWayOffset);
		if (!processWays(queryParameters, mapDatabaseCallback, waysOnQueryZoomLevel)) {
			return;
		}

	}



	private void processBlocks(IMapDatabaseCallback mapDatabaseCallback,
			QueryParameters queryParameters,
			SubFileParameter subFileParameter) throws IOException {
		boolean queryIsWater = true;
		// boolean queryReadWaterInfo = false;

		// read and process all blocks from top to bottom and from left to right
		for (long row = queryParameters.fromBlockY; row <= queryParameters.toBlockY; ++row) {
			for (long column = queryParameters.fromBlockX; column <= queryParameters.toBlockX; ++column) {

				// calculate the actual block number of the needed block in the
				// file
				long blockNumber = row * subFileParameter.blocksWidth + column;

				// get the current index entry
				long currentBlockIndexEntry = sDatabaseIndexCache.getIndexEntry(
						subFileParameter, blockNumber);

				// check if the current query would still return a water tile
				if (queryIsWater) {
					// check the water flag of the current block in its index
					// entry
					queryIsWater &= (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
					// queryReadWaterInfo = true;
				}

				// get and check the current block pointer
				long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
				if (currentBlockPointer < 1
						|| currentBlockPointer > subFileParameter.subFileSize) {
					Log.w(TAG, "invalid current block pointer: " + currentBlockPointer);
					Log.w(TAG, "subFileSize: " + subFileParameter.subFileSize);
					return;
				}

				long nextBlockPointer;
				// check if the current block is the last block in the file
				if (blockNumber + 1 == subFileParameter.numberOfBlocks) {
					// set the next block pointer to the end of the file
					nextBlockPointer = subFileParameter.subFileSize;
				} else {
					// get and check the next block pointer
					nextBlockPointer = sDatabaseIndexCache.getIndexEntry(
							subFileParameter, blockNumber + 1)
							& BITMASK_INDEX_OFFSET;
					if (nextBlockPointer < 1
							|| nextBlockPointer > subFileParameter.subFileSize) {
						Log.w(TAG, "invalid next block pointer: " + nextBlockPointer);
						Log.w(TAG, "sub-file size: " + subFileParameter.subFileSize);
						return;
					}
				}

				// calculate the size of the current block
				int currentBlockSize = (int) (nextBlockPointer - currentBlockPointer);
				if (currentBlockSize < 0) {
					Log.w(TAG, "current block size must not be negative: "
							+ currentBlockSize);
					return;
				} else if (currentBlockSize == 0) {
					// the current block is empty, continue with the next block
					continue;
				} else if (currentBlockSize > ReadBuffer.MAXIMUM_BUFFER_SIZE) {
					// the current block is too large, continue with the next
					// block
					Log.w(TAG, "current block size too large: " + currentBlockSize);
					continue;
				} else if (currentBlockPointer + currentBlockSize > mFileSize) {
					Log.w(TAG, "current block largher than file size: "
							+ currentBlockSize);
					return;
				}

				// seek to the current block in the map file
				mInputFile.seek(subFileParameter.startAddress + currentBlockPointer);

				// read the current block into the buffer
				if (!mReadBuffer.readFromFile(currentBlockSize)) {
					// skip the current block
					Log.w(TAG, "reading current block has failed: " + currentBlockSize);
					return;
				}

				// calculate the top-left coordinates of the underlying tile
				double tileLatitudeDeg = Projection.tileYToLatitude(
						subFileParameter.boundaryTileTop + row,
						subFileParameter.baseZoomLevel);
				double tileLongitudeDeg = Projection.tileXToLongitude(
						subFileParameter.boundaryTileLeft
								+ column, subFileParameter.baseZoomLevel);
				mTileLatitude = (int) (tileLatitudeDeg * 1000000);
				mTileLongitude = (int) (tileLongitudeDeg * 1000000);

				//try {
				processBlock(queryParameters, subFileParameter, mapDatabaseCallback);
				//} catch (ArrayIndexOutOfBoundsException e) {
				//	Log.e(TAG, e.getMessage());
				//}
			}
		}

		// the query is finished, was the water flag set for all blocks?
		// if (queryIsWater && queryReadWaterInfo) {
		// Tag[] tags = new Tag[1];
		// tags[0] = TAG_NATURAL_WATER;
		//
		// System.arraycopy(WATER_TILE_COORDINATES, 0, mWayNodes,
		// mWayNodePosition, 8);
		// mWayNodePosition += 8;
		// mapDatabaseCallback.renderWaterBackground(tags, wayDataContainer);
		// }

	}

	/**
	 * Processes the block signature, if present.
	 *
	 * @return true if the block signature could be processed successfully,
	 *         false otherwise.
	 */
	private boolean processBlockSignature() {
		if (mDebugFile) {
			// get and check the block signature
			mSignatureBlock = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
			if (!mSignatureBlock.startsWith("###TileStart")) {
				Log.w(TAG, "invalid block signature: " + mSignatureBlock);
				return false;
			}
		}
		return true;
	}

	/**
	 * Processes the given number of POIs.
	 *
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted POIs.
	 * @param numberOfPois
	 *            how many POIs should be processed.
	 * @return true if the POIs could be processed successfully, false
	 *         otherwise.
	 */
	private boolean processPOIs(IMapDatabaseCallback mapDatabaseCallback, int numberOfPois) {
		Tag[] poiTags = sMapFileHeader.getMapFileInfo().poiTags;
		Tag[] tags = null;
		Tag[] curTags;

		long x = mTile.tileX * Tile.SIZE;
		long y = mTile.tileY * Tile.SIZE + Tile.SIZE;
		long z = Tile.SIZE << mTile.zoomLevel;

		long dx = (x - (z >> 1));
		long dy = (y - (z >> 1));

		double divx = 180000000.0 / (z >> 1);
		double divy = z / PIx4;

		for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
			if (mDebugFile) {
				// get and check the POI signature
				mSignaturePoi = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
				if (!mSignaturePoi.startsWith("***POIStart")) {
					Log.w(TAG, "invalid POI signature: " + mSignaturePoi);
					Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
					return false;
				}
			}

			// get the POI latitude offset (VBE-S)
			int latitude = mTileLatitude + mReadBuffer.readSignedInt();

			// get the POI longitude offset (VBE-S)
			int longitude = mTileLongitude + mReadBuffer.readSignedInt();

			// get the special byte which encodes multiple flags
			byte specialByte = mReadBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

			if (numberOfTags != 0) {
				tags = mReadBuffer.readTags(poiTags, numberOfTags);
			}

			if (tags == null)
				return false;

			curTags = tags;

			// get the feature bitmask (1 byte)
			byte featureByte = mReadBuffer.readByte();

			// bit 1-3 enable optional features
			// check if the POI has a name
			if ((featureByte & POI_FEATURE_NAME) != 0) {
				String str = mReadBuffer.readUTF8EncodedString();

				curTags = new Tag[tags.length + 1];
				System.arraycopy(tags, 0, curTags, 0, tags.length);
				curTags[tags.length] = new Tag(Tag.TAG_KEY_NAME, str, false);
			}

			// check if the POI has a house number
			if ((featureByte & POI_FEATURE_HOUSE_NUMBER) != 0) {
				// mReadBuffer.getPositionAndSkip();
				// String str =
				mReadBuffer.readUTF8EncodedString();
			}

			// check if the POI has an elevation
			if ((featureByte & POI_FEATURE_ELEVATION) != 0) {
				mReadBuffer.readSignedInt();
				// mReadBuffer.getPositionAndSkip();// tags.add(new
				// Tag(Tag.TAG_KEY_ELE,
				// Integer.toString(mReadBuffer.readSignedInt())));
			}

			longitude = (int) (longitude / divx - dx);
			double sinLat = Math.sin(latitude * PI180);
			latitude = (int) (Math.log((1.0 + sinLat) / (1.0 - sinLat)) * divy + dy);

			mElem.clear();

			mElem.startPoints();
			mElem.addPoint(longitude, latitude);

			mElem.set(curTags, layer, MapElement.GEOM_POINT);
			mapDatabaseCallback.renderElement(mElem);

//			mGeom.points[0] = longitude;
//			mGeom.points[1] = latitude;
//			mGeom.index[0] = 2;
//			mapDatabaseCallback.renderPOI(layer, curTags, mGeom);

		}

		return true;
	}

	private boolean processWayDataBlock(boolean doubleDeltaEncoding) {
		// get and check the number of way coordinate blocks (VBE-U)
		int numBlocks = mReadBuffer.readUnsignedInt();
		if (numBlocks < 1 || numBlocks > Short.MAX_VALUE) {
			Log.w(TAG, "invalid number of way coordinate blocks: " + numBlocks);
			return false;
		}

		//short[] wayLengths = new short[numBlocks];
		short[] wayLengths = mElem.ensureIndexSize(numBlocks, false);
		if (wayLengths.length > numBlocks)
			wayLengths[numBlocks] = -1;

		mElem.pointPos = 0;

		// read the way coordinate blocks
		for (int coordinateBlock = 0; coordinateBlock < numBlocks; ++coordinateBlock) {
			// get and check the number of way nodes (VBE-U)
			int numWayNodes = mReadBuffer.readUnsignedInt();

			if (numWayNodes < 2 || numWayNodes > MAXIMUM_WAY_NODES_SEQUENCE_LENGTH) {
				Log.w(TAG, "invalid number of way nodes: " + numWayNodes);
				logDebugSignatures();
				return false;
			}

			// each way node consists of latitude and longitude
			int len = numWayNodes * 2;

			if (doubleDeltaEncoding) {
				len = decodeWayNodesDoubleDelta(len);
			} else {
				len = decodeWayNodesSingleDelta(len);
			}
			wayLengths[coordinateBlock] = (short) len;
		}

		return true;
	}

	private int decodeWayNodesDoubleDelta(int length) {
		int[] buffer = mIntBuffer;

		mReadBuffer.readSignedInt(buffer, length);

		float[] outBuffer = mElem.ensurePointSize(mElem.pointPos + length, true);
		int pointPos = mElem.pointPos;

		// get the first way node latitude offset (VBE-S)
		int wayNodeLatitude = mTileLatitude + buffer[0];

		// get the first way node longitude offset (VBE-S)
		int wayNodeLongitude = mTileLongitude + buffer[1];

		// store the first way node
		outBuffer[pointPos++] = wayNodeLongitude;
		outBuffer[pointPos++] = wayNodeLatitude;

		int singleDeltaLatitude = 0;
		int singleDeltaLongitude = 0;

		int cnt = 2, nLon, nLat, dLat, dLon;

		for (int pos = 2; pos < length; pos += 2) {

			singleDeltaLatitude = buffer[pos] + singleDeltaLatitude;
			nLat = wayNodeLatitude + singleDeltaLatitude;
			dLat = nLat - wayNodeLatitude;
			wayNodeLatitude = nLat;

			singleDeltaLongitude = buffer[pos + 1] + singleDeltaLongitude;
			nLon = wayNodeLongitude + singleDeltaLongitude;
			dLon = nLon - wayNodeLongitude;
			wayNodeLongitude = nLon;

			if (dLon > minLon || dLon < -minLon || dLat > minLat || dLat < -minLat
					|| (pos == length - 2)) {
				outBuffer[pointPos++] = nLon;
				outBuffer[pointPos++] = nLat;
				cnt += 2;
			}
		}

		mElem.pointPos = pointPos;

		return cnt;
	}

	private int decodeWayNodesSingleDelta(int length) {
		int[] buffer = mIntBuffer;
		mReadBuffer.readSignedInt(buffer, length);

		float[] outBuffer = mElem.ensurePointSize(mElem.pointPos + length, true);
		int pointPos = mElem.pointPos;

		// get the first way node latitude single-delta offset (VBE-S)
		int wayNodeLatitude = mTileLatitude + buffer[0];

		// get the first way node longitude single-delta offset (VBE-S)
		int wayNodeLongitude = mTileLongitude + buffer[1];

		// store the first way node
		outBuffer[pointPos++] = wayNodeLongitude;
		outBuffer[pointPos++] = wayNodeLatitude;

		int cnt = 2, nLon, nLat, dLat, dLon;

		for (int pos = 2; pos < length; pos += 2) {

			nLat = wayNodeLatitude + buffer[pos];
			dLat = nLat - wayNodeLatitude;
			wayNodeLatitude = nLat;

			nLon = wayNodeLongitude + buffer[pos + 1];
			dLon = nLon - wayNodeLongitude;
			wayNodeLongitude = nLon;

			if (dLon > minLon || dLon < -minLon || dLat > minLat || dLat < -minLat
					|| (pos == length - 2)) {
				outBuffer[pointPos++] = nLon;
				outBuffer[pointPos++] = nLat;
				cnt += 2;
			}
		}

		mElem.pointPos = pointPos;
		return cnt;
	}

	private int stringOffset = -1;

	/**
	 * Processes the given number of ways.
	 *
	 * @param queryParameters
	 *            the parameters of the current query.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted ways.
	 * @param numberOfWays
	 *            how many ways should be processed.
	 * @return true if the ways could be processed successfully, false
	 *         otherwise.
	 */
	private boolean processWays(QueryParameters queryParameters,
			IMapDatabaseCallback mapDatabaseCallback,
			int numberOfWays) {

		Tag[] tags = null;
		Tag[] curTags;
		Tag[] wayTags = sMapFileHeader.getMapFileInfo().wayTags;

		int wayDataBlocks;

		// skip string block
		int stringsSize = 0;
		stringOffset = 0;

		if (sMapExperimental) {
			stringsSize = mReadBuffer.readUnsignedInt();
			stringOffset = mReadBuffer.getBufferPosition();
			mReadBuffer.skipBytes(stringsSize);
		}

		for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
			if (mDebugFile) {
				// get and check the way signature
				mSignatureWay = mReadBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
				if (!mSignatureWay.startsWith("---WayStart")) {
					Log.w(TAG, "invalid way signature: " + mSignatureWay);
					Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
					return false;
				}
			}

			if (queryParameters.useTileBitmask) {
				elementCounter = mReadBuffer.skipWays(queryParameters.queryTileBitmask,
						elementCounter);

				if (elementCounter == 0)
					return true;

				if (elementCounter < 0)
					return false;

				if (sMapExperimental && mReadBuffer.lastTagPosition > 0) {
					int pos = mReadBuffer.getBufferPosition();
					mReadBuffer.setBufferPosition(mReadBuffer.lastTagPosition);

					byte numberOfTags = (byte) (mReadBuffer.readByte() & WAY_NUMBER_OF_TAGS_BITMASK);

					tags = mReadBuffer.readTags(wayTags, numberOfTags);
					if (tags == null)
						return false;

					mReadBuffer.setBufferPosition(pos);
				}
			} else {
				int wayDataSize = mReadBuffer.readUnsignedInt();
				if (wayDataSize < 0) {
					Log.w(TAG, "invalid way data size: " + wayDataSize);
					if (mDebugFile) {
						Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
					}
					Log.e(TAG, "BUG way 2");
					return false;
				}

				// ignore the way tile bitmask (2 bytes)
				mReadBuffer.skipBytes(2);
			}

			// get the special byte which encodes multiple flags
			byte specialByte = mReadBuffer.readByte();

			// bit 1-4 represent the layer
			byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);
			// bit 5-8 represent the number of tag IDs
			byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

			if (numberOfTags != 0)
				tags = mReadBuffer.readTags(wayTags, numberOfTags);

			if (tags == null)
				return false;

			curTags = tags;

			// get the feature bitmask (1 byte)
			byte featureByte = mReadBuffer.readByte();

			// bit 1-6 enable optional features
			boolean featureWayDoubleDeltaEncoding = (featureByte & WAY_FEATURE_DOUBLE_DELTA_ENCODING) != 0;

			boolean hasName = (featureByte & WAY_FEATURE_NAME) != 0;
			boolean hasHouseNr = (featureByte & WAY_FEATURE_HOUSE_NUMBER) != 0;
			boolean hasRef = (featureByte & WAY_FEATURE_REF) != 0;

			int add = (hasName ? 1 : 0) + (hasHouseNr ? 1 : 0) + (hasRef ? 1 : 0);
			int addTag = tags.length;

			if (add > 0) {
				curTags = new Tag[tags.length + add];
				System.arraycopy(tags, 0, curTags, 0, tags.length);
			}

			if (sMapExperimental) {
				if (hasName) {
					int textPos = mReadBuffer.readUnsignedInt();
					String str = mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos);
					curTags[addTag++] = new Tag(Tag.TAG_KEY_NAME, str, false);
				}
				if (hasHouseNr) {
					int textPos = mReadBuffer.readUnsignedInt();
					String str = mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos);
					curTags[addTag++] = new Tag(Tag.TAG_KEY_HOUSE_NUMBER, str, false);
				}
				if (hasRef) {
					int textPos = mReadBuffer.readUnsignedInt();
					String str = mReadBuffer.readUTF8EncodedStringAt(stringOffset + textPos);
					curTags[addTag++] = new Tag(Tag.TAG_KEY_REF, str, false);
				}
			} else {
				if (hasName) {
					String str = mReadBuffer.readUTF8EncodedString();
					curTags[addTag++] = new Tag(Tag.TAG_KEY_NAME, str, false);
				}
				if (hasHouseNr) {
					String str = mReadBuffer.readUTF8EncodedString();
					curTags[addTag++] = new Tag(Tag.TAG_KEY_HOUSE_NUMBER, str, false);
				}
				if (hasRef) {
					String str = mReadBuffer.readUTF8EncodedString();
					curTags[addTag++] = new Tag(Tag.TAG_KEY_REF, str, false);
				}
			}
			if ((featureByte & WAY_FEATURE_LABEL_POSITION) != 0)
				// labelPosition =
				readOptionalLabelPosition();

			if ((featureByte & WAY_FEATURE_DATA_BLOCKS_BYTE) != 0) {
				wayDataBlocks = mReadBuffer.readUnsignedInt();

				if (wayDataBlocks < 1) {
					Log.w(TAG, "invalid number of way data blocks: " + wayDataBlocks);
					logDebugSignatures();
					return false;
				}
			} else {
				wayDataBlocks = 1;
			}

			for (int wayDataBlock = 0; wayDataBlock < wayDataBlocks; ++wayDataBlock) {
				if (!processWayDataBlock(featureWayDoubleDeltaEncoding))
					return false;

				// wayDataContainer.textPos = textPos;
				int l = mElem.index[0];

				boolean closed = mElem.points[0] == mElem.points[l - 2]
						&& mElem.points[1] == mElem.points[l - 1];

				projectToTile(mElem.points, mElem.index);
				mElem.layer = layer;

				mElem.geometryType = closed ? MapElement.GEOM_POLY : MapElement.GEOM_LINE;
				mElem.tags = curTags;

				mapDatabaseCallback.renderElement(mElem);
			}
		}

		return true;
	}

	private float[] readOptionalLabelPosition() {
		float[] labelPosition = new float[2];

		// get the label position latitude offset (VBE-S)
		labelPosition[1] = mTileLatitude + mReadBuffer.readSignedInt();

		// get the label position longitude offset (VBE-S)
		labelPosition[0] = mTileLongitude + mReadBuffer.readSignedInt();

		return labelPosition;
	}

	// private int readOptionalWayDataBlocksByte(boolean
	// featureWayDataBlocksByte) {
	// if (featureWayDataBlocksByte) {
	// // get and check the number of way data blocks (VBE-U)
	// return mReadBuffer.readUnsignedInt();
	// }
	// // only one way data block exists
	// return 1;
	// }

	private int[][] readZoomTable(SubFileParameter subFileParameter) {
		int rows = subFileParameter.zoomLevelMax - subFileParameter.zoomLevelMin + 1;
		int[][] zoomTable = new int[rows][2];

		int cumulatedNumberOfPois = 0;
		int cumulatedNumberOfWays = 0;

		for (int row = 0; row < rows; ++row) {
			cumulatedNumberOfPois += mReadBuffer.readUnsignedInt();
			cumulatedNumberOfWays += mReadBuffer.readUnsignedInt();

			if (cumulatedNumberOfPois < 0
					|| cumulatedNumberOfPois > MAXIMUM_ZOOM_TABLE_OBJECTS) {
				Log.w(TAG, "invalid cumulated number of POIs in row " + row + ' '
						+ cumulatedNumberOfPois);
				if (mDebugFile) {
					Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
				}
				return null;
			} else if (cumulatedNumberOfWays < 0
					|| cumulatedNumberOfWays > MAXIMUM_ZOOM_TABLE_OBJECTS) {
				Log.w(TAG, "invalid cumulated number of ways in row " + row + ' '
						+ cumulatedNumberOfWays);
				if (sMapFileHeader.getMapFileInfo().debugFile) {
					Log.w(TAG, DEBUG_SIGNATURE_BLOCK + mSignatureBlock);
				}
				return null;
			}

			zoomTable[row][0] = cumulatedNumberOfPois;
			zoomTable[row][1] = cumulatedNumberOfWays;
		}

		return zoomTable;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}

	private static final double PI180 = (Math.PI / 180) / 1000000.0;
	private static final double PIx4 = Math.PI * 4;

	private boolean projectToTile(float[] coords, short[] indices) {

		long x = mTile.tileX * Tile.SIZE;
		long y = mTile.tileY * Tile.SIZE + Tile.SIZE;
		long z = Tile.SIZE << mTile.zoomLevel;

		double divx, divy = 0;
		long dx = (x - (z >> 1));
		long dy = (y - (z >> 1));

		divx = 180000000.0 / (z >> 1);
		divy = z / PIx4;

		for (int pos = 0, outPos = 0, i = 0, m = indices.length; i < m; i++) {
			int len = indices[i];
			if (len == 0)
				continue;
			if (len < 0)
				break;

			int cnt = 0;
			float lat, lon, prevLon = 0, prevLat = 0;
			int first = outPos;

			for (int end = pos + len; pos < end; pos += 2) {

				lon = (float) ((coords[pos]) / divx - dx);
				double sinLat = Math.sin(coords[pos + 1] * PI180);
				lat = (float) (Tile.SIZE - (Math.log((1.0 + sinLat) / (1.0 - sinLat)) * divy + dy));

				if (cnt != 0) {
					// drop small distance intermediate nodes
					if (lat == prevLat && lon == prevLon)
						continue;
				}
				coords[outPos++] = prevLon = lon;
				coords[outPos++] = prevLat = lat;

				cnt += 2;
			}
			if (coords[first] == coords[outPos - 2] && coords[first + 1] == coords[outPos - 1]) {
				//Log.d(TAG, "drop closed");
				indices[i] = (short) (cnt - 2);
				outPos -= 2;
			}
			else
				indices[i] = (short) cnt;
		}

		return true;
	}

}
