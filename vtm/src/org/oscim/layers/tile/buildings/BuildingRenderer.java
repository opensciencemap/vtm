package org.oscim.layers.tile.buildings;

import static java.lang.System.currentTimeMillis;
import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;
import static org.oscim.utils.FastMath.clamp;

import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileDistanceSort;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.bucket.ExtrusionBuckets;
import org.oscim.renderer.bucket.RenderBuckets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildingRenderer extends ExtrusionRenderer {
	static final Logger log = LoggerFactory.getLogger(BuildingRenderer.class);

	private final TileRenderer mTileRenderer;
	private final TileSet mTileSet;

	private final int mZoomMin;
	private final int mZoomMax;

	private final float mFadeInTime = 250;
	private final float mFadeOutTime = 400;

	private long mAnimTime;
	private boolean mShow;

	public BuildingRenderer(TileRenderer tileRenderer, int zoomMin, int zoomMax,
	        boolean mesh, boolean alpha) {
		super(mesh, alpha);

		mZoomMax = zoomMax;
		mZoomMin = zoomMin;
		mTileRenderer = tileRenderer;
		mTileSet = new TileSet();
	}

	@Override
	public boolean setup() {
		mAlpha = 0;
		return super.setup();

	}

	@Override
	public void update(GLViewport v) {

		int diff = (v.pos.zoomLevel - mZoomMin);

		/* if below min zoom or already faded out */
		if (diff < -1) {
			mAlpha = 0;
			mShow = false;
			setReady(false);
			return;
		}

		if (diff >= 0) {
			if (mAlpha < 1) {
				long now = currentTimeMillis();
				if (!mShow)
					mAnimTime = now - (long) (mAlpha * mFadeInTime);

				mShow = true;
				mAlpha = clamp((now - mAnimTime) / mFadeInTime, 0, 1);
				MapRenderer.animate();
			}
		} else {
			if (mAlpha > 0) {
				long now = currentTimeMillis();
				if (mShow)
					mAnimTime = now - (long) ((1 - mAlpha) * mFadeOutTime);

				mShow = false;
				mAlpha = clamp(1 - (now - mAnimTime) / mFadeOutTime, 0, 1);
				MapRenderer.animate();
			}
		}

		if (mAlpha == 0) {
			setReady(false);
			return;
		}

		mTileRenderer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0) {
			mTileRenderer.releaseTiles(mTileSet);
			setReady(false);
			return;
		}

		MapTile[] tiles = mTileSet.tiles;
		TileDistanceSort.sort(tiles, 0, mTileSet.cnt);

		/* keep a list of tiles available for rendering */
		if (mExtrusionLayerSet == null || mExtrusionLayerSet.length < mTileSet.cnt * 4)
			mExtrusionLayerSet = new ExtrusionBuckets[mTileSet.cnt * 4];

		/* compile one tile max per frame */
		boolean compiled = false;

		int activeTiles = 0;
		int zoom = tiles[0].zoomLevel;

		if (zoom >= mZoomMin && zoom <= mZoomMax) {
			/* TODO - if tile is not available try parent or children */

			for (int i = 0; i < mTileSet.cnt; i++) {
				ExtrusionBuckets els = getLayer(tiles[i]);
				if (els == null)
					continue;

				if (els.compiled)
					mExtrusionLayerSet[activeTiles++] = els;
				else if (!compiled && els.compileLayers()) {
					mExtrusionLayerSet[activeTiles++] = els;
					compiled = true;
				}
			}
		} else if (zoom == mZoomMax + 1) {
			/* special case for s3db: render from parent tiles */
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i].node.parent();

				if (t == null)
					continue;

				//	for (MapTile c : mTiles)
				//		if (c == t)
				//			continue O;

				ExtrusionBuckets els = getLayer(t);
				if (els == null)
					continue;

				if (els.compiled)
					mExtrusionLayerSet[activeTiles++] = els;

				else if (!compiled && els.compileLayers()) {
					mExtrusionLayerSet[activeTiles++] = els;
					compiled = true;
				}
			}
		} else if (zoom == mZoomMin - 1) {
			/* check if proxy children are ready */
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i];
				for (byte j = 0; j < 4; j++) {
					if (!t.hasProxy(1 << j))
						continue;

					MapTile c = t.node.child(j);
					ExtrusionBuckets el = getLayer(c);

					if (el == null || !el.compiled)
						continue;

					mExtrusionLayerSet[activeTiles++] = el;
				}
			}
		}

		/* load more tiles on next frame */
		if (compiled)
			MapRenderer.animate();

		mExtrusionLayerCnt = activeTiles;

		//log.debug("active tiles: {}", mExtrusionLayerCnt);

		if (activeTiles == 0) {
			mTileRenderer.releaseTiles(mTileSet);
			setReady(false);
			return;
		}
		setReady(true);
	}

	@Override
	public void render(GLViewport v) {
		super.render(v);

		/* release lock on tile data */
		mTileRenderer.releaseTiles(mTileSet);
	}

	private static ExtrusionBuckets getLayer(MapTile t) {
		RenderBuckets layers = t.getBuckets();
		if (layers != null && !t.state(READY | NEW_DATA))
			return null;

		return BuildingLayer.get(t);
	}
}
