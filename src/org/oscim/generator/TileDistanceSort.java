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
package org.oscim.generator;

import java.util.Comparator;

/**
 * 
 *
 */
public class TileDistanceSort implements Comparator<JobTile> {

	@Override
	public int compare(JobTile tile1, JobTile tile2) {
		if (tile1.distance == tile2.distance)
			return 0;

		return tile1.distance > tile2.distance ? 1 : -1;
	}
}
