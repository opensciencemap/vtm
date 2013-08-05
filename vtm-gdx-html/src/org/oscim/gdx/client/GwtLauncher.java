package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.gdx.GdxMap;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.bitmap.NaturalEarth;
import org.oscim.renderer.GLRenderer;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap2.OSciMap2TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;
import org.oscim.view.MapView;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtGraphics;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class GwtLauncher extends GwtApplication {

	@Override
	public GwtApplicationConfiguration getConfig() {
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(
				GwtGraphics.getWindowWidthJSNI(),
				GwtGraphics.getWindowHeightJSNI());

		DockLayoutPanel p = new DockLayoutPanel(Unit.EM);
		p.setHeight("100%");
		p.setWidth("100%");

		RootPanel.get().add(p);

		//HTML header = new HTML("header");
		//p.addNorth(header, 2);
		//header.setStyleName("header");

		//HTML footer = new HTML("footer");
		//footer.setStyleName("footer");
		//p.addSouth(footer, 2);

		cfg.rootPanel = new FlowPanel();
		p.add(cfg.rootPanel);

		cfg.stencil = true;
		cfg.fps = 30;

		return cfg;
	}

	@Override
	public ApplicationListener getApplicationListener() {
		if (GwtGraphics.getDevicePixelRatioJSNI() > 1)
			Tile.SIZE = 400;
		else
			Tile.SIZE = 360;

		return new GwtGdxMap();
	}

	@Override
	public PreloaderCallback getPreloaderCallback() {
		return new PreloaderCallback() {

			@Override
			public void update(PreloaderState state) {
			}

			@Override
			public void error(String file) {
				Log.d(this.getClass().getName(), "error loading " + file);
			}
		};
	}

	private static native String getMapConfig(String key)/*-{
		return $wnd.mapconfig && $wnd.mapconfig[key] || null;
	}-*/;

	class GwtGdxMap extends GdxMap {

		@Override
		public void create() {
			// stroke text takes about 70% cpu time in firefox:
			// https://bug568526.bugzilla.mozilla.org/attachment.cgi?id=447932
			// <- circle/stroke test 800ms firefox, 80ms chromium..
			// TODO use texture atlas to avoid drawing text-textures
			if (GwtApplication.agentInfo().isLinux() && GwtApplication.agentInfo().isFirefox())
				GwtCanvasAdapter.NO_STROKE_TEXT = true;

			CanvasAdapter.g = GwtCanvasAdapter.INSTANCE;
			GLAdapter.g = (GL20) Gdx.graphics.getGL20();
			GLAdapter.GDX_WEBGL_QUIRKS = true;
			GLRenderer.setBackgroundColor(0xffffff);
			//Gdx.app.setLogLevel(Application.LOG_DEBUG);

			super.create();

			double lat = Double.parseDouble(getMapConfig("latitude"));
			double lon = Double.parseDouble(getMapConfig("longitude"));
			int zoom = Integer.parseInt(getMapConfig("zoom"));
			float tilt = 0;
			float rotation = 0;

			if (Window.Location.getHash() != null) {
				String hash = Window.Location.getHash();
				//Log.d("...", "hash: " + hash);
				hash = hash.substring(1);
				String[] pairs = hash.split(",");

				for (String p : pairs) {
					try {
						if (p.startsWith("lat="))
							lat = Double.parseDouble(p.substring(4));
						else if (p.startsWith("lon="))
							lon = Double.parseDouble(p.substring(4));
						else if (p.startsWith("scale="))
							zoom = Integer.parseInt(p.substring(6));
						else if (p.startsWith("rot="))
							rotation = Float.parseFloat(p.substring(4));
						else if (p.startsWith("tilt="))
							tilt = Float.parseFloat(p.substring(5));
					} catch (NumberFormatException e) {

					}
				}
			}
			MapPosition p = new MapPosition();
			p.setZoomLevel(zoom);
			p.setPosition(lat, lon);
			mMapView.setMapPosition(p);

			mMapView.getMapViewPosition().setTilt(tilt);
			mMapView.getMapViewPosition().setRotation(rotation);

			String url = getMapConfig("tileurl");

			TileSource tileSource;
			if ("oscimap4".equals(getMapConfig("tilesource")))
				tileSource = new OSciMap4TileSource();
			else
				tileSource = new OSciMap2TileSource();
			tileSource.setOption("url", url);

			initDefaultMap(tileSource, false, true, true);

			mMapView.getLayerManager().add(new UrlPositionUpdate(mMapView));
			mMapView.setBackgroundMap(new BitmapTileLayer(mMapView, NaturalEarth.INSTANCE));

		}
	}

	class UrlPositionUpdate extends Layer {

		public UrlPositionUpdate(MapView mapView) {
			super(mapView);
		}

		private int curLon, curLat, curZoom, curTilt, curRot;

		@Override
		public void onUpdate(MapPosition pos, boolean changed, boolean clear) {
			if (!changed)
				return;
			int lat = (int) (MercatorProjection.toLatitude(pos.y) * 1000);
			int lon = (int) (MercatorProjection.toLongitude(pos.x) * 1000);
			int rot = (int) (pos.angle);
			rot = (int) (pos.angle) % 360;
			rot = rot < 0 ? -rot : rot;

			if (curZoom != pos.zoomLevel
					|| curLat != lat
					|| curLon != lon
					|| curTilt != rot
					|| curRot != (int) (pos.angle)) {

				curLat = lat;
				curLon = lon;
				curZoom = pos.zoomLevel;
				curTilt = (int) pos.tilt;
				curRot = rot;

				//String newURL = Window.Location.createUrlBuilder()
				//		.setHash("scale=" + pos.zoomLevel + ",rot=" + curRot + ",tilt=" + 
				// curTilt + ",lat=" + (curLat / 1000f) + ",lon=" + (curLon / 1000f))
				//		.buildString();
				//Window.Location.replace(newURL);
				//Window.Location.
			}
		}
	}
}
