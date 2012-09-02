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
package org.mapsforge.android.glrenderer;

import android.util.Log;

class TreeTile {
	private static String TAG = "TreeTile";

	private static TreeTile root;

	// parent pointer is used to link pool items
	private static TreeTile pool;

	// TreeTile members
	TreeTile parent;
	final TreeTile[] child = new TreeTile[4];
	int refs = 0;
	byte id;
	GLMapTile tile;

	static void init() {

		pool = null;
		root = new TreeTile();
		root.parent = root;

		TreeTile t;
		for (int i = 0; i < 200; i++) {
			t = new TreeTile();
			t.parent = pool;
			pool = t;
		}
	}

	static boolean remove(GLMapTile t) {
		if (t.rel == null) {
			Log.d(TAG, "already removed " + t);
			return true;
		}

		TreeTile cur = t.rel;
		TreeTile next;

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

	static TreeTile add(GLMapTile tile) {

		int x = tile.tileX;
		int y = tile.tileY;
		int z = tile.zoomLevel;

		TreeTile cur;

		// if (x < 0 || x >= 1 << z) {
		// Log.d(TAG, "invalid position");
		// return null;
		// }
		// if (y < 0 || y >= 1 << z) {
		// Log.d(TAG, "invalid position");
		// return null;
		// }

		TreeTile leaf = root;

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
				cur = new TreeTile();
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

	static GLMapTile getTile(int x, int y, int z) {
		TreeTile leaf = root;

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
