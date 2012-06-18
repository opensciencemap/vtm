/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.rendertheme;

import java.io.InputStream;

import org.mapsforge.android.maps.mapgenerator.JobTheme;

/**
 * Enumeration of all internal rendering themes.
 */
public enum InternalRenderTheme implements JobTheme {
	/**
	 * A rendering theme similar to the OpenStreetMap Osmarender style.
	 * 
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Osmarender">Osmarender</a>
	 */
	OSMARENDER("/org/mapsforge/android/maps/rendertheme/osmarender/osmarender.xml");

	private final String mPath;

	private InternalRenderTheme(String path) {
		mPath = path;
	}

	@Override
	public InputStream getRenderThemeAsStream() {
		return Thread.currentThread().getClass().getResourceAsStream(mPath);
	}
}
