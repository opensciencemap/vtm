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
// 1.1 test if label is actually visible
// 2. compare previous to current state
// 2.1 test for new labels to be placed
// 2.2 handle collisions
// 3 join segments that belong to one feature
// 4 handle zoom-level changes
// 5 R-Tree might be handy
//

import java.util.HashMap;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
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
import org.oscim.view.MapViewPosition;

import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public class TextOverlayExp extends BasicOverlay {
	private final static String TAG = TextOverlayExp.class.getName();

	private final MapViewPosition mMapViewPosition;
	private TileSet mTileSet;
	private final LabelThread mThread;

	private MapPosition mTmpPos;

	// TextLayer that is updating
	private TextLayer mTmpLayer;
	// TextLayer that is ready to be added to 'layers'
	private TextLayer mNextLayer;

	private final float[] mTmpCoords = new float[8];

	/* package */boolean mRun;

	class LabelThread extends PausableThread {

		@Override
		protected void doWork() {
			SystemClock.sleep(150);
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

	private float mSquareRadius;
	private int mRelabelCnt;

	public TextOverlayExp(MapView mapView) {
		super(mapView);
		mMapViewPosition = mapView.getMapViewPosition();

		layers.textureLayers = new TextLayer();
		mTmpLayer = new TextLayer();
		mActiveTiles = new HashMap<MapTile, Link>();
		mTmpPos = new MapPosition();
		mThread = new LabelThread();
		mThread.start();

		mRelabelCnt = 0;
	}

	private HashMap<MapTile, Label> mItemMap;

	class Label extends TextItem {
		TextItem item;

		Link blocking;
		Link blockedBy;

		// shared list of all label for a tile
		Link siblings;

		MapTile tile;

		public byte origin;
		public int active;
		public OBB2D bbox;
	}

	class Link {
		Link next;
		Link prev;
		Label it;
	}

	class ActiveTile{
		MapTile tile;
		int activeLabels;
		Label labels;
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
	private Label mPool;

	private Label mNewLabels;
	private Label mPrevLabels;
	private final HashMap<MapTile, Link> mActiveTiles;

	private byte checkOverlap(TextLayer tl, Label ti) {

		for (Label lp = (Label) tl.labels; lp != null;) {

			// check bounding box
			if (!TextItem.bboxOverlaps(ti, lp, 150)) {
				lp = (Label) lp.next;
				continue;
			}

			if (lp.text == ti.text && (lp.string == ti.string || lp.string.equals(ti.string))) {

				if (lp.active <= ti.active)
					return 1;

				// make strings unique
				ti.string = lp.string;

				if (lp.length < ti.length) {
					Label tmp = lp;
					lp = (Label) lp.next;

					TextItem.release(tmp.item);
					tl.removeText(tmp);

					tmp.next = mPool;
					mPool = tmp;

					continue;
				}

				return 3;
			}

			if (ti.bbox == null) {
				ti.bbox = new OBB2D(ti.x, ti.y, ti.x1, ti.y1,
						ti.width + 5, ti.text.fontHeight + 5);
			}

			if (lp.bbox == null) {
				lp.bbox = new OBB2D(lp.x, lp.y, lp.x1, lp.y1,
						lp.width + 5, lp.text.fontHeight + 5);
			}

			boolean intersect = ti.bbox.overlaps(lp.bbox);

			if (intersect) {

				if (lp.active <= ti.active)
					return 1;

				//Log.d(TAG, "intersection " + lp.string + " <> " + ti.string
				//		+ " at " + ti.x + ":" + ti.y);

				if (!lp.text.caption
						&& (lp.text.priority > ti.text.priority || lp.length < ti.length)) {
					Label tmp = lp;
					lp = (Label) lp.next;

					TextItem.release(tmp.item);
					tl.removeText(tmp);

					tmp.next = mPool;
					mPool = tmp;

					continue;
				}

				return 1;
			}
			lp = (Label) lp.next;
		}
		return 0;
	}

	private boolean nodeIsVisible(TextItem ti) {
		// rough filter
		float dist = ti.x * ti.x + ti.y * ti.y;
		if (dist > mSquareRadius)
			return false;

		return true;
	}

	private boolean wayIsVisible(TextItem ti) {
		// rough filter
		float dist = ti.x * ti.x + ti.y * ti.y;
		if (dist < mSquareRadius)
			return true;

		dist = ti.x1 * ti.x1 + ti.y1 * ti.y1;
		if (dist < mSquareRadius)
			return true;

		dist = ti.x2 * ti.x2 + ti.y2 * ti.y2;
		if (dist < mSquareRadius)
			return true;

		return false;
	}

	private Layers mDebugLayer;
	private final float[] mMVP = new float[16];

	void addTile(MapTile t) {

	}


	boolean updateLabels() {
		if (mTmpLayer == null)
			return false;

		// get current tiles
		mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0)
			return false;

		// reuse text layer
		TextLayer tl = mTmpLayer;
		mTmpLayer = null;

		mNewLabels = null;

		Layers dbg = null; //new Layers();

		float[] coords = mTmpCoords;
		MapPosition pos = mTmpPos;

		synchronized (mMapViewPosition) {
			mMapViewPosition.getMapPosition(pos);
			mMapViewPosition.getMapViewProjection(coords);
			mMapViewPosition.getMatrix(null, null, mMVP);
		}
		int mw = (mMapView.getWidth() + Tile.TILE_SIZE) / 2;
		int mh = (mMapView.getHeight() + Tile.TILE_SIZE) / 2;
		mSquareRadius = mw * mw + mh * mh;

		// mTiles might be from another zoomlevel than the current:
		// this scales MapPosition to the zoomlevel of mTiles...
		// TODO create a helper function in MapPosition
		MapTile[] tiles = mTileSet.tiles;
		int diff = tiles[0].zoomLevel - pos.zoomLevel;
		float div = FastMath.pow(diff);
		float scale = pos.scale * div;

		double angle = Math.toRadians(pos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.TILE_SIZE << (pos.zoomLevel - 1);

		Label l = null;

		if (dbg != null)
			addDebugLayers(dbg);

		mRelabelCnt++;

		for (Label lp = mPrevLabels; lp != null; ) {

			// transform screen coordinates to tile coordinates
			float s = FastMath.pow(lp.tile.zoomLevel - pos.zoomLevel);
			float sscale = pos.scale / s;

			if (lp.width > lp.length * sscale){
				//Log.d(TAG, "- scale " + lp + " " + s + " " + sscale + " " + lp.length + " " + lp.width);
				TextItem.release(lp.item);
				lp = (Label) lp.next;
				continue;
			}

			float dx = (float) (lp.tile.pixelX - pos.x * s);
			float dy = (float) (lp.tile.pixelY - pos.y * s);

			// flip around date-line
			if (dx > maxx)
				dx = dx - maxx * 2;
			else if (dx < -maxx)
				dx = dx + maxx * 2;

			lp.move(lp.item, dx, dy, sscale);

			if (!lp.text.caption) {
				// set line endpoints relative to view to be able to
				// check intersections with label from other tiles
				float width = (lp.x2 - lp.x1) / 2f;
				float height = (lp.y2 - lp.y1) / 2f;

				lp.x2 = (lp.x + width);
				lp.x1 = (lp.x - width);
				lp.y2 = (lp.y + height);
				lp.y1 = (lp.y - height);

				if (!wayIsVisible(lp)){
					//Log.d(TAG, "- visible " + lp);
					TextItem.release(lp.item);
					lp = (Label) lp.next;
					continue;
				}

				byte overlaps = -1;
				if (lp.bbox != null)
				lp.bbox.set(lp.x, lp.y, lp.x1, lp.y1,
						lp.width + 5, lp.text.fontHeight + 5);
				else lp.bbox= new OBB2D(lp.x, lp.y, lp.x1, lp.y1,
						lp.width + 5, lp.text.fontHeight + 5);

				if (dbg != null) {

					LineLayer ll;
					if (overlaps == 1)
						ll = (LineLayer) dbg.getLayer(4, Layer.LINE);
					else
						ll = (LineLayer) dbg.getLayer(5, Layer.LINE);

					{
						float[] points = new float[4];
						short[] indices = { 4 };
						points[0] = (lp.x - width * sscale);
						points[1] = (lp.y - height * sscale);
						points[2] = (lp.x + width * sscale);
						points[3] = (lp.y + height * sscale);
						ll.addLine(points, indices, false);
					}
					if (lp.bbox != null) {
						short[] indices = { 8 };
						ll.addLine(lp.bbox.corner, indices, true);
					}
				}

				overlaps = checkOverlap(tl, lp);

				if (overlaps == 0) {
					//if (s != 1)
					//Log.d(TAG, s + "add prev label " + lp);

					Label tmp = lp;
					lp = (Label) lp.next;

					tmp.next = null;
					tl.addText(tmp);
					continue;
				}
			}

			TextItem.release(lp.item);
			lp = (Label) lp.next;
		}

		/* add way labels */
		for (int i = 0, n = mTileSet.cnt; i < n; i++) {

			MapTile t = tiles[i];

			float dx = (float) (t.pixelX - pos.x);
			float dy = (float) (t.pixelY - pos.y);

			// flip around date-line
			if (dx > maxx)
				dx = dx - maxx * 2;
			else if (dx < -maxx)
				dx = dx + maxx * 2;

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {

				if (ti.text.caption)
					continue;

				// acquire a TextItem to add to TextLayer
				if (l == null) {
					if (mPool == null)
						l = new Label();
					else {
						l = mPool;
						mPool = (Label) mPool.next;
						l.next = null;
					}
					l.active = Integer.MAX_VALUE;
				}

				// check if path at current scale is long enough for text
				if (dbg == null && ti.width > ti.length * scale)
					continue;

				l.clone(ti);
				l.move(ti, dx, dy, scale);

				// set line endpoints relative to view to be able to
				// check intersections with label from other tiles
				float width = (ti.x2 - ti.x1) / 2f;
				float height = (ti.y2 - ti.y1) / 2f;
				l.bbox = null;
				l.x2 = (l.x + width);
				l.x1 = (l.x - width);
				l.y2 = (l.y + height);
				l.y1 = (l.y - height);

				if (!wayIsVisible(l))
					continue;

				byte overlaps = -1;

				if (dbg == null || ti.width < ti.length * scale)
					overlaps = checkOverlap(tl, l);

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
						points[0] = (l.x - width * scale);
						points[1] = (l.y - height * scale);
						points[2] = (l.x + width * scale);
						points[3] = (l.y + height * scale);
						ll.addLine(points, indices, false);
					}
					if (l.bbox != null) {
						short[] indices = { 8 };
						ll.addLine(l.bbox.corner, indices, true);
					}
				}
				if (overlaps == 0) {
					tl.addText(l);
					l.item = TextItem.copy(ti);
					l.tile = t;
					l.active = mRelabelCnt;
					l = null;
				}
			}
		}

		for (int i = 0, n = mTileSet.cnt; i < n; i++) {

			MapTile t = tiles[i];

			float dx = (float) (t.pixelX - pos.x);
			float dy = (float) (t.pixelY - pos.y);

			// flip around date-line
			if (dx > maxx)
				dx = dx - maxx * 2;
			 else if (dx < -maxx)
				dx = dx + maxx * 2;

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {
				if (!ti.text.caption)
					continue;

				// acquire a TextItem to add to TextLayer
				if (l == null) {
					if (mPool == null)
						l = new Label();
					else {
						l = mPool;
						mPool = (Label) mPool.next;
						l.next = null;
					}
					l.active = Integer.MAX_VALUE;
				}

				l.clone(ti);
				l.move(ti, dx, dy, scale);
				if (!nodeIsVisible(l))
					continue;

				l.setAxisAlignedBBox();

				l.bbox = new OBB2D(l.x, l.y, cos, -sin, l.width + 6,
						l.text.fontHeight + 6, true);

				boolean overlaps = false;
				for (Label lp = (Label) tl.labels; lp != null; lp = (Label) lp.next) {
					//if (!lp.text.caption)
					//	continue;
					if (lp.bbox == null) {
						lp.bbox = new OBB2D(lp.x, lp.y, lp.x1, lp.y1,
								lp.width + 5, lp.text.fontHeight + 5);
					}

					if (l.bbox.overlaps(lp.bbox)) {
						//Log.d(TAG, "overlap > " + ti2.string + " " + lp.string);
						//if (TextItem.bboxOverlaps(ti2, lp, 4)) {
						overlaps = true;
						break;
					}
				}
				if (!overlaps) {
					tl.addText(l);
					l.item = TextItem.copy(ti);
					l.tile = t;
					l.active = mRelabelCnt;
					l = null;
				}
			}
		}

		for (Label ti = (Label)tl.labels; ti != null; ti = (Label)ti.next) {

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
		if (l != null) {
			l.next = mPool;
			mPool = l;
		}

		// draw text to bitmaps and create vertices
		tl.prepare();

		// after 'prepare' TextLayer does not need TextItems any longer
		mPrevLabels = (Label) tl.labels;
		tl.labels = null;

		// remove tile locks
		GLRenderer.releaseTiles(mTileSet);

		// pass new labels for rendering
		synchronized (this) {
			mNextLayer = tl;
			mDebugLayer = dbg;
		}

		return true;
	}

	private static void addDebugLayers(Layers dbg){
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
		ll = (LineLayer) dbg.getLayer(4, Layer.LINE);
		ll.line = new Line((Color.CYAN & 0xaaffffff), 1, Cap.BUTT);
		ll.width = 2;
		ll = (LineLayer) dbg.getLayer(5, Layer.LINE);
		ll.line = new Line((Color.MAGENTA & 0xaaffffff), 1, Cap.BUTT);
		ll.width = 2;
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

		if (newSize == 0) {
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
	public synchronized void render(MapPosition pos, Matrices m) {
		float div = FastMath.pow(mMapPosition.zoomLevel - pos.zoomLevel);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);
		GLState.test(false, false);

		if (layers.layers != null) {
			setMatrix(pos, m, true);

			//Matrix.multiplyMM(m.mvp, 0, m.proj, 0, m.mvp,0);
			for (Layer l = layers.layers; l != null;) {
				if (l.type == Layer.POLYGON) {
					l = PolygonRenderer.draw(pos, l, m.mvp, true, false);
				} else {
					float scale = pos.scale * div;
					l = LineRenderer.draw(pos, l, m.mvp, scale, 0, layers.lineOffset);
				}
			}
		}

		setMatrix(pos, m);
		for (Layer l = layers.textureLayers; l != null;) {
			float scale = (mMapPosition.scale / pos.scale) * div;

			l = TextureRenderer.draw(l, scale, m.proj, m.mvp);
		}

	}

	@Override
	protected void setMatrix(MapPosition curPos, Matrices m) {
		MapPosition oPos = mMapPosition;

		float div = FastMath.pow(oPos.zoomLevel - curPos.zoomLevel);
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		float scale = (curPos.scale / mMapPosition.scale) / div;
		float s = curPos.scale / div;
		GlUtils.setMatrix(m.mvp, x * s, y * s,
				scale / GLRenderer.COORD_MULTIPLIER);

		Matrix.multiplyMM(m.mvp, 0, m.view, 0, m.mvp, 0);
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
