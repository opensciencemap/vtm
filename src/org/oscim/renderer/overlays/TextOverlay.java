/*
 * Copyright 2013 OpenScienceMap
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

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.renderer.layer.TextLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.GeometryUtils;
import org.oscim.utils.GlUtils;
import org.oscim.utils.PausableThread;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class TextOverlay extends BasicOverlay {
	private final static String TAG = TextOverlay.class.getName();

	private TileSet mTiles;
	private LabelThread mThread;

	private MapPosition mWorkPos;

	// TextLayer that is updating
	private TextLayer mWorkLayer;
	// TextLayer that is ready to be added to 'layers'
	private TextLayer mCurLayer;

	/* package */boolean mRun;
	/* package */boolean mRerun;

	class LabelThread extends PausableThread {

		@Override
		protected void doWork() {
			SystemClock.sleep(400);
			if (!mRun)
				return;

			mRun = false;
			updateLabels();
			mMapView.redrawMap(false);
		}

		@Override
		protected String getThreadName() {
			return "Labeling";
		}

		@Override
		protected boolean hasWork() {
			return mRun || mRerun;
		}
	}

	public TextOverlay(MapView mapView) {
		super(mapView);

		mWorkPos = new MapPosition();
		mThread = new LabelThread();
		mThread.start();
	}

	private TextItem mPool;

	private byte checkOverlap(TextLayer tl, TextItem ti) {

		for (TextItem lp = tl.labels; lp != null;) {
			if (lp.text.caption) {
				lp = lp.next;
				continue;
			}

			// check bounding box
			if (!TextItem.bboxOverlaps(ti, lp, 80)) {
				lp = lp.next;
				continue;
			}

			if (lp.text == ti.text && (lp.string == ti.string || lp.string.equals(ti.string))) {
				// make strings unique
				ti.string = lp.string;

				Log.d(TAG, "overlap, same label in bbox " + lp.string
						+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":" + lp.y);
				return 3;
			}

			if (!TextItem.bboxOverlaps(ti, lp, 10)) {
				lp = lp.next;
				continue;
			}

			byte intersect = GeometryUtils.linesIntersect(
					ti.x1, ti.y1, ti.x2, ti.y2,
					lp.x1, lp.y1, lp.x2, lp.y2);

			if (intersect != 0) {
				//Log.d(TAG, "overlap " + lp.string + " <> " + ti.string
				//+ " at " + ti.x + ":" + ti.y);

				if ((lp.n1 != null && lp.n1 == ti.n2) ||
						(lp.n2 != null && lp.n2 == ti.n1)) {
					//Log.d(TAG, "overlap with adjacent label " + lp.string
					//		+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":" + lp.y);

					return intersect;
				}

				if ((ti.n1 != null || ti.n2 != null) && (lp.n1 == null && lp.n2 == null)) {
					Log.d(TAG, "overlap, other is unique " + lp.string + " " + ti.string
							+ " at " + ti.x + ":" + ti.y + ", " + lp.x + ":" + lp.y);
					return intersect;
				}
				// just to make it more deterministic
				if (lp.x > ti.x) {
					//Log.d(TAG, "drop " + lp.string);

					TextItem tmp = lp;
					lp = lp.next;

					tl.removeText(tmp);

					tmp.next = mPool;
					mPool = tmp;
					continue;
				}
				return intersect;
			}

			lp = lp.next;
		}
		return 0;
	}

	private final Layers mDebugLayer = null; //new Layers();

	void updateLabels() {
		mTiles = mMapView.getTileManager().getActiveTiles(mTiles);

		// Log.d("...", "relabel " + mRerun + " " + x + " " + y);
		if (mTiles.cnt == 0)
			return;

		mMapView.getMapViewPosition().getMapPosition(mWorkPos, null);

		TextLayer tl = mWorkLayer;

		if (tl == null)
			tl = new TextLayer();

		// mTiles might be from another zoomlevel than the current:
		// this scales MapPosition to the zoomlevel of mTiles...
		// TODO create a helper function in MapPosition
		int diff = mTiles.tiles[0].zoomLevel - mWorkPos.zoomLevel;

		// only relabel when tiles belong to the current zoomlevel or its parent
		if (diff > 1 || diff < -2) {
			// pass back the current layer
			synchronized (this) {
				Log.d(TAG, "drop labels: diff " + diff);
				mCurLayer = tl;
			}
			return;
		}

		float scale = mWorkPos.scale;
		double angle = Math.toRadians(mWorkPos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.TILE_SIZE << (mWorkPos.zoomLevel - 1);

		MapTile[] tiles = mTiles.tiles;
		TextItem ti2 = null;

		if (mDebugLayer != null) {
			mDebugLayer.clear();
			LineLayer ll = (LineLayer) mDebugLayer.getLayer(0, Layer.LINE);
			ll.line = new Line(Color.BLUE, 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) mDebugLayer.getLayer(3, Layer.LINE);
			ll.line = new Line(Color.YELLOW, 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) mDebugLayer.getLayer(1, Layer.LINE);
			ll.line = new Line(Color.RED, 1, Cap.BUTT);
			ll.width = 2;
			ll = (LineLayer) mDebugLayer.getLayer(2, Layer.LINE);
			ll.line = new Line(Color.GREEN, 1, Cap.BUTT);
			ll.width = 2;
		}
		// TODO more sophisticated placement :)
		for (int i = 0, n = mTiles.cnt; i < n; i++) {
			MapTile t = tiles[i];
			if (!t.isVisible)
				continue;

			//	Log.d(TAG, "add: " + t);

			float dx = (float) (t.pixelX - mWorkPos.x);
			float dy = (float) (t.pixelY - mWorkPos.y);

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
					int tx = (int) (ti2.x);
					int ty = (int) (ti2.y);
					int tw = (int) (ti2.width / 2);
					int th = (int) (ti2.text.fontHeight / 2);

					boolean overlaps = false;
					for (TextItem lp = tl.labels; lp != null;) {
						int px = (int) (lp.x);
						int py = (int) (lp.y);
						int ph = (int) (lp.text.fontHeight / 2);
						int pw = (int) (lp.width / 2);

						if ((tx - tw) < (px + pw)
								&& (px - pw) < (tx + tw)
								&& (ty - th) < (py + ph)
								&& (py - ph) < (ty + th)) {

							overlaps = true;
							break;
						}
						lp = lp.next;
					}
					if (!overlaps) {
						tl.addText(ti2);
						ti2 = null;
					}

					continue;
				}

				/* text is way label */

				// check if path at current scale is long enough for text
				if (mDebugLayer == null && ti.width > ti.length * scale)
					continue;

				// set line endpoints relative to view to be able to
				// check intersections with label from other tiles
				float width = (ti.x2 - ti.x1) / 2f;
				float height = (ti.y2 - ti.y1) / 2f;

				ti2.move(ti, dx, dy, scale);
				ti2.x2 = (ti2.x + width);
				ti2.x1 = (ti2.x - width);
				ti2.y2 = (ti2.y + height);
				ti2.y1 = (ti2.y - height);

				byte overlaps = checkOverlap(tl, ti2);

				if (mDebugLayer != null) {

					LineLayer ll;
					if (ti.width > ti.length * scale) {
						ll = (LineLayer) mDebugLayer.getLayer(1, Layer.LINE);
						overlaps = 3;
					}
					else if (overlaps == 1)
						ll = (LineLayer) mDebugLayer.getLayer(0, Layer.LINE);
					else if (overlaps == 2)
						ll = (LineLayer) mDebugLayer.getLayer(3, Layer.LINE);
					else
						ll = (LineLayer) mDebugLayer.getLayer(2, Layer.LINE);

					float[] points = new float[4];
					short[] indices = { 4 };
					points[0] = ti2.x1 / scale;
					points[1] = ti2.y1 / scale;
					points[2] = ti2.x2 / scale;
					points[3] = ti2.y2 / scale;
					ll.addLine(points, indices, false);
				}

				if (overlaps == 0) {
					tl.addText(ti2);
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

		// everything synchronized?
		synchronized (this) {
			mCurLayer = tl;
		}
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {
		// Log.d("...", "update " + tilesChanged + " " + positionChanged);
		if (mHolding)
			return;

		if (mCurLayer != null) {
			// keep text layer, not recrating its canvas each time
			mWorkLayer = (TextLayer) layers.textureLayers;

			// clear textures and text items from previous layer
			layers.clear();

			if (mDebugLayer != null) {
				layers.layers = mDebugLayer.layers;
				mDebugLayer.layers = null;
			}

			// set new TextLayer to be uploaded and used
			layers.textureLayers = mCurLayer;

			mCurLayer = null;

			// make the 'labeled' MapPosition current
			MapPosition tmp = mMapPosition;
			mMapPosition = mWorkPos;
			mWorkPos = tmp;

			// TODO should return true instead
			newData = true;
		}

		if (tilesChanged || positionChanged) {
			if (!mRun) {
				mRun = true;
				synchronized (mThread) {
					mThread.notify();
				}
			}
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

	public synchronized void hold(boolean enable) {
		//		mHolding = enable;
		//		if (!enable && !mRun) {
		//			mRun = true;
		//			synchronized (mThread) {
		//				mThread.notify();
		//			}
		//		} else {
		//			mRun = false;
		//		}
	}
}
