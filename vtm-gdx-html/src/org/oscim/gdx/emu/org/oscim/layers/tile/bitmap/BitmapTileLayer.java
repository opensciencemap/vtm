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

import org.oscim.backend.Log;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.gdx.client.GwtBitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.renderer.sublayers.BitmapLayer;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.view.MapView;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

public class BitmapTileLayer extends TileLayer<TileLoader> {
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_READ = 10000;

	final TileSource mTileSource;

	public BitmapTileLayer(MapView mapView, TileSource tileSource) {
		super(mapView, tileSource.getZoomLevelMax());
		mTileSource = tileSource;
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
						Log.d("sup", "got image " + url);

						Bitmap bitmap = new GwtBitmap(img);
						tile.layers = new Layers();
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
					Log.d("sup", "load image " + url);
					loadImage(tile, url.toString());
				} catch (Exception e) {
					e.printStackTrace();
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
