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
package org.mapsforge.android.mapgenerator;

import org.mapsforge.android.MapRenderer;
import org.mapsforge.android.MapView;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.mapdatabase.IMapDatabase;

/**
 * A MapGenerator provides map tiles either by downloading or rendering them.
 */
public interface MapGenerator {
	/**
	 * Called once at the end of the MapGenerator lifecycle.
	 */
	void cleanup();

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param mapGeneratorJob
	 *            the job that should be executed.
	 * @return true if the job was executed successfully, false otherwise.
	 */
	boolean executeJob(MapGeneratorJob mapGeneratorJob);

	/**
	 * @return the start point of this MapGenerator (may be null).
	 */
	GeoPoint getStartPoint();

	/**
	 * @return the start zoom level of this MapGenerator (may be null).
	 */
	Byte getStartZoomLevel();

	/**
	 * @return the maximum zoom level that this MapGenerator supports.
	 */
	byte getZoomLevelMax();

	/**
	 * @return true if this MapGenerator requires an Internet connection, false otherwise.
	 */
	boolean requiresInternetConnection();

	/**
	 * @param mapView
	 *            the MapView
	 * @return GLSurfaceView Renderer
	 */
	MapRenderer getMapRenderer(MapView mapView);

	/**
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	void setMapDatabase(IMapDatabase mapDatabase);
}
