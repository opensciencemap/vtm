/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

package org.oscim.layers.tile.vector.labeling;

// TODO
// 1. rewrite. seriously
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
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.SymbolLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.renderer.elements.TextLayer;
import org.oscim.renderer.elements.TextureLayer;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileRenderer;
import org.oscim.tiling.TileSet;
import org.oscim.utils.FastMath;
import org.oscim.utils.OBB2D;
import org.oscim.utils.async.SimpleWorker;
import org.oscim.utils.pool.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextRenderer extends ElementRenderer {
	static final Logger log = LoggerFactory.getLogger(TextRenderer.class);

	private final static float MIN_CAPTION_DIST = 5;
	private final static float MIN_WAY_DIST = 3;

	private final static long MAX_RELABEL_DELAY = 200;

	private final Viewport mViewport;
	private final TileSet mTileSet;

	//private ElementLayers mDebugLayer;

	class LabelTask {
		final TextureLayer layers;
		final TextLayer textLayer;
		final SymbolLayer symbolLayer;

		final MapPosition pos;

		LabelTask() {
			pos = new MapPosition();

			symbolLayer = new SymbolLayer();
			textLayer = new TextLayer();

			layers = symbolLayer;
			symbolLayer.next = textLayer;

		}
	}

	// thread local pool (labeling)
	class LabelPool extends Pool<TextItem> {
		Label releaseAndGetNext(Label l) {
			if (l.item != null)
				l.item = TextItem.pool.release(l.item);

			// drop references
			l.item = null;
			l.tile = null;
			l.string = null;

			Label ret = (Label) l.next;

			// ignore warning
			super.release(l);

			return ret;
		}

		@Override
		protected TextItem createItem() {
			return new Label();
		}
	}

	private final LabelPool mPool = new LabelPool();

	// list of current labels
	private Label mLabels;

	//private final float[] mTmpCoords = new float[8];

	//private final HashMap<MapTile, LabelTile> mActiveTiles;

	private float mSquareRadius;
	private int mRelabelCnt;
	private final TileRenderer mTileLayer;
	private final Map mMap;
	private final Worker mWorker;

	public TextRenderer(Map map, TileRenderer baseLayer) {
		mMap = map;
		mViewport = map.getViewport();
		mTileLayer = baseLayer;
		mTileSet = new TileSet();

		layers.textureLayers = new TextLayer();
		layers.textureLayers.next = new SymbolLayer();

		//mActiveTiles = new HashMap<MapTile, LabelTile>();
		mRelabelCnt = 0;

		mWorker = new Worker(map);
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

				//log.debug("intersection " + lp.string + " <> " + ti.string
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

	private boolean iconIsVisible(int x, int y) {
		// rough filter
		float dist = x * x + y * y;
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

	private Label getLabel() {
		Label l = (Label) mPool.get();
		l.active = Integer.MAX_VALUE;

		return l;
	}

	private static float flipLongitude(float dx, int max) {
		// flip around date-line
		if (dx > max)
			dx = dx - max * 2;
		else if (dx < -max)
			dx = dx + max * 2;

		return dx;
	}

	private Label updateWayLabels(MapTile t, Label l, float dx, float dy, double scale,
	        ElementLayers dbg) {

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
				Debug.addDebugBox(dbg, l, ti, overlaps, false, (float) scale);

			if (overlaps == 0) {
				addLabel(l);
				l.item = TextItem.copy(ti);
				l.tile = t;
				l.active = mRelabelCnt;
				l = null;
			}
		}
		return l;
	}

	private Label updateNodeLabels(MapTile t, Label l, float dx, float dy, double scale, float cos,
	        float sin) {
		O: for (TextItem ti = t.labels; ti != null; ti = ti.next) {
			if (!ti.text.caption)
				continue;

			// acquire a TextItem to add to TextLayer
			if (l == null)
				l = getLabel();

			l.clone(ti);
			l.move(ti, dx, dy, (float) scale);
			if (!nodeIsVisible(l))
				continue;

			if (l.bbox == null)
				l.bbox = new OBB2D();

			l.bbox.setNormalized(l.x, l.y, cos, -sin,
			                     l.width + MIN_CAPTION_DIST,
			                     l.text.fontHeight + MIN_CAPTION_DIST,
			                     l.text.dy);

			for (Label lp = mLabels; lp != null;) {
				if (l.bbox.overlaps(lp.bbox)) {
					if (l.text.priority < lp.text.priority) {
						lp = removeLabel(lp);
						continue;
					}
					continue O;
				}
				lp = (Label) lp.next;
			}

			addLabel(l);
			l.item = TextItem.copy(ti);
			l.tile = t;
			l.active = mRelabelCnt;
			l = null;
		}
		return l;
	}

	boolean updateLabels(LabelTask work) {
		// nextLayer is not loaded yet
		//if (mNextLayer.ready)
		//	return false;

		// get current tiles
		boolean changedTiles = mTileLayer.getVisibleTiles(mTileSet);
		boolean changedPos;

		if (mTileSet.cnt == 0) {
			//log.debug("no tiles "+ mTileSet.getSerial());
			return false;
		}

		MapPosition pos = work.pos;

		synchronized (mViewport) {
			changedPos = mViewport.getMapPosition(pos);
			//mViewport.getMapViewProjection(coords);
		}

		if (!changedTiles && !changedPos) {
			//log.debug("not changed " + changedTiles + " " + changedPos);
			return false;
		}

		ElementLayers dbg = null;
		//if (mMap.getDebugSettings().debugLabels)
		//	dbg = new ElementLayers();

		int mw = (mMap.getWidth() + Tile.SIZE) / 2;
		int mh = (mMap.getHeight() + Tile.SIZE) / 2;
		mSquareRadius = mw * mw + mh * mh;

		MapTile[] tiles = mTileSet.tiles;
		int zoom = tiles[0].zoomLevel;

		// scale of tiles zoom-level relative to current position
		double scale = pos.scale / (1 << zoom);

		double angle = Math.toRadians(pos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.SIZE << (zoom - 1);

		//if (dbg != null)
		//	Debug.addDebugLayers(dbg);

		SymbolLayer sl = work.symbolLayer;
		sl.clearItems();

		mRelabelCnt++;

		double tileX = (pos.x * (Tile.SIZE << zoom));
		double tileY = (pos.y * (Tile.SIZE << zoom));

		Label prevLabels = mLabels;
		mLabels = null;

		for (Label l = prevLabels; l != null;) {
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

			dx = flipLongitude(dx, maxx);

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
				Debug.addDebugBox(dbg, l, l.item, overlaps, true, sscale);

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
			synchronized (t) {
				if (!t.state(MapTile.STATE_READY))
					continue;

				float dx = (float) (t.tileX * Tile.SIZE - tileX);
				float dy = (float) (t.tileY * Tile.SIZE - tileY);
				dx = flipLongitude(dx, maxx);

				l = updateWayLabels(t, l, dx, dy, scale, dbg);
			}
		}

		/* add caption */
		for (int i = 0, n = mTileSet.cnt; i < n; i++) {
			MapTile t = tiles[i];
			synchronized (t) {
				if (!t.state(MapTile.STATE_READY))
					continue;

				float dx = (float) (t.tileX * Tile.SIZE - tileX);
				float dy = (float) (t.tileY * Tile.SIZE - tileY);
				dx = flipLongitude(dx, maxx);

				l = updateNodeLabels(t, l, dx, dy, scale, cos, sin);
			}
		}

		for (Label ti = mLabels; ti != null; ti = (Label) ti.next) {
			if (ti.text.caption) {
				if (ti.text.texture != null) {
					SymbolItem s = SymbolItem.pool.get();
					s.texRegion = ti.text.texture;
					s.x = ti.x;
					s.y = ti.y;
					s.billboard = true;
					sl.addSymbol(s);
				}
				continue;
			}
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

		for (int i = 0, n = mTileSet.cnt; i < n; i++) {
			MapTile t = tiles[i];
			synchronized (t) {
				if (!t.state(MapTile.STATE_READY))
					continue;

				float dx = (float) (t.tileX * Tile.SIZE - tileX);
				float dy = (float) (t.tileY * Tile.SIZE - tileY);
				dx = flipLongitude(dx, maxx);

				for (SymbolItem ti = t.symbols; ti != null; ti = ti.next) {
					if (ti.texRegion == null)
						continue;

					int x = (int) ((dx + ti.x) * scale);
					int y = (int) ((dy + ti.y) * scale);

					if (!iconIsVisible(x, y))
						continue;

					SymbolItem s = SymbolItem.pool.get();
					s.texRegion = ti.texRegion;
					s.x = x;
					s.y = y;
					s.billboard = true;
					sl.addSymbol(s);
				}
			}
		}

		// temporary used Label
		l = (Label) mPool.release(l);

		// draw text to bitmaps and create vertices
		work.textLayer.labels = mLabels;
		work.textLayer.prepare();
		work.textLayer.labels = null;

		// remove tile locks
		mTileLayer.releaseTiles(mTileSet);

		//mDebugLayer = dbg;
		//mNextLayer.ready = true;

		return true;
	}

	long lastDraw = 0;

	@Override
	public synchronized void update(MapPosition pos, boolean changed,
	        Matrices matrices) {

		LabelTask t;
		synchronized (mWorker) {

			t = mWorker.poll();

			if (t == null)
				return;

			layers.clear();
		}

		// set new TextLayer to be uploaded and rendered
		layers.textureLayers = t.layers;
		mMapPosition = t.pos;
		compile();

		update();
	}

	@Override
	public synchronized void render(MapPosition pos, Matrices m) {
		GLState.test(false, false);

		layers.vbo.bind();

		float scale = (float) (mMapPosition.scale / pos.scale);

		if (layers.baseLayers != null) {
			setMatrix(pos, m, true);

			for (RenderElement l = layers.baseLayers; l != null;) {
				if (l.type == RenderElement.POLYGON) {
					l = PolygonLayer.Renderer.draw(pos, l, m, true, 1, false);
				} else {
					float div = (float) (mMapPosition.scale / (1 << pos.zoomLevel));
					l = LineLayer.Renderer.draw(layers, l, pos, m, div);
				}
			}
		}

		setMatrix(pos, m, false);

		for (RenderElement l = layers.textureLayers; l != null;)
			l = TextureLayer.Renderer.draw(l, scale, m);
	}

	final class Worker extends SimpleWorker<LabelTask> {

		public Worker(Map map) {
			super(map, 10, new LabelTask(), new LabelTask());
		}

		@Override
		public boolean doWork(LabelTask t) {

			if (updateLabels(t)) {
				mMap.render();
				return true;
			}

			return false;
		}

		@Override
		public void cleanup(LabelTask t) {
		}

		@Override
		public void finish() {
			mLabels = (Label) mPool.releaseAll(mLabels);
			mTileSet.releaseTiles();
		}
	}

	public void clearLabels() {
		mWorker.cancel(true);
	}

	public void update() {
		mWorker.submit(MAX_RELABEL_DELAY);
	}
}
