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
package org.oscim.utils.quadtree;

public abstract class QuadTreeIndex<T> {

	QuadTree<T> pool;

	QuadTree<T> root;

	public QuadTreeIndex() {
		root = new QuadTree<T>();
		root.parent = root;
	}

	static void checkIndex(int x, int y, int max) {
		if (x < 0 || x >= max || y < 0 || y >= max) {
			throw new IllegalArgumentException("invalid position " + x + '/' + y + '/' + (max >> 1));
		}
	}

	public abstract T create(int x, int y, int z);

	public abstract void remove(T item);

	public QuadTree<T> add(int x, int y, int z) {

		checkIndex(x, y, 1 << z);

		QuadTree<T> leaf = root;

		for (int level = z - 1; level >= 0; level--) {

			int id = ((x >> level) & 1) | ((y >> level) & 1) << 1;

			leaf.refs++;

			QuadTree<T> cur = null;

			switch (id) {
				case 0:
					cur = leaf.child00;
					break;
				case 1:
					cur = leaf.child01;
					break;
				case 2:
					cur = leaf.child10;
					break;
				case 3:
					cur = leaf.child11;
					break;
			}

			if (cur != null) {
				leaf = cur;
				continue;
			}

			if (pool != null) {
				cur = pool;
				pool = pool.parent;
			} else {
				cur = new QuadTree<T>();
			}

			cur.refs = 0;
			cur.id = (byte) id;
			cur.parent = leaf;

			switch (id) {
				case 0:
					cur.parent.child00 = cur;
					break;
				case 1:
					cur.parent.child01 = cur;
					break;
				case 2:
					cur.parent.child10 = cur;
					break;
				case 3:
					cur.parent.child11 = cur;
					break;
			}

			leaf = cur;
		}

		leaf.refs++;

		return leaf;
	}

	public T getTile(int x, int y, int z) {

		checkIndex(x, y, 1 << z);

		QuadTree<T> leaf = root;

		for (int level = z - 1; level >= 0; level--) {

			int id = ((x >> level) & 1) | ((y >> level) & 1) << 1;

			switch (id) {
				case 0:
					leaf = leaf.child00;
					break;
				case 1:
					leaf = leaf.child01;
					break;
				case 2:
					leaf = leaf.child10;
					break;
				case 3:
					leaf = leaf.child11;
					break;
			}

			if (leaf == null)
				return null;

			if (level == 0) {
				return leaf.item;
			}
		}
		return null;
	}

	public boolean remove(QuadTree<T> item) {

		QuadTree<T> cur = item;
		QuadTree<T> next;

		while (cur != root) {
			if (cur == null)
				throw new IllegalArgumentException("QuadTree.remove: item not in index");

			// keep pointer to parent
			next = cur.parent;
			cur.refs--;

			// if current node has no children
			if (cur.refs == 0) {
				// unhook from parent

				switch (cur.id) {
					case 0:
						next.child00 = null;
						break;
					case 1:
						next.child01 = null;
						break;
					case 2:
						next.child10 = null;
						break;
					case 3:
						next.child11 = null;
						break;
				}

				// add item back to pool
				cur.parent = pool;
				pool = cur;
			}
			cur = next;
		}

		root.refs--;

		return true;
	}
}
