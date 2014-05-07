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
package org.oscim.backend;

public class GLAdapter {

	public final static boolean debug = false;
	public final static boolean debugView = false;

	/** The instance provided by backend */
	private static GL20 g;

	public static boolean GDX_DESKTOP_QUIRKS;
	public static boolean GDX_WEBGL_QUIRKS;

	public static boolean NON_PREMUL_CANVAS;

	/**
	 * Set false as workaround for adreno driver issue:
	 * https://github.com/opensciencemap/vtm/issues/52
	 */
	public static boolean VBO_TEXTURE_LAYERS = false;

	public static GL20 get() {
		if (g == null)
			throw new IllegalStateException();

		return g;
	}

	public static void init(GL20 gl20) {
		g = gl20;
	}
}
