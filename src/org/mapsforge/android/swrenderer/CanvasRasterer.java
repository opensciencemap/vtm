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

import java.util.List;

import org.mapsforge.core.Tile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * A CanvasRasterer uses a Canvas for drawing.
 * 
 * @see <a href="http://developer.android.com/reference/android/graphics/Canvas.html">Canvas</a>
 */
class CanvasRasterer {
	private static final Paint PAINT_BITMAP_FILTER = new Paint(Paint.FILTER_BITMAP_FLAG);
	private static final Paint PAINT_TILE_COORDINATES = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_COORDINATES_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TILE_FRAME = new Paint();

	private static final Paint PAINT_MARK = new Paint();
	static final int COLOR_MARK = Color.argb(30, 0, 255, 0);

	// private static final float[] TILE_FRAME = new float[] { 0, 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE,
	// Tile.TILE_SIZE,
	// Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };

	private static void configurePaints() {
		PAINT_TILE_COORDINATES.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES.setTextSize(12);

		PAINT_TILE_COORDINATES_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_TILE_COORDINATES_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_TILE_COORDINATES_STROKE.setStrokeWidth(1);
		PAINT_TILE_COORDINATES_STROKE.setTextSize(6);
		PAINT_TILE_COORDINATES_STROKE.setColor(Color.WHITE);
		PAINT_MARK.setColor(COLOR_MARK);

	}

	private final Canvas mCanvas;
	private final Path mPath;
	private final Matrix mSymbolMatrix;

	private float mScaleFactor;

	CanvasRasterer() {
		mCanvas = new Canvas();
		mSymbolMatrix = new Matrix();
		mPath = new Path();
		mPath.setFillType(Path.FillType.EVEN_ODD);
		mScaleFactor = 1;
		configurePaints();

	}

	private void drawTileCoordinate(String string, int offsetY) {
		mCanvas.drawText(string, 20, offsetY, PAINT_TILE_COORDINATES);
	}

	void drawNodes(List<PointTextContainer> pointTextContainers) {

		for (int index = pointTextContainers.size() - 1; index >= 0; --index) {
			PointTextContainer pointTextContainer = pointTextContainers.get(index);

			if (pointTextContainer.paintBack != null) {

				mCanvas.drawText(pointTextContainer.text, pointTextContainer.x * mScaleFactor, pointTextContainer.y
						* mScaleFactor, pointTextContainer.paintBack);
			}

			mCanvas.drawText(pointTextContainer.text, pointTextContainer.x * mScaleFactor, pointTextContainer.y
					* mScaleFactor, pointTextContainer.paintFront);
		}
	}

	void drawSymbols(List<SymbolContainer> symbolContainers) {
		for (int index = symbolContainers.size() - 1; index >= 0; --index) {
			SymbolContainer symbolContainer = symbolContainers.get(index);

			if (symbolContainer.alignCenter) {
				int pivotX = symbolContainer.symbol.getWidth() >> 1;
				int pivotY = symbolContainer.symbol.getHeight() >> 1;
				mSymbolMatrix.setRotate(symbolContainer.rotation, pivotX, pivotY);
				mSymbolMatrix.postTranslate(symbolContainer.x - pivotX, symbolContainer.y - pivotY);
			} else {
				mSymbolMatrix.setRotate(symbolContainer.rotation);
				mSymbolMatrix.postTranslate(symbolContainer.x, symbolContainer.y);
			}
			mSymbolMatrix.postTranslate(mScaleFactor, mScaleFactor);

			// symbolMatrix.postScale(zoomFactor, zoomFactor);
			mCanvas.drawBitmap(symbolContainer.symbol, mSymbolMatrix, PAINT_BITMAP_FILTER);
		}
	}

	void drawTileCoordinates(Tile tile, long time_load, long time_draw, long blub, long blah) {

		drawTileCoordinate(tile.tileX + " / " + tile.tileY + " / " + tile.zoomLevel + " " + mScaleFactor, 20);

		drawTileCoordinate("l:" + time_load, 40);
		drawTileCoordinate("d:" + time_draw, 60);
		drawTileCoordinate("+:" + blub, 80);
		drawTileCoordinate("-:" + blah, 100);

	}

	void drawTileFrame() {
		float size = (Tile.TILE_SIZE * mScaleFactor);
		float[] frame = new float[] { 0, 0, 0, size - 1, 0, size - 1, size - 1, size - 1, size - 1, size - 1, size - 1,
				0 };
		mCanvas.drawLines(frame, PAINT_TILE_FRAME);
	}

