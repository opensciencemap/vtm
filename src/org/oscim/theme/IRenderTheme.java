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
package org.oscim.theme;

import org.oscim.core.MapElement;
import org.oscim.theme.renderinstruction.RenderInstruction;

public interface IRenderTheme {

	/**
	 * Matches a MapElement with the given parameters against this RenderTheme.
	 *
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 * @return matching render instructions
	 */
	public abstract RenderInstruction[] matchElement(MapElement element, int zoomLevel);


	/**
	 * Must be called when this RenderTheme gets destroyed to clean up and free
	 * resources.
	 */
	public abstract void destroy();

	/**
	 * @return the number of distinct drawing levels required by this
	 *         RenderTheme.
	 */
	public abstract int getLevels();

	/**
	 * @return the map background color of this RenderTheme.
	 */
	public abstract int getMapBackground();

	/**
	 * Scales the stroke width of this RenderTheme by the given factor.
	 *
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public abstract void scaleStrokeWidth(float scaleFactor);

	/**
	 * Scales the text size of this RenderTheme by the given factor.
	 *
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public abstract void scaleTextSize(float scaleFactor);

}
