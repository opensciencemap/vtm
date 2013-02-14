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

package org.oscim.renderer.overlays;

// TODO
// 1. rewrite :)
// 2. compare previous to current state
// 2.1 test for new labels to be placed
// 2.2 handle collisions
// 3 join segments that belong to one feature
// 4 handle zoom-level changes
// 5 3D-Tree might be handy
//

import java.util.HashMap;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.generator.JobTile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.LineRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.PolygonRenderer;
import org.oscim.renderer.TextureRenderer;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.renderer.layer.TextLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.utils.OBB2D;
import org.oscim.utils.PausableThread;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

public class TextOverlayExp extends BasicOverlay {
	private final static String TAG = TextOverlayExp.class.getName();

	private TileSet mTileSet;
	private final LabelThread mThread;

	private MapPosition mTmpPos;

	// TextLayer that is updating
	private TextLayer mTmpLayer;
	// TextLayer that is ready to be added to 'layers'
	private TextLayer mNextLayer;

	/* package */boolean mRun;

	class LabelThread extends PausableThread {

		@Override
		protected void doWork() {
			SystemClock.sleep(300);
			if (!mRun)
				return;

			mRun = false;

			if (updateLabels()) {
				mMapView.redrawMap(true);
			} else {
				mRun = true;
			}
		}

		@Override
		protected String getThreadName() {
			return "Labeling";
		}

		@Override
		protected boolean hasWork() {
			return mRun;
		}
	}

	public TextOverlayExp(MapView mapView) {
		super(mapView);

		layers.textureLayers = new TextLayer();
		mTmpLayer = new TextLayer();

		mTmpPos = new MapPosition();
		mThread = new LabelThread();
		mThread.start();
	}

	private HashMap<MapTile, PlacementItem> mItemMap;

	class PlacementItem extends TextItem {
		int tileX;
		int tileY;

		boolean isTileNeighbour(PlacementItem other) {
			int dx = other.tileX - tileX;
			if (dx > 1 || dx < -1)
				return false;

			int dy = other.tileY - tileY;
			if (dy > 1 || dy < -1)
				return false;

			return true;
		}
	}

	//private static void setOBB(TextItem ti){
	//	if (ti.bbox == null)
	//	ti.bbox = new OBB2D(ti.x, ti.y, ti.x1, ti.y1, ti.width + 10,
	//			ti.text.fontHeight + 10);
	//	else
	//		ti.bbox.set(ti.x, ti.y, ti.x1, ti.y1, ti.width + 10,
	//			ti.text.fontHeight + 10);
	//}

	// local pool, avoids synchronized TextItem.get()/release()
	private TextItem mPool;

	private byte checkOverlap(TextLayer tl, TextItem ti) {

		for (TextItem lp = tl.labels; lp != null;) {
			if (lp.text.caption) {
				lp = lp.next;
				continue;
			}

			// check bounding box
			if (!TextItem.bboxOverlaps(ti, lp, 100)) {
				lp = lp.next;
				continue;
			}

			if (lp.text == ti.text && (lp.string == ti.string || lp.string.equals(ti.string))) {
				// make strings unique
				ti.string = lp.string;

				//p.active < ti.active ||

				//Log.d(TAG, "overlap, same label in bbox " + lp.string
				//		+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":"
				//+ lp.y + " " + ti.length + "/" + lp.length);

				if (lp.length < ti.length) {

					//if (lp.length > ti.length) {
					//Log.d(TAG, "drop " + lp.string);
					TextItem tmp = lp;
					lp = lp.next;

					tl.removeText(tmp);

					tmp.next = mPool;
					mPool = tmp;

					continue;
				}

				return 3;
			}

			if (ti.bbox == null) {
				ti.bbox = new OBB2D(ti.x, ti.y, ti.x1, ti.y1,
						ti.width + 10, ti.text.fontHeight + 10);
			}
			if (lp.bbox == null) {
				lp.bbox = new OBB2D(lp.x, lp.y, lp.x1, lp.y1,
						lp.width + 10, lp.text.fontHeight + 10);
			}

			boolean intersect = ti.bbox.overlaps(lp.bbox);

			//	byte intersect = GeometryUtils.linesIntersect(
			//		ti.x1, ti.y1, ti.x2, ti.y2,
			//		lp.x1, lp.y1, lp.x2, lp.y2);

			if (intersect) {
				//Log.d(TAG, "intersection " + lp.string + " <> " + ti.string
				//		+ " at " + ti.x + ":" + ti.y);

				if (lp.text.priority > ti.text.priority || lp.length < ti.length) {
					//if (lp.length > ti.length) {
					//Log.d(TAG, "drop " + lp.string);
					TextItem tmp = lp;
					lp = lp.next;

					tl.removeText(tmp);

					tmp.next = mPool;
					mPool = tmp;

					continue;
				}

				return 1;

				//	if ((lp.n1 != null && lp.n1 == ti.n2) ||
				//			(lp.n2 != null && lp.n2 == ti.n1)) {
				//		Log.d(TAG, "overlap with adjacent label " + lp.string
				//				+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":" + lp.y);
				//
				//		return intersect;
				//	}
				//
				//	if ((ti.n1 != null || ti.n2 != null) && (lp.n1 == null && lp.n2 == null)) {
				//		Log.d(TAG, "overlap, other is unique " + lp.string + " " + ti.string
				//				+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":" + lp.y);
				//		return intersect;
				//	}
				//
				//	return intersect;
			}

			lp = lp.next;
		}
		return 0;
	}

