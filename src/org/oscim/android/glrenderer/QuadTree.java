/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.android.glrenderer;

import android.util.Log;

class QuadTree {
	private static String TAG = "QuadTree";

	// pointer to tile 0/0/0
	private static QuadTree root;

	// parent pointer is used to link pool items
	private static QuadTree pool;

	QuadTree parent;
	// .... x y
	// 0 => 0 0
	// 1 => 1 0
	// 2 => 0 1
	// 3 => 1 1
	final QuadTree[] child = new QuadTree[4];
	int refs = 0;
	byte id;
	MapTile tile;

	static void init() {

		pool = null;
		root = new QuadTree();
		root.parent = root;

		QuadTree t;
		for (int i = 0; i < 200; i++) {
			t = new QuadTree();
			t.parent = pool;
			pool = t;
		}
	}

	static boolean remove(MapTile t) {
		if (t.rel == null) {
			Log.d(TAG, "already removed " + t);
			return true;
		}

		QuadTree cur = t.rel;
		QuadTree next;

		for (; cur != root;) {
			// keep pointer to parent
			next = cur.parent;
			cur.refs--;

			// if current node has no children
			if (cur.refs == 0) {
				// unhook from parent
				next.child[cur.id] = null;

				// add item back to pool
				cur.parent = pool;
				pool = cur;
			}
			cur = next;
		}

		root.refs--;

		t.rel.tile = null;
		t.rel = null;

		return true;
	}

	static QuadTree add(MapTile tile) {

		int x = tile.tileX;
		int y = tile.tileY;
		int z = tile.zoomLevel;

		QuadTree cur;

		// if (x < 0 || x >= 1 << z) {
		// Log.d(TAG, "invalid position");
		// return null;
		// }
		// if (y < 0 || y >= 1 << z) {
		// Log.d(TAG, "invalid position");
		// return null;
		// }

		QuadTree leaf = root;

		for (int level = z - 1; level >= 0; level--) {

			int id = ((x >> level) & 1) | ((y >> level) & 1) << 1;

			leaf.refs++;

			cur = leaf.child[id];

			if (cur != null) {
				leaf = cur;
				continue;
			}

			if (pool != null) {
				cur = pool;
				pool = pool.parent;
			} else {
				cur = new QuadTree();
			}

			cur.refs = 0;
			cur.id = (byte) id;
			cur.parent = leaf;
			cur.parent.child[id] = cur;

			leaf = cur;
		}

		leaf.refs++;
		leaf.tile = tile;
		tile.rel = leaf;

		return leaf;
	}

	static MapTile getTile(int x, int y, int z) {
		QuadTree leaf = root;

		for (int level = z - 1; level >= 0; level--) {

			leaf = leaf.child[((x >> level) & 1) | ((y >> level) & 1) << 1];

			if (leaf == null)
				return null;

			if (level == 0) {
				return leaf.tile;
			}
		}
		return null;
	}
}
