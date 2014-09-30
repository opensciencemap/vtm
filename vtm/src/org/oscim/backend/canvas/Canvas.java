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

/**
 * The Interface Canvas.
 */
public interface Canvas {

	/**
	 * Sets the backing {@link Bitmap}.
	 * 
	 * @param bitmap the new bitmap
	 */
	void setBitmap(Bitmap bitmap);

	/**
	 * Draw text to Canvas.
	 * 
	 * @param string the String
	 * @param x
	 * @param y
	 * @param stroke the stroke
	 * @param
	 */
	void drawText(String string, float x, float y, Paint fill, Paint stroke);

	/**
	 * Draw Bitmap to Canvas.
	 * 
	 * @param bitmap the Bitmap
	 * @param x
	 * @param y
	 */
	void drawBitmap(Bitmap bitmap, float x, float y);

}
