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
package org.oscim.theme;

import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.Text;

import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * Callback methods for rendering areas, ways and points of interest (POIs).
 */
public interface IRenderCallback {
	/**
	 * Renders an area with the given parameters.
	 *
	 * @param area
	 *            ...
	 * @param level
	 *            ...
	 */
	void renderArea(Area area, int level);

	/**
	 * Renders an area symbol with the given bitmap.
	 *
	 * @param symbol
	 *            the symbol to be rendered.
	 */
	void renderAreaSymbol(Bitmap symbol);

	/**
	 * Renders a point of interest circle with the given parameters.
	 *
	 * @param radius
	 *            the radius of the circle.
	 * @param fill
	 *            the paint to be used for rendering the circle.
	 * @param level
	 *            the drawing level on which the circle should be rendered.
	 */
	void renderPointOfInterestCircle(float radius, Paint fill, int level);

	/**
	 * Renders a point of interest symbol with the given bitmap.
	 *
	 * @param symbol
	 *            the symbol to be rendered.
	 */
	void renderPointOfInterestSymbol(Bitmap symbol);

	/**
	 * Renders a way with the given parameters.
	 *
	 * @param line
	 *            ...
	 * @param level
	 *            ...
	 */
	void renderWay(Line line, int level);

	/**
	 * Renders a way with the given symbol along the way path.
	 *
	 * @param symbol
	 *            the symbol to be rendered.
	 * @param alignCenter
	 *            true if the symbol should be centered, false otherwise.
	 * @param repeat
	 *            true if the symbol should be repeated, false otherwise.
	 */
	void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat);

	/**
	 * Renders a way with the given text along the way path.
	 *
	 * @param text
	 *            ...
	 */
	void renderWayText(Text text);

	/**
	 * Renders an area caption with the given text.
	 *
	 * @param text
	 *            the text to be rendered.
	 */
	void renderAreaCaption(Text text);

	/**
	 * Renders a point of interest caption with the given text.
	 *
	 * @param text
	 *            the text to be rendered.
	 */
	void renderPointOfInterestCaption(Text text);

}
