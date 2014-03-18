/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.theme;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.TagSet;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.CircleStyle;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;

public interface IRenderTheme {

	/**
	 * Matches a MapElement with the given parameters against this RenderTheme.
	 * 
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 * @return matching render instructions
	 */
	public abstract RenderStyle[] matchElement(GeometryType type, TagSet tags, int zoomLevel);

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

	public void updateStyles();

	/**
	 * Scales the text size of this RenderTheme by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public abstract void scaleTextSize(float scaleFactor);

	/**
	 * Callback methods for rendering areas, ways and points of interest (POIs).
	 */
	public interface Callback {
		/**
		 * Renders an area with the given parameters.
		 * 
		 * @param area
		 * @param level
		 */
		void renderArea(AreaStyle area, int level);

		/**
		 * Renders an extrusion with the given parameters.
		 * 
		 * @param extrusion
		 * @param level
		 */
		void renderExtrusion(ExtrusionStyle extrusion, int level);

		/**
		 * Renders a point of interest circle with the given parameters.
		 * 
		 * @param circle
		 *            the circle.
		 * @param level
		 *            the drawing level on which the circle should be rendered.
		 */
		void renderCircle(CircleStyle circle, int level);

		/**
		 * Renders a point of interest symbol with the given bitmap.
		 * 
		 * @param symbol
		 *            the symbol to be rendered.
		 */
		void renderSymbol(SymbolStyle symbol);

		/**
		 * Renders a way with the given parameters.
		 * 
		 * @param line
		 * @param level
		 */
		void renderWay(LineStyle line, int level);

		/**
		 * Renders a way with the given text along the way path.
		 * 
		 * @param text
		 */
		void renderText(TextStyle text);

	}

	public static class ThemeException extends IllegalArgumentException {
		public ThemeException(String string) {
			super(string);
		}

		private static final long serialVersionUID = 1L;
	}
}
