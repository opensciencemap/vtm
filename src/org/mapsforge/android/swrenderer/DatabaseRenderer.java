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
package org.mapsforge.android.swrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.JobTheme;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.rendertheme.IRenderCallback;
import org.mapsforge.android.rendertheme.RenderTheme;
import org.mapsforge.android.rendertheme.RenderThemeHandler;
import org.mapsforge.android.rendertheme.renderinstruction.Area;
import org.mapsforge.android.rendertheme.renderinstruction.Line;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.MapFileInfo;
import org.mapsforge.database.mapfile.MapDatabase;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.FloatMath;
import android.util.Log;

/**
 * A DatabaseRenderer renders map tiles by reading from a {@link MapDatabase}.
 */
public class DatabaseRenderer implements IMapGenerator, IRenderCallback,
		IMapDatabaseCallback {
	private static String TAG = DatabaseRenderer.class.getName();
	private static final Byte DEFAULT_START_ZOOM_LEVEL = Byte.valueOf((byte) 12);
	private static final byte LAYERS = 11;
	private static final Paint PAINT_WATER_TILE_HIGHTLIGHT = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final double STROKE_INCREASE = 1.5;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;

	private static final byte ZOOM_MAX = 22;

	// private static MapRenderer mMapRenderer;

	private static RenderTheme getRenderTheme(JobTheme jobTheme) {
		InputStream inputStream = null;
		try {
			inputStream = jobTheme.getRenderThemeAsStream();
			return RenderThemeHandler.getRenderTheme(inputStream);
		} catch (ParserConfigurationException e) {
			Log.e(TAG, e.getMessage());
		} catch (SAXException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return null;
	}

	private static byte getValidLayer(byte layer) {
		if (layer < 0) {
			return 0;
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	private final CanvasRasterer mCanvasRasterer;

	private LayerContainer mDrawingLayer;
	private final LabelPlacement mLabelPlacement;
	private IMapDatabase mMapDatabase;
	private List<PointTextContainer> mNodes;
	private float mPoiX;
	private float mPoiY;
	private JobTheme mPreviousJobTheme;
	private float mPreviousTextScale;
	private byte mPreviousZoomLevel;
	private static RenderTheme renderTheme;

	private final List<WayTextContainer> mWayNames;
	private final LayerContainer[] mWays;
	private final List<SymbolContainer> mWaySymbols;
	private final List<SymbolContainer> mPointSymbols;
	private final List<PointTextContainer> mAreaLabels;

	// private float mLat1, mLat2, mLon1, mLon2;
	// private float mTileWidth, mTileHeight;
	private float mScale;

	// private float[] mCoordinates;
	private WayDataContainer mWayDataContainer;
	private final Bitmap mTileBitmap;

	private static float PI180 = (float) (Math.PI / 180) / 1000000.0f;
	private static float PIx4 = (float) Math.PI * 4;

	private Tile mCurrentTile;
	private static long mCurrentTileY;
	private static long mCurrentTileX;
	private static long mCurrentTileZoom;

	private float[] mCoords = null;

	// private long _renderTime;
	private int _nodes, _nodesDropped;

	/**
	 * Constructs a new DatabaseRenderer.
	 */
	public DatabaseRenderer() {
		mCanvasRasterer = new CanvasRasterer();
		mLabelPlacement = new LabelPlacement();

		mWays = new LayerContainer[LAYERS];
		mWayNames = new ArrayList<WayTextContainer>(64);
		mNodes = new ArrayList<PointTextContainer>(64);
		mAreaLabels = new ArrayList<PointTextContainer>(64);
		mWaySymbols = new ArrayList<SymbolContainer>(64);
		mPointSymbols = new ArrayList<SymbolContainer>(64);

		PAINT_WATER_TILE_HIGHTLIGHT.setStyle(Paint.Style.FILL);
		PAINT_WATER_TILE_HIGHTLIGHT.setColor(Color.CYAN);
		// mCoordinates = new float[1024];

		mTileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE * 2, Tile.TILE_SIZE * 2,
				Bitmap.Config.RGB_565);

	}

	@Override
	public void cleanup() {
		mTileBitmap.recycle();
		if (DatabaseRenderer.renderTheme != null) {
			DatabaseRenderer.renderTheme.destroy();
		}
	}

	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob) {
		long time_load = System.currentTimeMillis();
		_nodes = 0;
		_nodesDropped = 0;
		// _renderTime = 0;

		mCurrentTile = mapGeneratorJob.tile;
		mCurrentTileZoom = ((long) Tile.TILE_SIZE << mCurrentTile.zoomLevel);
		mCurrentTileX = mCurrentTile.pixelX;
		mCurrentTileY = mCurrentTile.pixelY;

		// mLon1 = (float) MercatorProjection.pixelXToLongitude(mCurrentTileX, mCurrentTile.zoomLevel) * 1000000;
		// mLat1 = (float) MercatorProjection.pixelYToLatitude(mCurrentTileY, mCurrentTile.zoomLevel) * 1000000;
		// mLon2 = (float) MercatorProjection.pixelXToLongitude(mCurrentTileX + Tile.TILE_SIZE, mCurrentTile.zoomLevel)
		// * 1000000;
		// mLat2 = (float) MercatorProjection.pixelYToLatitude(mCurrentTileY + Tile.TILE_SIZE, mCurrentTile.zoomLevel) *
		// 1000000;
		//
		// mTileWidth = mLon2 - mLon1;
		// mTileHeight = mLat1 - mLat2;
		mScale = mapGeneratorJob.getScale();

		JobTheme jobTheme = mapGeneratorJob.jobParameters.jobTheme;
		if (!jobTheme.equals(mPreviousJobTheme)) {
			if (DatabaseRenderer.renderTheme == null)
				DatabaseRenderer.renderTheme = getRenderTheme(jobTheme);
			if (DatabaseRenderer.renderTheme == null) {
				mPreviousJobTheme = null;
				return false;
			}
			createWayLists();
			mPreviousJobTheme = jobTheme;
			mPreviousZoomLevel = Byte.MIN_VALUE;
		}

		byte zoomLevel = mCurrentTile.zoomLevel;
		if (zoomLevel != mPreviousZoomLevel) {
			setScaleStrokeWidth(zoomLevel);
			mPreviousZoomLevel = zoomLevel;
		}

		float textScale = mapGeneratorJob.jobParameters.textScale;
		if (textScale != mPreviousTextScale) {
			DatabaseRenderer.renderTheme.scaleTextSize(textScale);
			mPreviousTextScale = textScale;
		}

		if (mMapDatabase != null) {
			mMapDatabase.executeQuery(mCurrentTile, this);
		}
		else {
			return false;
		}
		time_load = System.currentTimeMillis() - time_load;

		mNodes = mLabelPlacement.placeLabels(mNodes, mPointSymbols, mAreaLabels,
				mCurrentTile);

		long time_draw = System.currentTimeMillis();

		// FIXME mCoords = mMapDatabase.getCoordinates();

		mCanvasRasterer.setCanvasBitmap(mTileBitmap, mScale);
		mCanvasRasterer.fill(DatabaseRenderer.renderTheme.getMapBackground());
		mCanvasRasterer.drawWays(mCoords, mWays);
		mCanvasRasterer.drawSymbols(mWaySymbols);
		mCanvasRasterer.drawSymbols(mPointSymbols);
		mCanvasRasterer.drawWayNames(mCoords, mWayNames);
		mCanvasRasterer.drawNodes(mNodes);
		mCanvasRasterer.drawNodes(mAreaLabels);
		time_draw = System.currentTimeMillis() - time_draw;

		if (mapGeneratorJob.debugSettings.mDrawTileFrames) {
			mCanvasRasterer.drawTileFrame();
		}

		if (mapGeneratorJob.debugSettings.mDrawTileCoordinates) {
			mCanvasRasterer.drawTileCoordinates(mCurrentTile, time_load, time_draw,
					_nodes, _nodesDropped);
		}

		clearLists();

		mapGeneratorJob.setBitmap(mTileBitmap);

		return true;
	}

	@Override
	public GeoPoint getStartPoint() {
		if (mMapDatabase != null && mMapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = mMapDatabase.getMapFileInfo();
			if (mapFileInfo.startPosition != null) {
				return mapFileInfo.startPosition;
			} else if (mapFileInfo.mapCenter != null) {
				return mapFileInfo.mapCenter;
			}
		}

		return null;
	}

	@Override
	public Byte getStartZoomLevel() {
		if (mMapDatabase != null && mMapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = mMapDatabase.getMapFileInfo();
			if (mapFileInfo.startZoomLevel != null) {
				return mapFileInfo.startZoomLevel;
			}
		}

		return DEFAULT_START_ZOOM_LEVEL;
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}

	@Override
	public void renderAreaCaption(String textKey, float verticalOffset, Paint paint,
			Paint stroke) {
		// mapDatabase.readTag(caption);
		// if (caption.value != null) {
		// float[] centerPosition = GeometryUtils
		// .calculateCenterOfBoundingBox(coordinates[0]);
		// areaLabels.add(new PointTextContainer(caption.value,
		// centerPosition[0],
		// centerPosition[1],
		// paint, stroke));
		// }
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
		// float[] centerPosition = GeometryUtils
		// .calculateCenterOfBoundingBox(coordinates[0]);
		// pointSymbols.add(new SymbolContainer(symbol, centerPosition[0]
		// - (symbol.getWidth() >> 1), centerPosition[1]
		// - (symbol.getHeight() >> 1)));
	}

	@Override
	public void renderPointOfInterest(byte layer, int latitude, int longitude, Tag[] tags) {
		mDrawingLayer = mWays[getValidLayer(layer)];
		mPoiX = scaleLongitude(longitude);
		mPoiY = scaleLatitude(latitude);
		DatabaseRenderer.renderTheme.matchNode(this, tags, mCurrentTile.zoomLevel);
	}

	@Override
	public void renderPointOfInterestCaption(String textKey, float verticalOffset,
			Paint paint, Paint stroke) {
		// mapDatabase.readTag(caption);
		// if (caption.value != null) {
		// nodes.add(new PointTextContainer(caption.value, poiX, poiY + verticalOffset, paint, stroke));
		// }
	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint outline, int level) {

		mDrawingLayer.add(level, new CircleContainer(mPoiX, mPoiY, radius), outline);
	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap symbol) {
		mPointSymbols.add(new SymbolContainer(symbol, mPoiX - (symbol.getWidth() >> 1),
				mPoiY
						- (symbol.getHeight() >> 1)));
	}

	@Override
	public void renderWaterBackground() {
		// if (mCoords == null)
		// mCoords = mMapDatabase.getCoordinates();

		// float[] coords = mCoords;
		//
		// mDrawingLayer = mWays[5];
		//
		// int len = wayData.length[0];
		// int pos = wayData.position[0];
		//
		// for (int j = pos, m = pos + len; j < m; j += 2) {
		// coords[j] = coords[j] * mScale;
		// coords[j + 1] = coords[j + 1] * mScale;
		// }
		//
		// mWayDataContainer = wayData;
		//
		// Log.i("mapsforge", "render water");
		//
		// DatabaseRenderer.renderTheme.matchWay(this, tags, mCurrentTile.zoomLevel, true);
	}

	// private boolean mPrevClosed = false;
	// private byte mPrevLayer = 0;

	@Override
	public void renderWay(byte layer, Tag[] tags, float[] wayNodes, int[] wayLengths,
			boolean changed) {
		// if (mCoords == null)
		// mCoords = mMapDatabase.getCoordinates();

		// float[] coords = mCoords;
		//
		// boolean closed = false;
		// boolean added = false;
		//
		// // coordinatesLength = wayData.length.length;
		// if (mCurrentTile.zoomLevel < 6) {
		// long x = mCurrentTileX;
		// long y = mCurrentTileY;
		// long z = mCurrentTileZoom;
		// float s = mScale;
		//
		// added = true;
		//
		// for (int i = wayData.length.length - 1; i >= 0; i--) {
		// int len = wayData.length[i];
		// int pos = wayData.position[i];
		//
		// if (i == 0)
		// closed = (coords[pos] == coords[(pos + len) - 2] &&
		// coords[pos + 1] == coords[(pos + len) - 1]);
		//
		// for (int j = pos, m = pos + len; j < m; j += 2) {
		//
		// coords[j] = (float) (((coords[j] / 1000000.0 + 180) / 360 * z) - x) * s;
		//
		// double sinLat = Math.sin(coords[j + 1] * PI180);
		// coords[j + 1] = (float) ((0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / PIx4) * z - y) * s;
		// }
		// }
		// } else {
		// // use linear approximation on high zoom levels.
		// float ssize = Tile.TILE_SIZE * mScale;
		// float sw = ssize / mTileWidth;
		// float sh = ssize / mTileHeight;
		// int j, o;
		// float x, y;
		//
		// int min = 1;
		// if (mCurrentTile.zoomLevel < 14)
		// min = 3;
		// else if (mCurrentTile.zoomLevel < 9)
		// min = 5;
		//
		// for (int i = wayData.length.length - 1; i >= 0; i--) {
		//
		// int len = wayData.length[i];
		// int pos = wayData.position[i];
		// _nodes += len / 2;
		//
		// if (i == 0) {
		// closed = (coords[pos] == coords[(pos + len) - 2] &&
		// coords[pos + 1] == coords[(pos + len) - 1]);
		// }
		//
		// coords[pos] = (coords[pos] - mLon1) * sw;
		// coords[pos + 1] = ssize - (coords[pos + 1] - mLat2) * sh;
		//
		// j = o = pos + 2;
		//
		// // drop intermediate nodes with less than 'min' distance.
		// for (int m = pos + len - 2; j < m; j += 2) {
		// x = (coords[j] - mLon1) * sw;
		// y = ssize - (coords[j + 1] - mLat2) * sh;
		//
		// if (x > coords[o - 2] + min || x < coords[o - 2] - min ||
		// y > coords[o - 1] + min || y < coords[o - 1] - min) {
		//
		// coords[o++] = x;
		// coords[o++] = y;
		// } else
		// _nodesDropped++;
		// }
		// coords[o] = (coords[j] - mLon1) * sw;
		// coords[o + 1] = ssize - (coords[j + 1] - mLat2) * sh;
		// o += 2;
		//
		// wayData.length[i] = o - pos;
		//
		// if (!closed || (o - pos) > 4)
		// added = true;
		// else
		// wayData.length[i] = 0;
		// }
		// }
		//
		// if (!added && !changed)
		// return;
		//
		// mWayDataContainer = wayData;
		//
		// mDrawingLayer = mWays[getValidLayer(layer)];
		//
		// if (changed || (closed != mPrevClosed) || (layer != mPrevLayer)) {
		// mCurLevelContainer1 = null;
		// mCurLevelContainer2 = null;
		// DatabaseRenderer.renderTheme.matchWay(this, tags, mCurrentTile.zoomLevel, closed);
		// } else {
		// if (mCurLevelContainer1 != null)
		// mCurLevelContainer1.add(mWayDataContainer);
		// if (mCurLevelContainer2 != null)
		// mCurLevelContainer2.add(mWayDataContainer);
		// }
		// mPrevClosed = closed;
		// mPrevLayer = layer;
	}

	private List<ShapeContainer> mCurLevelContainer1;
	private List<ShapeContainer> mCurLevelContainer2;

	@Override
	public void renderWay(Line line) {
		List<ShapeContainer> c = mDrawingLayer.add(line.level, mWayDataContainer,
				line.paint);

		if (mCurLevelContainer1 == null)
			mCurLevelContainer1 = c;
		else if (mCurLevelContainer2 == null)
			mCurLevelContainer2 = c;
	}

	@Override
	public void renderArea(Area area) {
		if (area.paintFill != null)
			mCurLevelContainer1 = mDrawingLayer.add(area.level, mWayDataContainer,
					area.paintFill);
		if (area.paintOutline != null)
			mCurLevelContainer1 = mDrawingLayer.add(area.level, mWayDataContainer,
					area.paintOutline);
	}

	@Override
	public void renderWaySymbol(Bitmap symbolBitmap, boolean alignCenter,
			boolean repeatSymbol) {
		// WayDecorator.renderSymbol(symbolBitmap, alignCenter, repeatSymbol,
		// coordinates,
		// waySymbols);
	}

	@Override
	public void renderWayText(String textKey, Paint paint, Paint outline) {
		// if (mWayDataContainer.textPos[0] >= 0)
		// WayDecorator.renderText(this, paint, outline, mCoords, mWayDataContainer, mWayNames);
	}

	String getWayName() {
		return mMapDatabase.readString(mWayDataContainer.textPos[0]);
	}

	@Override
	public void setMapDatabase(IMapDatabase mapDatabase) {
		mMapDatabase = mapDatabase;
	}

	private void clearLists() {
		for (int i = LAYERS - 1; i >= 0; --i) {
			mWays[i].clear();
		}

		mAreaLabels.clear();
		mNodes.clear();
		mPointSymbols.clear();
		mWayNames.clear();
		mWaySymbols.clear();
	}

	private void createWayLists() {
		int levels = DatabaseRenderer.renderTheme.getLevels();
		for (byte i = LAYERS - 1; i >= 0; --i) {
			mWays[i] = new LayerContainer(levels);
		}
	}

	/**
	 * Converts a latitude value into an Y coordinate on the current tile.
	 * 
	 * @param latitude
	 *            the latitude value.
	 * @return the Y coordinate on the current tile.
	 */
	private static float scaleLatitude(float latitude) {
		double sinLatitude = FloatMath.sin(latitude * PI180);

		return (float) (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / PIx4)
				* mCurrentTileZoom
				- mCurrentTileY;
	}

	/**
	 * Converts a longitude value into an X coordinate on the current tile.
	 * 
	 * @param longitude
	 *            the longitude value.
	 * @return the X coordinate on the current tile.
	 */

	private static float scaleLongitude(float longitude) {
		return (float) ((longitude / 1000000.0 + 180) / 360 * mCurrentTileZoom)
				- mCurrentTileX;
	}

	/**
	 * Sets the scale stroke factor for the given zoom level.
	 * 
	 * @param zoomLevel
	 *            the zoom level for which the scale stroke factor should be set.
	 */
	private static void setScaleStrokeWidth(byte zoomLevel) {
		int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		DatabaseRenderer.renderTheme.scaleStrokeWidth((float) Math.pow(STROKE_INCREASE,
				zoomLevelDiff));
	}

	@Override
	public MapRenderer getMapRenderer(MapView mapView) {
		return new MapRenderer(mapView);
	}
}