	private Layers mDebugLayer;

	boolean updateLabels() {
		if (mTmpLayer == null)
			return false;

		mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0)
			return false;

		TextLayer tl = mTmpLayer;
		mTmpLayer = null;

		Layers dbg = null;//new Layers();

		// mTiles might be from another zoomlevel than the current:
		// this scales MapPosition to the zoomlevel of mTiles...
		// TODO create a helper function in MapPosition
		mMapView.getMapViewPosition().getMapPosition(mTmpPos, null);
		// capture current state

		MapTile[] tiles = mTileSet.tiles;

		int diff = tiles[0].zoomLevel - mTmpPos.zoomLevel;

		float div = FastMath.pow(diff);

		float scale = mTmpPos.scale * div;
		double angle = Math.toRadians(mTmpPos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.TILE_SIZE << (mTmpPos.zoomLevel - 1);

		TextItem ti2 = null;

		if (dbg != null) {
			dbg.clear();
			LineLayer ll = (LineLayer) dbg.getLayer(0, Layer.LINE);
			ll.line = new Line((Color.BLUE & 0xaaffffff), 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) dbg.getLayer(3, Layer.LINE);
			ll.line = new Line((Color.YELLOW & 0xaaffffff), 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) dbg.getLayer(1, Layer.LINE);
			ll.line = new Line((Color.RED & 0xaaffffff), 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) dbg.getLayer(2, Layer.LINE);
			ll.line = new Line((Color.GREEN & 0xaaffffff), 1, Cap.BUTT);
			ll.width = 2;
		}

		for (int i = 0, n = mTileSet.cnt; i < n; i++) {

			MapTile t = tiles[i];

			if (t.state == JobTile.STATE_NONE || t.state == JobTile.STATE_LOADING)
				continue;

			//if (t.joined != MapTile.JOINED)
			//	joinTile()

			float dx = (float) (t.pixelX - mTmpPos.x);
			float dy = (float) (t.pixelY - mTmpPos.y);

			// flip around date-line
			if (dx > maxx) {
				dx = dx - maxx * 2;
			} else if (dx < -maxx) {
				dx = dx + maxx * 2;
			}
			dx *= scale;
			dy *= scale;

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {

				// acquire a TextItem to add to TextLayer
				if (ti2 == null) {
					if (mPool == null)
						ti2 = TextItem.get();
					else {
						ti2 = mPool;
						mPool = mPool.next;
						ti2.next = null;
					}
				}

				if (ti.text.caption) {
					ti2.move(ti, dx, dy, scale);
					ti2.setAxisAlignedBBox();
					ti2.bbox = new OBB2D(ti2.x, ti2.y, cos, -sin, ti2.width + 6,
							ti2.text.fontHeight + 6, true);

					boolean overlaps = false;
					for (TextItem lp = tl.labels; lp != null; lp = lp.next) {
						if (!lp.text.caption)
							continue;

						if (ti2.bbox.overlaps(lp.bbox)) {
							Log.d(TAG, "overlap > " + ti2.string + " " + lp.string);
							//if (TextItem.bboxOverlaps(ti2, lp, 4)) {
							overlaps = true;
							break;
						}
					}
					if (!overlaps) {
						tl.addText(ti2);
						ti2 = null;
					}

					continue;
				}

				/* text is way label */

				// check if path at current scale is long enough for text
				if (dbg == null && ti.width > ti.length * scale)
					continue;

				// set line endpoints relative to view to be able to
				// check intersections with label from other tiles
				float width = (ti.x2 - ti.x1) / 2f;
				float height = (ti.y2 - ti.y1) / 2f;
				ti2.bbox = null;

				ti2.move(ti, dx, dy, scale);
				ti2.x2 = (ti2.x + width);
				ti2.x1 = (ti2.x - width);
				ti2.y2 = (ti2.y + height);
				ti2.y1 = (ti2.y - height);

				byte overlaps = -1;

				if (dbg == null || ti.width < ti.length * scale)
					overlaps = checkOverlap(tl, ti2);

				if (dbg != null) {

					LineLayer ll;
					if (ti.width > ti.length * scale) {
						ll = (LineLayer) dbg.getLayer(1, Layer.LINE);
						overlaps = 3;
					}
					else if (overlaps == 1)
						ll = (LineLayer) dbg.getLayer(0, Layer.LINE);
					else if (overlaps == 2)
						ll = (LineLayer) dbg.getLayer(3, Layer.LINE);
					else
						ll = (LineLayer) dbg.getLayer(2, Layer.LINE);

					//if (ti2.bbox == null)
					//	ti2.bbox = new OBB2D(ti2.x, ti2.y, ti2.x1, ti2.y1, ti.width + 10,
					//			ti.text.fontHeight + 10);

					{
						float[] points = new float[4];
						short[] indices = { 4 };
						points[0] = (ti2.x - width * scale) / scale;
						points[1] = (ti2.y - height * scale) / scale;
						points[2] = (ti2.x + width * scale) / scale;
						points[3] = (ti2.y + height * scale) / scale;
						ll.addLine(points, indices, false);
					}
					if (ti2.bbox != null) {
						short[] indices = { 8 };
						float[] points = new float[8];
						for (int p = 0; p < 8; p++)
							points[p] = ti2.bbox.corner[p] / scale;

						ll.addLine(points, indices, true);
					}
				}
				if (overlaps == 0) {
					ti.active++;

					tl.addText(ti2);
					ti2.active = ti.active;
					ti2 = null;
				}
			}
		}
		for (TextItem ti = tl.labels; ti != null; ti = ti.next) {
			// scale back to fixed zoom-level. could be done in setMatrix
			ti.x /= scale;
			ti.y /= scale;

			if (ti.text.caption)
				continue;

			// flip label upside-down
			if (cos * (ti.x2 - ti.x1) - sin * (ti.y2 - ti.y1) < 0) {
				float tmp = ti.x1;
				ti.x1 = ti.x2;
				ti.x2 = tmp;

				tmp = ti.y1;
				ti.y1 = ti.y2;
				ti.y2 = tmp;
			}
		}

		// release temporarily used TextItems
		if (ti2 != null) {
			ti2.next = mPool;
			mPool = ti2;
		}
		if (mPool != null) {
			TextItem.release(mPool);
			mPool = null;
		}

		// draw text to bitmaps and create vertices
		tl.setScale(scale);
		tl.prepare();

		//TextItem.printPool();
		//Log.d(TAG, "new labels: " + count);

		GLRenderer.releaseTiles(mTileSet);

		// pass new labels for rendering
		synchronized (this) {
			mNextLayer = tl;
			mDebugLayer = dbg;
		}
		return true;
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		if (mNextLayer != null) {
			// keep text layer, not recrating its canvas each time
			mTmpLayer = (TextLayer) layers.textureLayers;

			// clear textures and text items from previous layer
			layers.clear();

			if (mDebugLayer != null) {
				layers.layers = mDebugLayer.layers;
				mDebugLayer = null;
			}

			// set new TextLayer to be uploaded and rendered
			layers.textureLayers = mNextLayer;

			// make the 'labeled' MapPosition current
			MapPosition tmp = mMapPosition;
			mMapPosition = mTmpPos;
			mTmpPos = tmp;

			newData = true;
			mNextLayer = null;
			if (!(positionChanged || tilesChanged))
				return;
		}

		if (mHolding)
			return;

		if (!mRun) {
			mRun = true;
			synchronized (mThread) {
				mThread.notify();
			}
		}
	}

