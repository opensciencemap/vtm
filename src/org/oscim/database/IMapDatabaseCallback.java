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
package org.oscim.database;

import org.oscim.core.Tag;
import org.oscim.database.mapfile.MapDatabase;

/**
 * Callback methods which can be triggered from the {@link MapDatabase}.
 * ____
 * NOTE: All parameters passed belong to the caller! i.e. dont hold
 * references to any arrays after callback function returns.
 */
public interface IMapDatabaseCallback {
	/**
	 * Renders a single point of interest node (POI).
	 *
	 * @param layer
	 *            the layer of the node.
	 * @param tags
	 *            the tags of the node.
	 * @param latitude
	 *            the latitude of the node.
	 * @param longitude
	 *            the longitude of the node.
	 */
	void renderPointOfInterest(byte layer, Tag[] tags, float latitude, float longitude);

	/**
	 * Renders water background for the current tile.
	 */
	void renderWaterBackground();

	/**
	 * Renders a single way or area (closed way).
	 *
	 * @param layer
	 *            the osm layer of the way.
	 * @param tags
	 *            the tags of the way.
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes in the order
	 *            longitude/latitude or x/y depending on the projection.
	 * @param wayLength
	 *            length of way data in wayNodes
	 * @param closed
	 *            wheter the way is an polygon.
	 * @param prio TODO
	 */
	void renderWay(byte layer, Tag[] tags, float[] wayNodes, short[] wayLength,
			boolean closed, int prio);

	/**
	 * TBD: check if way will be rendered before decoding
	 *
	 * @param tags
	 *            ...
	 * @param closed
	 *            ...
	 * @return true if the way will be rendered (i.e. found match in
	 *         RenderTheme)
	 */
	boolean checkWay(Tag[] tags, boolean closed);

}