	void drawWayNames(float[] coords, List<WayTextContainer> wayTextContainers) {

		for (int index = wayTextContainers.size() - 1; index >= 0; --index) {
			WayTextContainer wayTextContainer = wayTextContainers.get(index);
			mPath.rewind();

			int first = wayTextContainer.first;
			int last = wayTextContainer.last;

			// int len = wayTextContainer.wayDataContainer.length[0];
			// int pos = wayTextContainer.wayDataContainer.position[0];

			// System.arraycopy(floats, pos, coords, 0, len);

			if (coords[first] < coords[last]) {
				mPath.moveTo(coords[first], coords[first + 1]);

				for (int i = first + 2; i <= last; i += 2) {
					mPath.lineTo(coords[i], coords[i + 1]);
				}
			} else {
				mPath.moveTo(coords[last], coords[last + 1]);

				for (int i = last - 2; i >= first; i -= 2) {
					mPath.lineTo(coords[i], coords[i + 1]);
				}
			}
			mCanvas.drawTextOnPath(wayTextContainer.text, mPath, 0, 3, wayTextContainer.paint);

			// if (wayTextContainer.match)
			// canvas.drawRect(wayTextContainer.x1,
			// wayTextContainer.top, wayTextContainer.x2,
			// wayTextContainer.bot, PAINT_MARK);
		}
	}

	void drawWays(float[] coords, LayerContainer[] drawWays) {
		int levels = drawWays[0].mLevelActive.length;

		for (LayerContainer layerContainer : drawWays) {
			if (!layerContainer.mActive)
				continue;

			for (int level = 0; level < levels; level++) {

				if (!layerContainer.mLevelActive[level])
					continue;

				// mPath.rewind();

				LevelContainer levelContainer = layerContainer.mLevels[level];

				for (int way = levelContainer.mShapeContainers.size() - 1; way >= 0; way--) {
					mPath.rewind();
					// switch (shapePaintContainer.shapeContainer.getShapeType()) {
					//
					// case WAY:
					WayDataContainer wayDataContainer = (WayDataContainer) levelContainer.mShapeContainers.get(way);
					// (WayDataContainer) shapePaintContainer.shapeContainer;

					// if (wayDataContainer.closed) {
					for (int i = 0, n = wayDataContainer.length.length; i < n; i++) {

						int len = wayDataContainer.length[i];
						int pos = wayDataContainer.position[i];
						if (len > 2) {
							mPath.moveTo(coords[pos], coords[pos + 1]);

							for (int j = pos + 2; j < len + pos; j += 2)
								mPath.lineTo(coords[j], coords[j + 1]);
						}
					}
					mCanvas.drawPath(mPath, levelContainer.mPaint[0]);
					if (levelContainer.mPaint[1] != null)
						mCanvas.drawPath(mPath, levelContainer.mPaint[1]);

					// }else {
					// for (int i = 0, n = wayDataContainer.length.length; i < n; i++) {
					// // levelContainer.mPaint[0].setStrokeJoin(Join.ROUND);
					//
					// int len = wayDataContainer.length[i];
					// int pos = wayDataContainer.position[i];
					// if (len > 2) {
					// mCanvas.drawPoints(coords, pos, len, levelContainer.mPaint[0]);
					// if (levelContainer.mPaint[1] != null)
					// mCanvas.drawPoints(coords, pos, len, levelContainer.mPaint[1]);
					// }
					//
					// }
					// }
					// break;

					// case CIRCLE:
					// CircleContainer circleContainer =
					// (CircleContainer) shapePaintContainer.shapeContainer;
					//
					// mPath.rewind();
					//
					// mPath.addCircle(circleContainer.mX, circleContainer.mY,
					// circleContainer.mRadius, Path.Direction.CCW);
					//
					// mCanvas.drawPath(mPath, shapePaintContainer.paint);
					// break;
					// }

				}
			}
		}
	}

	void fill(int color) {
		mCanvas.drawColor(color);
	}

	void setCanvasBitmap(Bitmap bitmap, float scale) {
		mCanvas.setBitmap(bitmap);
		// add some extra pixels to avoid < 1px blank edges while scaling
		mCanvas.clipRect(0, 0, Tile.TILE_SIZE * scale + 2, Tile.TILE_SIZE * scale + 2);
		mScaleFactor = scale;
	}
}