	@Override
	public void compile() {
		int newSize = layers.getSize();
		//if (newSize == 0)
		//	Log.d(TAG, "text layer size " + newSize);

		if (newSize == 0) {
			//BufferObject.release(vbo);
			//vbo = null;
			isReady = false;
			return;
		}

		if (vbo == null) {
			vbo = BufferObject.get(0);
		}

		if (newSize > 0) {
			if (GLRenderer.uploadLayers(layers, vbo, newSize, true))
				isReady = true;
		}
	}

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {
		setMatrix(pos, mv);
		float div = FastMath.pow(mMapPosition.zoomLevel - pos.zoomLevel);

		Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);
		GLState.test(false, false);

		for (Layer l = layers.layers; l != null;) {
			if (l.type == Layer.POLYGON) {
				l = PolygonRenderer.draw(pos, l, mvp, true, false);
			} else {
				l = LineRenderer.draw(pos, l, mvp, div, 0, layers.lineOffset);
			}
		}

		for (Layer l = layers.textureLayers; l != null;) {
			l = TextureRenderer.draw(l, (mMapPosition.scale / pos.scale) * div, proj, mv);
		}

	}

	@Override
	protected void setMatrix(MapPosition curPos, float[] matrix) {
		MapPosition oPos = mMapPosition;

		float div = FastMath.pow(oPos.zoomLevel - curPos.zoomLevel);
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		float scale = curPos.scale / div;

		GlUtils.setMatrix(matrix, x * scale, y * scale,
				scale / GLRenderer.COORD_MULTIPLIER);

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);
	}

	private boolean mHolding;

	/**
	 * @param enable layer updates
	 */
	public synchronized void hold(boolean enable) {
		//		mHolding = enable;
		//		if (!enable && !mRun) {
		//			mRun = true;
		//			synchronized (mThread) {
		//				mThread.notify();
		//			}
		//		}
		//		} else {
		//			mRun = false;
		//		}
	}
}
