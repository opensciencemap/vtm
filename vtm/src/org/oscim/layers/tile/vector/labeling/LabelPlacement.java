package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.SymbolLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileRenderer;
import org.oscim.tiling.TileSet;
import org.oscim.utils.FastMath;
import org.oscim.utils.OBB2D;

public class LabelPlacement {
	static final boolean dbg = false;

	private final static float MIN_CAPTION_DIST = 5;
	private final static float MIN_WAY_DIST = 3;

	/** thread local pool of for unused label items */
	private final LabelPool mPool = new LabelPool();

	private final TileSet mTileSet = new TileSet();
	private final TileRenderer mTileRenderer;
	private final Map mMap;

	/** list of current labels */
	private Label mLabels;

	private float mSquareRadius;

	/**
	 * incremented each update, to prioritize labels
	 * that became visible ealier.
	 */
	private int mRelabelCnt;

	public LabelPlacement(Map map, TileRenderer tileRenderer) {
		mMap = map;
		mTileRenderer = tileRenderer;
	}

	/** remove Label l from mLabels and return l.next */
	private Label removeLabel(Label l) {
		Label ret = (Label) l.next;
		mLabels = (Label) mPool.release(mLabels, l);

		return ret;
	}

	public void addLabel(Label l) {
		for (Label o = mLabels; o != null; o = (Label) o.next) {
			/* find other label with same text style */
			if (l.text == o.text) {
				while (o.next != null
				        /* break if next item uses different text style */
				        && l.text == o.next.text
				        /* check same string instance */
				        && l.string != o.string
				        /* check same string */
				        && !l.string.equals(o.string))
					o = (Label) o.next;

				/* Note: required for 'packing test' in prepare to work */
				Label.shareText(l, o);
				/* insert after text of same type or before same string */
				l.next = o.next;
				o.next = l;
				return;
			}
		}
		l.next = mLabels;
		mLabels = l;
	}

	private byte checkOverlap(Label l) {

		for (Label o = mLabels; o != null;) {
			//check bounding box
			if (!Label.bboxOverlaps(l, o, 150)) {
				o = (Label) o.next;
				continue;
			}

			if (Label.shareText(l, o)) {
				// keep the label that was active earlier
				if (o.active <= l.active)
					return 1;

				// keep the label with longer segment
				if (o.length < l.length) {
					o = removeLabel(o);
					continue;
				}
				// keep other
				return 2;
			}
			if (l.bbox.overlaps(o.bbox)) {
				if (o.active <= l.active)
					return 1;

				if (!o.text.caption
				        && (o.text.priority > l.text.priority
				        || o.length < l.length)) {

					o = removeLabel(o);
					continue;
				}
				// keep other
				return 1;
			}
			o = (Label) o.next;
		}
		return 0;
	}

	private boolean isVisible(float x, float y) {
		// rough filter
		float dist = x * x + y * y;
		if (dist > mSquareRadius)
			return false;

		return true;
	}

