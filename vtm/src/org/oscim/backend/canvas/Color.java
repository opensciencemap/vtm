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
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oscim.backend.canvas;

/**
 * The Class Color.
 */
public class Color {

	/** The Constant BLACK. */
	public static final int BLACK = 0xFF000000;

	/** The Constant DKGRAY. */
	public static final int DKGRAY = 0xFF444444;

	/** The Constant GRAY. */
	public static final int GRAY = 0xFF888888;

	/** The Constant LTGRAY. */
	public static final int LTGRAY = 0xFFCCCCCC;

	/** The Constant WHITE. */
	public static final int WHITE = 0xFFFFFFFF;

	/** The Constant RED. */
	public static final int RED = 0xFFFF0000;

	/** The Constant GREEN. */
	public static final int GREEN = 0xFF00FF00;

	/** The Constant BLUE. */
	public static final int BLUE = 0xFF0000FF;

	/** The Constant YELLOW. */
	public static final int YELLOW = 0xFFFFFF00;

	/** The Constant CYAN. */
	public static final int CYAN = 0xFF00FFFF;

	/** The Constant MAGENTA. */
	public static final int MAGENTA = 0xFFFF00FF;

	/** The Constant TRANSPARENT. */
	public static final int TRANSPARENT = 0;

	/**
	 * Pack 8 bit r, g, b into one int.
	 * 
	 * @param r the r
	 * @param g the g
	 * @param b the b
	 * @return the int
	 */
	public static int get(int r, int g, int b) {
		return 0xff << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Parse the color string, and return the corresponding color-int.
	 * If the string cannot be parsed, throws an IllegalArgumentException
	 * exception. Supported formats are:
	 * #RRGGBB
	 * #AARRGGBB
	 * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
	 * 'yellow', 'lightgray', 'darkgray'
	 * 
	 * @param colorString the color string
	 * @return the int
	 */
	public static int parseColor(String colorString) {
		if (colorString.charAt(0) == '#') {
			// Use a long to avoid rollovers on #ffXXXXXX
			long color = Long.parseLong(colorString.substring(1), 16);
			if (colorString.length() == 7) {
				// Set the alpha value
				color |= 0x00000000ff000000;
			} else if (colorString.length() != 9) {
				throw new IllegalArgumentException("Unknown color");
			}
			return (int) color;
		}
		throw new IllegalArgumentException("Unknown color");
	}

	public static int fade(int color, double alpha) {
		alpha *= ((color >>> 24) & 0xff);

		return ((int) alpha) << 24
		        | ((int) alpha * ((color >>> 16) & 0xff)) << 16
		        | ((int) alpha * ((color >>> 8) & 0xff)) << 8
		        | ((int) alpha * ((color) & 0xff));
	}

	public static float rToFloat(int color) {
	    return ((color >>> 16) & 0xff)/255f;
    }
	public static float gToFloat(int color) {
	    return ((color >>> 8) & 0xff)/255f;
    }
	public static float bToFloat(int color) {
	    return ((color) & 0xff)/255f;
    }
	public static float aToFloat(int color) {
	    return ((color >>> 24) & 0xff)/255f;
    }
}
