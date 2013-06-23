/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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

import java.io.InputStream;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;


public abstract class CanvasAdapter {
	public static CanvasAdapter g;

	public static float dpi = 240;

	public enum Color {
		BLACK, CYAN, TRANSPARENT, WHITE;
	}

	public abstract Bitmap decodeBitmap(InputStream inputStream);

	public abstract int getColor(Color color);

	public abstract Paint getPaint();

	public abstract int parseColor(String colorString);


	public abstract Bitmap getBitmap(int width, int height, int format);

	public abstract Canvas getCanvas();
}
