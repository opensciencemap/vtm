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
package org.oscim.utils.quadtree;

public class QuadTree<T> {
	public QuadTree<T> parent;
	public QuadTree<T> child00;
	public QuadTree<T> child10;
	public QuadTree<T> child01;
	public QuadTree<T> child11;

	public T item;

	// id of this child relative to parent
	byte id;

	// number of children and grandchildren
	int refs = 0;

	public T getParent() {
		return parent.item;
	}

	public T get(int i) {
		switch (i) {
			case 0:
				return (child00 != null) ? child00.item : null;
			case 1:
				return (child01 != null) ? child01.item : null;
			case 2:
				return (child10 != null) ? child10.item : null;
			case 3:
				return (child11 != null) ? child11.item : null;
		}
		return null;
	}
}
