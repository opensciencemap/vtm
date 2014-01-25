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

package org.oscim.backend.canvas;

import org.oscim.utils.FastMath;

public class Color {

	public static int fade(int color, double alpha) {
		alpha = FastMath.clamp(alpha, 0, 1);

		alpha *= (color >>> 24) & 0xff;
		int c = (((int) alpha) & 0xff) << 24;

		alpha /= 255;

		c |= ((int) (alpha * ((color >>> 16) & 0xff))) << 16;
		c |= ((int) (alpha * ((color >>> 8) & 0xff))) << 8;
		c |= ((int) (alpha * (color & 0xff)));

		return c;
	}

	public static int rainbow(float pos) {
		float i = 255 * pos;
		int r = (int) Math.round(Math.sin(0.024 * i + 0) * 127 + 128);
		int g = (int) Math.round(Math.sin(0.024 * i + 2) * 127 + 128);
		int b = (int) Math.round(Math.sin(0.024 * i + 4) * 127 + 128);
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}

	/**
	 * Pack r, g, b bytes into one int.
	 */
	public static int get(int r, int g, int b) {
		return 0xff << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Pack premultiplied a, r, g, b bytes into one int.
	 */
	public static int get(int a, int r, int g, int b) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Pack r, g, b bytes into one int with premultiplied alpha a.
	 */
	public static int get(float a, int r, int g, int b) {
		return fade(0xff << 24 | r << 16 | g << 8 | b, a);
	}

	public static float rToFloat(int color) {
		return ((color >>> 16) & 0xff) / 255f;
	}

	public static float gToFloat(int color) {
		return ((color >>> 8) & 0xff) / 255f;
	}

	public static float bToFloat(int color) {
		return ((color) & 0xff) / 255f;
	}

	public static float aToFloat(int color) {
		return ((color >>> 24) & 0xff) / 255f;
	}

	/*
	 * Copyright (C) 2006 The Android Open Source Project
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	public static final int BLACK = 0xFF000000;
	public static final int DKGRAY = 0xFF444444;
	public static final int GRAY = 0xFF888888;
	public static final int LTGRAY = 0xFFCCCCCC;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int RED = 0xFFFF0000;
	public static final int GREEN = 0xFF00FF00;
	public static final int BLUE = 0xFF0000FF;
	public static final int YELLOW = 0xFFFFFF00;
	public static final int CYAN = 0xFF00FFFF;
	public static final int MAGENTA = 0xFFFF00FF;
	public static final int TRANSPARENT = 0;

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
}
