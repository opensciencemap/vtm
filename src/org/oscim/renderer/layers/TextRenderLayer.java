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

package org.oscim.renderer.layers;

// TODO
// 1. rewrite :)
// 1.1 test if label is actually visible
// 2. compare previous to current state
// 2.1 test for new labels to be placed
// 2.2 handle collisions
// 2.3 try to place labels along a way
// 2.4 use 4 point labeling
// 3 join segments that belong to one feature
// 4 handle zoom-level changes
// 5 R-Tree might be handy
//

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.graphics.Color;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderLayer;
import org.oscim.layers.tile.TileSet;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.sublayers.Layer;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.renderer.sublayers.LineLayer;
import org.oscim.renderer.sublayers.LineRenderer;
import org.oscim.renderer.sublayers.PolygonRenderer;
import org.oscim.renderer.sublayers.TextItem;
import org.oscim.renderer.sublayers.TextLayer;
import org.oscim.renderer.sublayers.TextureRenderer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.OBB2D;
import org.oscim.utils.PausableThread;
import org.oscim.utils.pool.LList;
import org.oscim.utils.pool.Pool;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

public class TextRenderLayer extends BasicRenderLayer {
	private final static String TAG = TextRenderLayer.class.getName();
	private final static float MIN_CAPTION_DIST = 5;
	private final static float MIN_WAY_DIST = 3;

	private final MapViewPosition mMapViewPosition;
	private final TileSet mTileSet;
	private final LabelThread mThread;

	private MapPosition mTmpPos;

	// TextLayer that is updating
	private TextLayer mTmpLayer;
	// TextLayer that is ready to be added to 'layers'
	private TextLayer mNextLayer;

	// thread local pool
	class LabelPool extends Pool<TextItem> {
		Label releaseAndGetNext(Label l) {
			if (l.item != null)
				TextItem.pool.release(l.item);

			// drop references
			l.item = null;
			l.tile = null;
			l.string = null;

			Label ret = (Label) l.next;

			super.release(l);

			return ret;
		}

		@Override
		protected TextItem createItem() {
			return new Label();
		}
	}

	private final LabelPool mPool = new LabelPool();

	// list of previous labels
	private Label mPrevLabels;

	// list of current labels
	private Label mLabels;

	private final float[] mTmpCoords = new float[8];

	//private final HashMap<MapTile, LabelTile> mActiveTiles;

	class LabelTile {
		Tile tile;
		LList<Label> labels;
	}

	class Label extends TextItem {
		TextItem item;

		//Link blocking;
		//Link blockedBy;
		// shared list of all label for a tile
		//Link siblings;

		MapTile tile;

		//public byte origin;
		public int active;
		public OBB2D bbox;

		public TextItem move(TextItem ti, float dx, float dy) {
			this.x = dx + ti.x;
			this.y = dy + ti.y;
			return this;

		}

		public TextItem move(TextItem ti, float dx, float dy, float scale) {
			this.x = (dx + ti.x) * scale;
			this.y = (dy + ti.y) * scale;
			return this;
		}

		public void clone(TextItem ti) {
			this.string = ti.string;
			this.text = ti.text;
			this.width = ti.width;
			this.length = ti.length;
		}

		public void setAxisAlignedBBox() {
			this.x1 = x - width / 2;
			this.y1 = y - text.fontHeight / 2;
			this.x2 = x + width / 2;
			this.y2 = y + text.fontHeight / 2;
		}
	}

	//	class ActiveTile {
	//		MapTile tile;
	//		int activeLabels;
	//		Label labels;
	//	}

	/* package */boolean mRun;

	class LabelThread extends PausableThread {

