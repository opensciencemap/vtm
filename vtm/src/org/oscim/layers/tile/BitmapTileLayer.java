/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers.tile;

import static org.oscim.tiling.MapTile.State.CANCEL;

import java.util.concurrent.CancellationException;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.tiling.VectorTileRenderer;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.ITileDataSource.QueryResult;
import org.oscim.tiling.source.TileSource;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitmapTileLayer extends TileLayer {

	protected static final Logger log = LoggerFactory.getLogger(BitmapTileLayer.class);

	private final static int CACHE_LIMIT = 20;

	protected final TileSource mTileSource;

	public static class FadeStep {
		public final double scaleStart, scaleEnd;
		public final float alphaStart, alphaEnd;

		public FadeStep(int zoomStart, int zoomEnd, float alphaStart, float alphaEnd) {
			this.scaleStart = 1 << zoomStart;
			this.scaleEnd = 1 << zoomEnd;
			this.alphaStart = alphaStart;
			this.alphaEnd = alphaEnd;
		}
	}

	public BitmapTileLayer(Map map, TileSource tileSource) {
		this(map, tileSource, CACHE_LIMIT);
	}

	public BitmapTileLayer(Map map, TileSource tileSource, int cacheLimit) {
		super(map, tileSource.getZoomLevelMin(), tileSource.getZoomLevelMax(), cacheLimit);
		mTileSource = tileSource;
		setRenderer(new VectorTileRenderer(mTileManager));
		initLoader(4);
	}

	@Override
	public void onMapUpdate(MapPosition pos, boolean changed, boolean clear) {
		super.onMapUpdate(pos, changed, clear);

		FadeStep[] fade = mTileSource.getFadeSteps();

		if (fade == null) {
			//mRenderLayer.setBitmapAlpha(1);
			return;
		}

		float alpha = 0;
		for (FadeStep f : fade) {
			if (pos.scale < f.scaleStart || pos.scale > f.scaleEnd)
				continue;

			if (f.alphaStart == f.alphaEnd) {
				alpha = f.alphaStart;
				break;
			}
			double range = f.scaleEnd / f.scaleStart;
			float a = (float) ((range - (pos.scale / f.scaleStart)) / range);
			a = FastMath.clamp(a, 0, 1);
			// interpolate alpha between start and end
			alpha = a * f.alphaStart + (1 - a) * f.alphaEnd;
			break;
		}

		tileRenderer().setBitmapAlpha(alpha);
	}

	@Override
	protected TileLoader createLoader(TileManager tm) {
		return new BitmapTileLoader(tm, mTileSource);
	}

	class BitmapTileLoader extends TileLoader implements ITileDataSink {

		private final ITileDataSource mTileDataSource;
		private MapTile mTile;

		public BitmapTileLoader(TileManager tileManager, TileSource tileSource) {
			super(tileManager);
			mTileDataSource = tileSource.getDataSource();
		}

		@Override
		public void cleanup() {
			mTile = null;
		}

		@Override
		protected boolean executeJob(MapTile tile) {
			mTile = tile;
			QueryResult result = null;
			try {
				result = mTileDataSource.executeQuery(tile, this);
			} catch (CancellationException e) {
				log.debug("{} was canceled", mTile);
			} catch (Exception e) {
				log.debug("{} {}", mTile, e.getMessage());
			} finally {
				mTile = null;
			}
			return result == QueryResult.SUCCESS;
		}

		@Override
		public void setTileImage(Bitmap bitmap) {
			if (isCanceled() || mTile.state(CANCEL))
				throw new CancellationException();

			BitmapLayer l = new BitmapLayer(false);
			l.setBitmap(bitmap, Tile.SIZE, Tile.SIZE);
			mTile.layers = new ElementLayers();
			mTile.layers.setTextureLayers(l);
		}

		@Override
		public void process(MapElement element) {

		}

		@Override
		public void completed(boolean success) {

		}
	}

}
