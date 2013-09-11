/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers.tile.bitmap;

import java.net.URL;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.gdx.client.GwtBitmap;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.bitmap.TileSource.FadeStep;
import org.oscim.map.Map;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.utils.FastMath;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public class BitmapTileLayer extends TileLayer<TileLoader> {

	final TileSource mTileSource;
	private final FadeStep[] mFade;

	public BitmapTileLayer(Map map, TileSource tileSource) {
		super(map, tileSource.getZoomLevelMin(), tileSource.getZoomLevelMax(), 100);
		mTileSource = tileSource;
		mFade = mTileSource.getFadeSteps();
	}

	@Override
	public void onUpdate(MapPosition pos, boolean changed, boolean clear) {
		super.onUpdate(pos, changed, clear);

		if (mFade == null) {
			mRenderLayer.setBitmapAlpha(1);
			return;
		}

		float alpha = 0;
		for (FadeStep f : mFade) {
			if (pos.scale < f.scaleStart || pos.scale > f.scaleEnd)
				continue;

			if (f.alphaStart == f.alphaEnd) {
				alpha = f.alphaStart;
				break;
			}
			double range = f.scaleEnd / f.scaleStart;
			float a = (float)((range - (pos.scale / f.scaleStart)) / range);
			a = FastMath.clamp(a, 0, 1);
			// interpolate alpha between start and end
			alpha = a * f.alphaStart + (1 - a) * f.alphaEnd;
			break;
		}

		mRenderLayer.setBitmapAlpha(alpha);
	}

	@Override
	protected TileLoader createLoader(TileManager tm) {
		return new TileLoader(tm) {

			private void loadImage(final MapTile tile, final String url) {
				final Image img = new Image(url);
				RootPanel.get().add(img);
				img.setVisible(false);

				img.addLoadHandler(new LoadHandler() {
					public void onLoad(LoadEvent event) {

						Bitmap bitmap = new GwtBitmap(img);
						tile.layers = new ElementLayers();
						BitmapLayer l = new BitmapLayer(false);
						l.setBitmap(bitmap, Tile.SIZE, Tile.SIZE);

						tile.layers.textureLayers = l;
						tile.loader.jobCompleted(tile, true);
					}
				});

				img.addErrorHandler(new ErrorHandler() {

					@Override
					public void onError(ErrorEvent event) {
						tile.loader.jobCompleted(tile, false);
					}
				});
			}

			@Override
			protected boolean executeJob(MapTile tile) {
				URL url;
				try {
					url = mTileSource.getTileUrl(tile);
					loadImage(tile, url.toString());
				} catch (Exception e) {
					e.printStackTrace();
					tile.loader.jobCompleted(tile, false);
					return false;
				}

				return false;
			}

			@Override
			public void cleanup() {
			}
		};
	}
}
