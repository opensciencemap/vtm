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
package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
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
		GwtApplicationConfiguration cfg =
		        new GwtApplicationConfiguration(GwtGraphics.getWindowWidthJSNI(),
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
				//log.debug("error loading " + file);
			}
		};
	}
}
