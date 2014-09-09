/*
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
package org.oscim.core;

/**
 * The Class Box.
 */
public class Box {

	/** The min x. */
	public double xmin;

	/** The max x. */
	public double xmax;

	/** The min y. */
	public double ymin;

	/** The max y. */
	public double ymax;

	/**
	 * Instantiates a new Box with all values being 0.
	 */
	public Box() {

	}

	/**
	 * Instantiates a new Box.
	 * 
	 * @param xmin the min x
	 * @param ymin the min y
	 * @param xmax the max x
	 * @param ymax the max y
	 */
	public Box(double xmin, double ymin, double xmax, double ymax) {
		this.xmin = xmin;
		this.ymin = ymin;
		this.xmax = xmax;
		this.ymax = ymax;
	}

	public Box(Box bbox) {
		this.xmin = bbox.xmin;
		this.ymin = bbox.ymin;
		this.xmax = bbox.xmax;
		this.ymax = bbox.ymax;
	}

	/**
	 * Check if Box contains point defined by coordinates x and y.
	 * 
	 * @param x the x ordinate
	 * @param y the y ordinate
	 * @return true, if point is inside box.
	 */
	public boolean contains(double x, double y) {
		return (x >= xmin && x <= ymax && y >= ymin && y <= ymax);
	}

	/**
	 * Check if Box contains Point.
	 */
	public boolean contains(Point p) {
		return (p.x >= xmin && p.x <= ymax && p.y >= ymin && p.y <= ymax);
	}

	/**
	 * Check if this Box is inside box.
	 */
	public boolean inside(Box box) {
		return xmin >= box.xmin && xmax <= box.xmax && ymin >= box.ymin && ymax <= box.ymax;
	}

	public double getWidth() {
		return xmax - xmin;
	}

	public double getHeight() {
		return ymax - ymin;
	}

	public boolean overlap(Box other) {
		return !(xmin > other.xmax || xmax < other.xmin || ymin > other.ymax || ymax < other.ymin);
	}

	@Override
	public String toString() {
		return "[" + xmin + ',' + ymin + ',' + xmax + ',' + ymax + ']';
	}

}