		@Override
		protected void doWork() {
			SystemClock.sleep(250);

			if (!mRun)
				return;

			synchronized (this) {

				if (updateLabels()) {
					mRun = false;
					mMapView.render();
				} else {
					mRun = true;
				}
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
	private final TileRenderLayer mTileLayer;

	public TextRenderLayer(MapView mapView, TileRenderLayer baseLayer) {
		super(mapView);

		mMapViewPosition = mapView.getMapViewPosition();
		mTileLayer = baseLayer;
		mTileSet = new TileSet();
		layers.textureLayers = new TextLayer();
		mTmpLayer = new TextLayer();

		//mActiveTiles = new HashMap<MapTile, LabelTile>();
		mTmpPos = new MapPosition();
		mThread = new LabelThread();
		mThread.start();

		mRelabelCnt = 0;
	}

	// remove Label l from mLabels and return l.next
	private Label removeLabel(Label l) {
		Label ret = (Label) l.next;
		mLabels = (Label) mPool.release(mLabels, l);

		return ret;
	}

	public void addLabel(Label l) {
		TextItem ll = mLabels;

		for (; ll != null; ll = ll.next) {
			// find other label with same text style
			if (l.text == ll.text) {
				while (ll.next != null
						// break if next item uses different text style
						&& l.text == ll.next.text
						// check same string instance
						&& l.string != ll.string
						// check same string
						&& !l.string.equals(ll.string))
					ll = ll.next;

				// Note: this is required for 'packing test' in prepare to work!
				TextItem.shareText(l, ll);

				// insert after text of same type and/or before same string
				l.next = ll.next;
				ll.next = l;
				return;
			}
		}

		l.next = mLabels;
		mLabels = l;
	}

	private byte checkOverlap(Label l) {

		for (Label ll = mLabels; ll != null;) {
			// check bounding box
			if (!TextItem.bboxOverlaps(l, ll, 150)) {
				ll = (Label) ll.next;
				continue;
			}

			if (TextItem.shareText(l, ll)) {

				// keep the label that was active earlier
				if (ll.active <= l.active)
					return 1;

				// keep the label with longer segment
				if (ll.length < l.length) {
					ll = removeLabel(ll);
					continue;
				}

				return 2;
			}

			boolean intersect = l.bbox.overlaps(ll.bbox);

			if (intersect) {
				if (ll.active <= l.active)
					return 1;

				//Log.d(TAG, "intersection " + lp.string + " <> " + ti.string
				//		+ " at " + ti.x + ":" + ti.y);

				if (!ll.text.caption
						&& (ll.text.priority > l.text.priority || ll.length < l.length)) {

					ll = removeLabel(ll);
					continue;
				}

				return 1;
			}
			ll = (Label) ll.next;
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
	private final static float[] mDebugPoints = new float[4];

	private Label getLabel() {
		Label l = (Label) mPool.get();
		l.active = Integer.MAX_VALUE;

		return l;
	}

	private static float flipLatitude(float dx, int max) {
		// flip around date-line
		if (dx > max)
			dx = dx - max * 2;
		else if (dx < -max)
			dx = dx + max * 2;

		return dx;
	}

	boolean updateLabels() {
		// could this happen?
		if (mTmpLayer == null)
			return false;

		// get current tiles
		mTileLayer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0)
			return false;

		Layers dbg = null;
		if (mMapView.getDebugSettings().debugLabels)
			dbg = new Layers();

		float[] coords = mTmpCoords;
		MapPosition pos = mTmpPos;

		synchronized (mMapViewPosition) {
			mMapViewPosition.getMapPosition(pos);
			mMapViewPosition.getMapViewProjection(coords);
		}

		int mw = (mMapView.getWidth() + Tile.SIZE) / 2;
		int mh = (mMapView.getHeight() + Tile.SIZE) / 2;
		mSquareRadius = mw * mw + mh * mh;

		MapTile[] tiles = mTileSet.tiles;

		int zoom = tiles[0].zoomLevel;

		// scale of tiles zoom-level relative to current position
		double scale = pos.scale / (1 << zoom);

		double angle = Math.toRadians(pos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.SIZE << (zoom - 1);

		if (dbg != null)
			addDebugLayers(dbg);

		mRelabelCnt++;

		double tileX = (pos.x * (Tile.SIZE << zoom));
		double tileY = (pos.y * (Tile.SIZE << zoom));

		for (Label l = mPrevLabels; l != null;) {
			if (l.text.caption) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			int diff = l.tile.zoomLevel - zoom;
			if (diff > 1 || diff < -1) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			float div = FastMath.pow(diff);
			float sscale = (float) (pos.scale / (1 << l.tile.zoomLevel));

			if (l.width > l.length * sscale) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			float dx = (float) (l.tile.tileX * Tile.SIZE - tileX * div);
			float dy = (float) (l.tile.tileY * Tile.SIZE - tileY * div);

			dx = flipLatitude(dx, maxx);

			l.move(l.item, dx, dy, sscale);

			// set line endpoints relative to view to be able to
			// check intersections with label from other tiles
			float w = (l.item.x2 - l.item.x1) / 2f;
			float h = (l.item.y2 - l.item.y1) / 2f;
			l.x1 = l.x - w;
			l.y1 = l.y - h;
			l.x2 = l.x + w;
			l.y2 = l.y + h;

			if (!wayIsVisible(l)) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			l.bbox.set(l.x, l.y, l.x1, l.y1,
					l.width + MIN_WAY_DIST,
					l.text.fontHeight + MIN_WAY_DIST);

			byte overlaps = checkOverlap(l);

			if (dbg != null)
				addDebugBox(dbg, l, l.item, overlaps, true, sscale);

			if (overlaps == 0) {
				Label ll = l;
				l = (Label) l.next;

				ll.next = null;
				addLabel(ll);
				continue;
			}
			l = mPool.releaseAndGetNext(l);
		}

		Label l = null;

		/* add way labels */
		for (int i = 0, n = mTileSet.cnt; i < n; i++) {
			MapTile t = tiles[i];
			if (t.state != MapTile.STATE_READY)
				continue;

			float dx = (float) (t.tileX * Tile.SIZE - tileX);
			float dy = (float) (t.tileY * Tile.SIZE - tileY);
			dx = flipLatitude(dx, maxx);

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {

				if (ti.text.caption)
					continue;

				// acquire a TextItem to add to TextLayer
				if (l == null)
					l = getLabel();

				// check if path at current scale is long enough for text
				if (dbg == null && ti.width > ti.length * scale)
					continue;

				l.clone(ti);
				l.move(ti, dx, dy, (float) scale);

				// set line endpoints relative to view to be able to
				// check intersections with label from other tiles
				float w = (ti.x2 - ti.x1) / 2f;
				float h = (ti.y2 - ti.y1) / 2f;
				l.bbox = null;
				l.x1 = l.x - w;
				l.y1 = l.y - h;
				l.x2 = l.x + w;
				l.y2 = l.y + h;

				if (!wayIsVisible(l))
					continue;

				byte overlaps = -1;

				if (l.bbox == null)
					l.bbox = new OBB2D(l.x, l.y, l.x1, l.y1,
							l.width + MIN_WAY_DIST,
							l.text.fontHeight + MIN_WAY_DIST);
				else
					l.bbox.set(l.x, l.y, l.x1, l.y1,
							l.width + MIN_WAY_DIST,
							l.text.fontHeight + MIN_WAY_DIST);

				if (dbg == null || ti.width < ti.length * scale)
					overlaps = checkOverlap(l);

				if (dbg != null)
					addDebugBox(dbg, l, ti, overlaps, false, (float) scale);

				if (overlaps == 0) {
					addLabel(l);
					l.item = TextItem.copy(ti);
					l.tile = t;
					l.active = mRelabelCnt;
					l = null;
				}
			}
		}

		/* add caption */
		for (int i = 0, n = mTileSet.cnt; i < n; i++) {
			MapTile t = tiles[i];
			if (t.state != MapTile.STATE_READY)
				continue;

			float dx = (float) (t.tileX * Tile.SIZE - tileX);
			float dy = (float) (t.tileY * Tile.SIZE - tileY);
			dx = flipLatitude(dx, maxx);

			for (TextItem ti = t.labels; ti != null; ti = ti.next) {
				if (!ti.text.caption)
					continue;

				// acquire a TextItem to add to TextLayer
				if (l == null)
					l = getLabel();

				l.clone(ti);
				l.move(ti, dx, dy, (float) scale);
				if (!nodeIsVisible(l))
					continue;

				//l.setAxisAlignedBBox();

				if (l.bbox == null)
					l.bbox = new OBB2D();

				l.bbox.setNormalized(l.x, l.y, cos, -sin,
						l.width + MIN_CAPTION_DIST,
						l.text.fontHeight + MIN_CAPTION_DIST);

				boolean overlaps = false;

				for (Label lp = mLabels; lp != null;) {
					if (l.bbox.overlaps(lp.bbox)) {
						if (l.text.priority < lp.text.priority) {
							lp = removeLabel(lp);
							continue;
						}

						overlaps = true;
						break;
					}
					lp = (Label) lp.next;
				}
				if (!overlaps) {
					addLabel(l);
					l.item = TextItem.copy(ti);
					l.tile = t;
					l.active = mRelabelCnt;
					l = null;
				}
			}
		}

		for (Label ti = mLabels; ti != null; ti = (Label) ti.next) {
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

		// temporarily used Label
		mPool.release(l);

		//reuse text layer
		TextLayer tl = mTmpLayer;
		mTmpLayer = null;

		tl.labels = mLabels;
		// draw text to bitmaps and create vertices
		tl.prepare();

		// after 'prepare' TextLayer does not need TextItems
		mPrevLabels = mLabels;
		mLabels = null;
		tl.labels = null;

		// remove tile locks
		mTileLayer.releaseTiles(mTileSet);

		// pass new labels for rendering
		//synchronized (this) {
			mNextLayer = tl;
			mDebugLayer = dbg;
		//}

		return true;
	}

	private static void addDebugBox(Layers dbg, Label l, TextItem ti, int overlaps, boolean prev,
			float scale) {

		LineLayer ll;
		if (prev) {
			if (overlaps == 1)
				ll = dbg.getLineLayer(4);
			else
				ll = dbg.getLineLayer(5);

		} else {
			if (ti.width > ti.length * scale) {
				ll = dbg.getLineLayer(1);
				overlaps = 3;
			}
			else if (overlaps == 1)
				ll = dbg.getLineLayer(0);
			else if (overlaps == 2)
				ll = dbg.getLineLayer(3);
			else
				ll = dbg.getLineLayer(2);
		}
		float[] points = mDebugPoints;
		float width = (ti.x2 - ti.x1) / 2f;
		float height = (ti.y2 - ti.y1) / 2f;
		points[0] = (l.x - width * scale);
		points[1] = (l.y - height * scale);
		points[2] = (l.x + width * scale);
		points[3] = (l.y + height * scale);
		ll.addLine(points, null, false);

		if (l.bbox != null && overlaps != 3) {
			ll.addLine(l.bbox.corner, null, true);
		}
	}

	private static void addDebugLayers(Layers dbg) {
		int alpha = 0xaaffffff;

		dbg.clear();
		dbg.addLineLayer(0, new Line((Color.BLUE & alpha), 2));
		dbg.addLineLayer(1, new Line((Color.RED & alpha), 2));
		dbg.addLineLayer(3, new Line((Color.YELLOW & alpha), 2));
		dbg.addLineLayer(2, new Line((Color.GREEN & alpha), 2));
		dbg.addLineLayer(4, new Line((Color.CYAN & alpha), 2));
		dbg.addLineLayer(5, new Line((Color.MAGENTA & alpha), 2));
	}

	@Override
	public synchronized void update(MapPosition pos, boolean changed,
			Matrices matrices) {

		if (mNextLayer != null) {
			// keep text layer, not recrating its canvas each time
			mTmpLayer = (TextLayer) layers.textureLayers;

			// clear textures and text items from previous layer
			layers.clear();

			if (mDebugLayer != null) {
				layers.baseLayers = mDebugLayer.baseLayers;
				mDebugLayer = null;
			}

			// set new TextLayer to be uploaded and rendered
			layers.textureLayers = mNextLayer;
			mNextLayer = null;

			// make the 'labeled' MapPosition current
			MapPosition tmp = mMapPosition;
			mMapPosition = mTmpPos;
			mTmpPos = tmp;

			this.newData = true;

			if (!changed)
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

		if (layers.vbo == null) {
			layers.vbo = BufferObject.get(0);
		}

		if (newSize > 0) {
			if (GLRenderer.uploadLayers(layers, newSize, true))
				isReady = true;
		}
	}

	@Override
	public synchronized void render(MapPosition pos, Matrices m) {

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, layers.vbo.id);
		GLState.test(false, false);

		float scale = (float) (mMapPosition.scale / pos.scale);

		if (layers.baseLayers != null) {
			setMatrix(pos, m, true);

			for (Layer l = layers.baseLayers; l != null;) {
				if (l.type == Layer.POLYGON) {
					l = PolygonRenderer.draw(pos, l, m, true, false);
				} else {
					float div = scale * (float) (pos.scale / (1 << pos.zoomLevel));
					l = LineRenderer.draw(layers, l, pos, m, div, 0);
				}
			}
		}

		setMatrix(pos, m, false);

		for (Layer l = layers.textureLayers; l != null;)
			l = TextureRenderer.draw(l, scale, m);
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

	public void clearLabels() {
		Log.d(TAG, "clearLabels");
		synchronized (mThread) {
			mRun = false;
			mPool.releaseAll(mPrevLabels);
			mPrevLabels = null;
			mTileSet.clear();
		}
	}
}
