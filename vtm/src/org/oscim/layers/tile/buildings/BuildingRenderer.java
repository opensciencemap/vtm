package org.oscim.layers.tile.buildings;

import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;

import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileDistanceSort;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayers;
import org.oscim.utils.FastMath;

public class BuildingRenderer extends ExtrusionRenderer {

	private final TileRenderer mTileLayer;
	private final TileSet mTileSet;

	private final int mZoomMin;
	private final int mZoomMax;

	private final float mFadeInTime = 300;
	private final float mFadeOutTime = 500;

	private long mStartTime;

	public BuildingRenderer(TileRenderer tileRenderLayer, int zoomMin, int zoomMax,
	        boolean mesh, boolean alpha) {
		super(mesh, alpha);

		mZoomMax = zoomMax;
		mZoomMin = zoomMin;
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
	}

	@Override
	protected boolean setup() {
		mAlpha = 0;
		return super.setup();

	}

	@Override
	public void update(GLViewport v) {

		int diff = (v.pos.zoomLevel - mZoomMin);

		/* if below min zoom or already faded out */
		if ((diff < -1)) {
			setReady(false);
			return;
		}

		boolean show = diff >= 0;

		if (show) {
			if (mAlpha < 1) {
				//log.debug("fade in {}", mAlpha);
				long now = System.currentTimeMillis();
				if (mStartTime == 0) {
					mStartTime = now;
				}
				float a = (now - mStartTime) / mFadeInTime;
				mAlpha = FastMath.clamp(a, 0, 1);
				MapRenderer.animate();
			} else
				mStartTime = 0;
		} else {
			if (mAlpha > 0) {
				//log.debug("fade out {} {}", mAlpha, mStartTime);
				long now = System.currentTimeMillis();
				if (mStartTime == 0) {
					mStartTime = now + 200; // delay hide a little
				}
				long dt = (now - mStartTime);
				if (dt > 0) {
					float a = 1 - dt / mFadeOutTime;
					mAlpha = FastMath.clamp(a, 0, 1);
				}
				MapRenderer.animate();
			} else
				mStartTime = 0;
		}

		if (mAlpha == 0 || v.pos.zoomLevel < (mZoomMin - 1)) {
			setReady(false);
			return;
		}

		mTileLayer.getVisibleTiles(mTileSet);

		if (mTileSet.cnt == 0) {
			mTileLayer.releaseTiles(mTileSet);
			setReady(false);
			return;
		}

		MapTile[] tiles = mTileSet.tiles;
		TileDistanceSort.sort(tiles, 0, mTileSet.cnt);

		/* keep a list of tiles available for rendering */
		if (mExtrusionLayerSet == null || mExtrusionLayerSet.length < mTileSet.cnt * 4)
			mExtrusionLayerSet = new ExtrusionLayers[mTileSet.cnt * 4];

		/* compile one tile max per frame */
		boolean compiled = false;

		int activeTiles = 0;
		int zoom = tiles[0].zoomLevel;

		if (zoom >= mZoomMin && zoom <= mZoomMax) {
			/* TODO - if tile is not available try parent or children */

			for (int i = 0; i < mTileSet.cnt; i++) {
				ExtrusionLayers els = getLayer(tiles[i]);
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

				ExtrusionLayers els = getLayer(t);
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
					ExtrusionLayers el = getLayer(c);

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
			mTileLayer.releaseTiles(mTileSet);
			setReady(false);
			return;
		}
		setReady(true);
	}

	@Override
	public void render(GLViewport v) {
		super.render(v);

		/* release lock on tile data */
		mTileLayer.releaseTiles(mTileSet);
	}

	private static ExtrusionLayers getLayer(MapTile t) {
		ElementLayers layers = t.getLayers();
		if (layers != null && !t.state(READY | NEW_DATA))
			return null;

		return BuildingLayer.get(t);
	}
}
