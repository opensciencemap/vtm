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
package org.mapsforge.android.maps.rendertheme.renderinstruction;

import android.graphics.Typeface;

enum FontFamily {
	DEFAULT, DEFAULT_BOLD, MONOSPACE, SANS_SERIF, SERIF;

	/**
	 * @return the typeface object of this FontFamily.
	 * @see <a href="http://developer.android.com/reference/android/graphics/Typeface.html">Typeface</a>
	 */
	Typeface toTypeface() {
		switch (this) {
			case DEFAULT:
				return Typeface.DEFAULT;
			case DEFAULT_BOLD:
				return Typeface.DEFAULT_BOLD;
			case MONOSPACE:
				return Typeface.MONOSPACE;
			case SANS_SERIF:
				return Typeface.SANS_SERIF;
			case SERIF:
				return Typeface.SERIF;
		}

		throw new IllegalArgumentException("unknown enum value: " + this);
	}
}
