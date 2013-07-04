package org.oscim.gdx;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.layers.overlay.GenericOverlay;
import org.oscim.renderer.layers.GridRenderLayer;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap2.OSciMap2TileSource;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class Main {

	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "vtm-gdx";
		cfg.useGL20 = true;
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil = 8;
		cfg.foregroundFPS = 20;
		// cfg.samples = 4;

		// set our globals
		CanvasAdapter.g = AwtGraphics.INSTANCE;
		GLAdapter.g = new GdxGLAdapter();
		Tile.SIZE = 256;

		new SharedLibraryLoader().load("vtm-jni");

		//TileSource tileSource = new OSciMap4TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/testing-nocache");

		TileSource tileSource = new OSciMap2TileSource();
		tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/map-live");

		new LwjglApplication(new GdxMapDesktop(tileSource), cfg);
	}

	static class GdxMapDesktop extends GdxMap{

		public GdxMapDesktop(TileSource tileSource) {
	        super(tileSource);
        }

		@Override
		public void create() {
		    super.create();

		    //mMapView.getLayerManager().add(new BitmapTileLayer(mMapView, ArcGISWorldShaded.INSTANCE));
			//mMapView.getLayerManager().add(new GenericOverlay(mMapView, new GridRenderLayer(mMapView)));
		}
	}
}
