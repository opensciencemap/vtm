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
package org.oscim.database.mapfile.header;

import org.oscim.core.MercatorProjection;

/**
 * Holds all parameters of a sub-file.
 */
public class SubFileParameter {
	/**
	 * Number of bytes a single index entry consists of.
	 */
	public static final byte BYTES_PER_INDEX_ENTRY = 5;

	/**
	 * Divisor for converting coordinates stored as integers to double values.
	 */
	private static final double COORDINATES_DIVISOR = 1000000d;

	/**
	 * Base zoom level of the sub-file, which equals to one block.
	 */
	public final byte baseZoomLevel;

	/**
	 * Size of the entries table at the beginning of each block in bytes.
	 */
	public final int blockEntriesTableSize;

	/**
	 * Vertical amount of blocks in the grid.
	 */
	public final long blocksHeight;

	/**
	 * Horizontal amount of blocks in the grid.
	 */
	public final long blocksWidth;

	/**
	 * Y number of the tile at the bottom boundary in the grid.
	 */
	public final long boundaryTileBottom;

	/**
	 * X number of the tile at the left boundary in the grid.
	 */
	public final long boundaryTileLeft;

	/**
	 * X number of the tile at the right boundary in the grid.
	 */
	public final long boundaryTileRight;

	/**
	 * Y number of the tile at the top boundary in the grid.
	 */
	public final long boundaryTileTop;

	/**
	 * Absolute end address of the index in the enclosing file.
	 */
	public final long indexEndAddress;

	/**
	 * Absolute start address of the index in the enclosing file.
	 */
	public final long indexStartAddress;

	/**
	 * Total number of blocks in the grid.
	 */
	public final long numberOfBlocks;

	/**
	 * Absolute start address of the sub-file in the enclosing file.
	 */
	public final long startAddress;

	/**
	 * Size of the sub-file in bytes.
	 */
	public final long subFileSize;

	/**
	 * Maximum zoom level for which the block entries tables are made.
	 */
	public final byte zoomLevelMax;

	/**
	 * Minimum zoom level for which the block entries tables are made.
	 */
	public final byte zoomLevelMin;

	/**
	 * Stores the hash code of this object.
	 */
	private final int hashCodeValue;

	SubFileParameter(SubFileParameterBuilder subFileParameterBuilder) {
		this.startAddress = subFileParameterBuilder.startAddress;
		this.indexStartAddress = subFileParameterBuilder.indexStartAddress;
		this.subFileSize = subFileParameterBuilder.subFileSize;
		this.baseZoomLevel = subFileParameterBuilder.baseZoomLevel;
		this.zoomLevelMin = subFileParameterBuilder.zoomLevelMin;
		this.zoomLevelMax = subFileParameterBuilder.zoomLevelMax;
		this.hashCodeValue = calculateHashCode();

		// calculate the XY numbers of the boundary tiles in this sub-file
		this.boundaryTileBottom = MercatorProjection.latitudeToTileY(
				subFileParameterBuilder.boundingBox.minLatitudeE6
						/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileLeft = MercatorProjection.longitudeToTileX(
				subFileParameterBuilder.boundingBox.minLongitudeE6
						/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileTop = MercatorProjection.latitudeToTileY(
				subFileParameterBuilder.boundingBox.maxLatitudeE6
						/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileRight = MercatorProjection.longitudeToTileX(
				subFileParameterBuilder.boundingBox.maxLongitudeE6
						/ COORDINATES_DIVISOR, this.baseZoomLevel);

		// calculate the horizontal and vertical amount of blocks in this sub-file
		this.blocksWidth = this.boundaryTileRight - this.boundaryTileLeft + 1;
		this.blocksHeight = this.boundaryTileBottom - this.boundaryTileTop + 1;

		// calculate the total amount of blocks in this sub-file
		this.numberOfBlocks = this.blocksWidth * this.blocksHeight;

		this.indexEndAddress = this.indexStartAddress + this.numberOfBlocks * BYTES_PER_INDEX_ENTRY;

		// calculate the size of the tile entries table
		this.blockEntriesTableSize = 2 * (this.zoomLevelMax - this.zoomLevelMin + 1) * 2;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof SubFileParameter)) {
			return false;
		}
		SubFileParameter other = (SubFileParameter) obj;
		if (this.startAddress != other.startAddress) {
			return false;
		} else if (this.subFileSize != other.subFileSize) {
			return false;
		} else if (this.baseZoomLevel != other.baseZoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("SubFileParameter [baseZoomLevel=");
		stringBuilder.append(this.baseZoomLevel);
		stringBuilder.append(", blockEntriesTableSize=");
		stringBuilder.append(this.blockEntriesTableSize);
		stringBuilder.append(", blocksHeight=");
		stringBuilder.append(this.blocksHeight);
		stringBuilder.append(", blocksWidth=");
		stringBuilder.append(this.blocksWidth);
		stringBuilder.append(", boundaryTileBottom=");
		stringBuilder.append(this.boundaryTileBottom);
		stringBuilder.append(", boundaryTileLeft=");
		stringBuilder.append(this.boundaryTileLeft);
		stringBuilder.append(", boundaryTileRight=");
		stringBuilder.append(this.boundaryTileRight);
		stringBuilder.append(", boundaryTileTop=");
		stringBuilder.append(this.boundaryTileTop);
		stringBuilder.append(", indexStartAddress=");
		stringBuilder.append(this.indexStartAddress);
		stringBuilder.append(", numberOfBlocks=");
		stringBuilder.append(this.numberOfBlocks);
		stringBuilder.append(", startAddress=");
		stringBuilder.append(this.startAddress);
		stringBuilder.append(", subFileSize=");
		stringBuilder.append(this.subFileSize);
		stringBuilder.append(", zoomLevelMax=");
		stringBuilder.append(this.zoomLevelMax);
		stringBuilder.append(", zoomLevelMin=");
		stringBuilder.append(this.zoomLevelMin);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + (int) (this.startAddress ^ (this.startAddress >>> 32));
		result = 31 * result + (int) (this.subFileSize ^ (this.subFileSize >>> 32));
		result = 31 * result + this.baseZoomLevel;
		return result;
	}
}
