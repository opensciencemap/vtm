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
package org.oscim.web.client;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.client.GwtGdxGraphics;
import org.oscim.gdx.client.UrlUpdater;
import org.oscim.renderer.MapRenderer;
import org.oscim.web.js.JsMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.google.gwt.user.client.Window;

public class GwtMap extends GdxMap {
	static final Logger log = LoggerFactory.getLogger(GwtMap.class);

	@Override
	public void create() {

		GwtGdxGraphics.init();
		GdxAssets.init("");
		CanvasAdapter.textScale = 0.7f;
		GLAdapter.init((GL20) Gdx.graphics.getGL20());
		GLAdapter.GDX_WEBGL_QUIRKS = true;
		MapRenderer.setBackgroundColor(0xffffff);

		JsMap.init(mMap);

		// stroke text takes about 70% cpu time in firefox:
		// https://bug568526.bugzilla.mozilla.org/attachment.cgi?id=447932
		// <- circle/stroke test 800ms firefox, 80ms chromium..
		// TODO use texture atlas to avoid drawing text-textures
		if (GwtApplication.agentInfo().isLinux() &&
		        GwtApplication.agentInfo().isFirefox())
			GwtGdxGraphics.NO_STROKE_TEXT = true;

		MapConfig c = MapConfig.get();
		super.create();

		double lat = c.getLatitude();
		double lon = c.getLongitude();
		int zoom = c.getZoom();

		float tilt = 0;
		float rotation = 0;

		if (Window.Location.getHash() != null) {
			String hash = Window.Location.getHash();

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
		p.bearing = rotation;
		p.tilt = tilt;
		mMap.setMapPosition(p);

		UrlUpdater urlUpdater = new UrlUpdater(mMap);
		urlUpdater.scheduleRepeating(4000);
	}

	private final native void createLayersN()/*-{
		$wnd.createLayers();
	}-*/;

	@Override
	protected void createLayers() {
		log.debug("<<< create layers >>>");
		createLayersN();
	}
}
