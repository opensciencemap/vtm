/*
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
package org.oscim.core;

/**
 * The Class Box.
 */
public class Box {

	/** The min x. */
	public double minX;

	/** The max x. */
	public double maxX;

	/** The min y. */
	public double minY;

	/** The max y. */
	public double maxY;

	/**
	 * Instantiates a new Box with all values being 0.
	 */
	public Box() {

	}

	/**
	 * Instantiates a new Box.
	 *
	 * @param minX the min x
	 * @param minY the min y
	 * @param maxX the max x
	 * @param maxY the max y
	 */
	public Box(double minX, double minY, double maxX, double maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	/**
	 * Check if Box contains point defined by coordinates x and y.
	 *
	 * @param x the x ordinate
	 * @param y the y ordinate
	 * @return true, if point is inside box.
	 */
	public boolean contains(double x, double y) {
		return (x >= minX && x <= maxY && y >= minY && y <= maxY);
	}

	/**
	 * Check if Box contains Point.
	 *
	 * @param p the point
	 * @return true, if point is inside box.
	 */
	public boolean contains(Point p) {
		return (p.x >= minX && p.x <= maxY && p.y >= minY && p.y <= maxY);
	}
}
