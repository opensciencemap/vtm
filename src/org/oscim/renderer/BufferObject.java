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
import android.util.Log;

public final class BufferObject {
	private final static String TAG = BufferObject.class.getName();

	private static BufferObject pool;
	static int counter = 0;

	public static synchronized BufferObject get(int size) {

		if (pool == null) {
			if (counter != 0)
				Log.d(TAG, "missing BufferObjects: " + counter);

			createBuffers(10);
			counter += 10;
		}
		counter--;

		if (size != 0) {
			// find an item that has bound more than 'size' bytes.
			// this has the advantage that either memory can be reused or
			// a large unused block will be replaced by a smaller one.
			BufferObject prev = null;
			for (BufferObject bo = pool; bo != null; bo = bo.next) {
				if (bo.size > size) {
					if (prev == null)
						pool = bo.next;
					else
						prev.next = bo.next;

					bo.next = null;
					return bo;
				}
				prev = bo;
			}
		}
		BufferObject bo = pool;
		pool = pool.next;
		bo.next = null;
		return bo;
	}

	public static synchronized void release(BufferObject bo) {
		if (bo == null)
			return;

		//if (counter > 200) {
		//	Log.d(TAG, "should clear some buffers " + counter);
		//}

		bo.next = pool;
		pool = bo;
		counter++;
	}

	// Note: only call from GL-Thread
	static synchronized int limitUsage(int reduce) {
		if (pool == null) {
			Log.d(TAG, "nothing to free");
			return 0;
		}
		int vboIds[] = new int[10];
		int removed = 0;
		int freed = 0;
		BufferObject prev = pool;

		for (BufferObject bo = pool.next; bo != null;) {
			if (bo.size > 0) {
				freed += bo.size;
				bo.size = 0;
				vboIds[removed++] = bo.id;
				prev.next = bo.next;
				bo = bo.next;
				if (removed == 10 || reduce < freed)
					break;

			} else {
				prev = bo;
				bo = bo.next;
			}
		}
		if (removed > 0) {
			GLES20.glDeleteBuffers(removed, vboIds, 0);
			counter -= removed;
		}

		return freed;
	}

	static void createBuffers(int num) {
		int[] mVboIds = new int[num];
		GLES20.glGenBuffers(num, mVboIds, 0);

		for (int i = 0; i < num; i++) {
			BufferObject bo = new BufferObject(mVboIds[i]);
			bo.next = pool;
			pool = bo;
		}
	}

	static synchronized void init(int num) {
		pool = null;
		createBuffers(num);
		counter = num;
	}

	public int id;
	public int size;
	BufferObject next;

	BufferObject(int id) {
		this.id = id;
	}
}
