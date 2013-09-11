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
package org.oscim.tiling.source.mapfile;

class QueryParameters {
	long fromBaseTileX;
	long fromBaseTileY;
	long fromBlockX;
	long fromBlockY;
	int queryTileBitmask;
	int queryZoomLevel;
	long toBaseTileX;
	long toBaseTileY;
	long toBlockX;
	long toBlockY;
	boolean useTileBitmask;

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("QueryParameters [fromBaseTileX=");
		stringBuilder.append(this.fromBaseTileX);
		stringBuilder.append(", fromBaseTileY=");
		stringBuilder.append(this.fromBaseTileY);
		stringBuilder.append(", fromBlockX=");
		stringBuilder.append(this.fromBlockX);
		stringBuilder.append(", fromBlockY=");
		stringBuilder.append(this.fromBlockY);
		stringBuilder.append(", queryTileBitmask=");
		stringBuilder.append(this.queryTileBitmask);
		stringBuilder.append(", queryZoomLevel=");
		stringBuilder.append(this.queryZoomLevel);
		stringBuilder.append(", toBaseTileX=");
		stringBuilder.append(this.toBaseTileX);
		stringBuilder.append(", toBaseTileY=");
		stringBuilder.append(this.toBaseTileY);
		stringBuilder.append(", toBlockX=");
		stringBuilder.append(this.toBlockX);
		stringBuilder.append(", toBlockY=");
		stringBuilder.append(this.toBlockY);
		stringBuilder.append(", useTileBitmask=");
		stringBuilder.append(this.useTileBitmask);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
