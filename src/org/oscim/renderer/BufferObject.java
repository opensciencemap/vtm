/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oscim.renderer;

import android.opengl.GLES20;

public final class BufferObject {
	private static BufferObject pool;
	static int counter;

	static synchronized BufferObject get() {

		if (pool == null)
			return null;
		counter--;

		BufferObject bo = pool;
		pool = pool.next;
		bo.next = null;
		return bo;
	}

	// static synchronized BufferObject get(int size) {
	// BufferObject bo, prev = null;
	//
	// if (pool == null) {
	// return null;
	// }
	//
	// int max = size * 4;
	//
	// for (bo = pool; bo != null; bo = bo.next) {
	// if (bo.size > size && size < max)
	// break;
	//
	// prev = bo;
	// }
	//
	// if (prev != null && bo != null) {
	// prev.next = bo.next;
	// bo.next = null;
	// return bo;
	// }
	//
	// bo = pool;
	// pool = pool.next;
	// bo.next = null;
	// return bo;
	// }

	static synchronized void release(BufferObject bo) {
		bo.next = pool;
		pool = bo;
		counter++;
	}

	// Note: only call from GL-Thread
	static synchronized int limitUsage(int reduce) {
		int vboIds[] = new int[10];
		BufferObject[] tmp = new BufferObject[10];
		int removed = 0;
		int freed = 0;

		for (BufferObject bo = pool; bo != null; bo = bo.next) {
			if (bo.size > 0) {
				freed += bo.size;
				bo.size = 0;
				vboIds[removed] = bo.id;
				tmp[removed++] = bo;

				if (removed == 10 || reduce < freed)
					break;
			}
		}
		if (removed > 0) {
			GLES20.glDeleteBuffers(removed, vboIds, 0);
			GLES20.glGenBuffers(removed, vboIds, 0);

			for (int i = 0; i < removed; i++)
				tmp[i].id = vboIds[i];
		}

		return freed;
	}

	static void init(int num) {
		int[] mVboIds = new int[num];
		GLES20.glGenBuffers(num, mVboIds, 0);

		BufferObject bo;

		for (int i = 1; i < num; i++) {
			bo = new BufferObject(mVboIds[i]);
			bo.next = pool;
			pool = bo;
		}
		counter = num;
	}

	public int id;
	int size;
	BufferObject next;

	BufferObject(int id) {
		this.id = id;
	}
}
