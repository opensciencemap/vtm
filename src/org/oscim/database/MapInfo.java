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

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.database.mapfile.MapDatabase;

/**
 * Contains the immutable metadata of a map file.
 *
 * @see MapDatabase#getMapInfo()
 */
public class MapInfo {
	/**
	 * The bounding box of the map file.
	 */
	public final BoundingBox boundingBox;

	/**
	 * The comment field of the map file (may be null).
	 */
	public final String comment;

	/**
	 * The created by field of the map file (may be null).
	 */
	public final String createdBy;

	/**
	 * The size of the map file, measured in bytes.
	 */
	public final long fileSize;

	/**
	 * The file version number of the map file.
	 */
	public final int fileVersion;

	/**
	 * The preferred language for names as defined in ISO 3166-1 (may be null).
	 */
	public final String languagePreference;

	/**
	 * The center point of the map file.
	 */
	public final GeoPoint mapCenter;

	/**
	 * The date of the map data in milliseconds since January 1, 1970.
	 */
	public final long mapDate;

	/**
	 * The name of the projection used in the map file.
	 */
	public final String projectionName;

	/**
	 * The map start position from the file header (may be null).
	 */
	public final GeoPoint startPosition;

	/**
	 * The map start zoom level from the file header (may be null).
	 */
	public final Byte startZoomLevel;

	/**
	 * Zoomlevels provided by this Database, if null then any zoomlevel can be
	 * queried.
	 */
	public final int[] zoomLevel;

	/**
	 * @param bbox
	 *            ...
	 * @param zoom
	 *            ...
	 * @param start
	 *            ...
	 * @param projection
	 *            ...
	 * @param date
	 *            ...
	 * @param size
	 *            ...
	 * @param version
	 *            ...
	 * @param language
	 *            ...
	 * @param comment
	 *            ...
	 * @param createdBy
	 *            ...
	 * @param zoomLevel
	 *            TODO
	 */
	public MapInfo(BoundingBox bbox, Byte zoom, GeoPoint start, String projection,
			long date, long size, int version, String language, String comment,
			String createdBy, int[] zoomLevel) {

		this.startZoomLevel = zoom;
		this.zoomLevel = zoomLevel;
		this.startPosition = start;
		this.projectionName = projection;
		this.mapDate = date;
		this.boundingBox = bbox;
		this.mapCenter = bbox.getCenterPoint();
		this.languagePreference = language;
		this.fileSize = size;
		this.fileVersion = version;

		this.comment = comment;
		this.createdBy = createdBy;

	}
}