	private boolean wayIsVisible(Label ti) {
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

	private void placeLabelFrom(Label l, TextItem ti) {
		// set line endpoints relative to view to be able to
		// check intersections with label from other tiles
		float w = (ti.x2 - ti.x1) / 2f;
		float h = (ti.y2 - ti.y1) / 2f;

		l.x1 = (int) (l.x - w);
		l.y1 = (int) (l.y - h);
		l.x2 = (int) (l.x + w);
		l.y2 = (int) (l.y + h);
	}

	private Label addWayLabels(MapTile t, Label l, float dx, float dy,
	        double scale) {

		for (TextItem ti = t.labels; ti != null; ti = ti.next) {
			if (ti.text.caption)
				continue;

			/* acquire a TextItem to add to TextLayer */
			if (l == null)
				l = getLabel();

			/* check if path at current scale is long enough */
			if (!dbg && ti.width > ti.length * scale)
				continue;

			l.clone(ti);
			l.x = (float) ((dx + ti.x) * scale);
			l.y = (float) ((dy + ti.y) * scale);
			placeLabelFrom(l, ti);

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

			if (dbg || ti.width < ti.length * scale)
				overlaps = checkOverlap(l);

			if (dbg)
				Debug.addDebugBox(l, ti, overlaps, false, (float) scale);

			if (overlaps == 0) {
				addLabel(l);
				l.item = TextItem.copy(ti);
				l.tileX = t.tileX;
				l.tileY = t.tileY;
				l.tileZ = t.zoomLevel;
				l.active = mRelabelCnt;
				l = null;
			}
		}
		return l;
	}

	private Label addNodeLabels(MapTile t, Label l, float dx, float dy,
	        double scale, float cos, float sin) {
		O: for (TextItem ti = t.labels; ti != null; ti = ti.next) {
			if (!ti.text.caption)
				continue;

			// acquire a TextItem to add to TextLayer
			if (l == null)
				l = getLabel();

			l.clone(ti);
			l.x = (float) ((dx + ti.x) * scale);
			l.y = (float) ((dy + ti.y) * scale);
			if (!isVisible(l.x, l.y))
				continue;

			if (l.bbox == null)
				l.bbox = new OBB2D();

			l.bbox.setNormalized(l.x, l.y, cos, -sin,
			                     l.width + MIN_CAPTION_DIST,
			                     l.text.fontHeight + MIN_CAPTION_DIST,
			                     l.text.dy);

			for (Label o = mLabels; o != null;) {
				if (l.bbox.overlaps(o.bbox)) {
					if (l.text.priority < o.text.priority) {
						o = removeLabel(o);
						continue;
					}
					continue O;
				}
				o = (Label) o.next;
			}

			addLabel(l);
			l.item = TextItem.copy(ti);
			l.tileX = t.tileX;
			l.tileY = t.tileY;
			l.tileZ = t.zoomLevel;
			l.active = mRelabelCnt;
			l = null;
		}
		return l;
	}

	boolean updateLabels(LabelTask work) {

		/* get current tiles */
		boolean changedTiles = mTileRenderer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0) {
			//log.debug("no tiles "+ mTileSet.getSerial());
			return false;
		}

		MapPosition pos = work.pos;
		boolean changedPos = mMap.getViewport().getMapPosition(pos);

		/* do not loop! */
		if (!changedTiles && !changedPos)
			return false;

		mRelabelCnt++;

		MapTile[] tiles = mTileSet.tiles;
		int zoom = tiles[0].zoomLevel;

		/* estimation for visible area to be labeled */
		int mw = (mMap.getWidth() + Tile.SIZE) / 2;
		int mh = (mMap.getHeight() + Tile.SIZE) / 2;
		mSquareRadius = mw * mw + mh * mh;

		/* scale of tiles zoom-level relative to current position */
		double scale = pos.scale / (1 << zoom);

		double angle = Math.toRadians(pos.angle);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		int maxx = Tile.SIZE << (zoom - 1);

		// FIXME ???
		SymbolLayer sl = work.symbolLayer;
		sl.clearItems();

		double tileX = (pos.x * (Tile.SIZE << zoom));
		double tileY = (pos.y * (Tile.SIZE << zoom));

		/* put current label to previous label */
		Label prevLabels = mLabels;

		/* new labels */
		mLabels = null;
		Label l = null;

		/* add currently active labels first */
		for (l = prevLabels; l != null;) {

			if (l.text.caption) {
				// TODO!!!
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			int diff = l.tileZ - zoom;
			if (diff > 1 || diff < -1) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			float div = FastMath.pow(diff);
			float sscale = (float) (pos.scale / (1 << l.tileZ));

			if (l.width > l.length * sscale) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			float dx = (float) (l.tileX * Tile.SIZE - tileX * div);
			float dy = (float) (l.tileY * Tile.SIZE - tileY * div);

			dx = flipLongitude(dx, maxx);
			l.x = (float) ((dx + l.item.x) * sscale);
			l.y = (float) ((dy + l.item.y) * sscale);
			placeLabelFrom(l, l.item);

			if (!wayIsVisible(l)) {
				l = mPool.releaseAndGetNext(l);
				continue;
			}

			l.bbox.set(l.x, l.y, l.x1, l.y1,
			           l.width + MIN_WAY_DIST,
			           l.text.fontHeight + MIN_WAY_DIST);

			byte overlaps = checkOverlap(l);

			if (dbg)
				Debug.addDebugBox(l, l.item, overlaps, true, sscale);

			if (overlaps == 0) {
				Label ll = l;
				l = (Label) l.next;

				ll.next = null;
				addLabel(ll);
				continue;
			}
			l = mPool.releaseAndGetNext(l);
		}

		/* add way labels */
		for (int i = 0, n = mTileSet.cnt; i < n; i++) {
			MapTile t = tiles[i];
			synchronized (t) {
				if (!t.state(MapTile.STATE_READY))
					continue;

				float dx = (float) (t.tileX * Tile.SIZE - tileX);
				float dy = (float) (t.tileY * Tile.SIZE - tileY);
				dx = flipLongitude(dx, maxx);

				l = addWayLabels(t, l, dx, dy, scale);
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

				l = addNodeLabels(t, l, dx, dy, scale, cos, sin);
			}
		}

		for (Label ti = mLabels; ti != null; ti = (Label) ti.next) {
			/* add caption symbols */
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

			/* flip way label orientation */
			if (cos * (ti.x2 - ti.x1) - sin * (ti.y2 - ti.y1) < 0) {
				float tmp = ti.x1;
				ti.x1 = ti.x2;
				ti.x2 = tmp;

				tmp = ti.y1;
				ti.y1 = ti.y2;
				ti.y2 = tmp;
			}
		}

		/* add symbol items */
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

					if (!isVisible(x, y))
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

		/* temporary used Label */
		l = (Label) mPool.release(l);

		/* draw text to bitmaps and create vertices */
		work.textLayer.labels = mLabels;
		work.textLayer.prepare();
		work.textLayer.labels = null;

		/* remove tile locks */
		mTileRenderer.releaseTiles(mTileSet);

		return true;
	}

	public void cleanup() {
		mLabels = (Label) mPool.releaseAll(mLabels);
		mTileSet.releaseTiles();
	}
}
