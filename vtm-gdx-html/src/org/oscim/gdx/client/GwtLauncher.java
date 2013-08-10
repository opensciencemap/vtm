package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
import org.oscim.backend.Log;
import org.oscim.core.Tile;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtGraphics;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.google.gwt.dom.client.Style.Unit;
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
}
