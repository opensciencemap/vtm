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

import java.nio.Buffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.utils.GlUtils;

public final class BufferObject {
	private final static String TAG = BufferObject.class.getName();

	private final static GL20 GL = GLAdapter.INSTANCE;

	private static final int MB = 1024 * 1024;
	private static final int LIMIT_BUFFERS = 16 * MB;

	// GL identifier
	public int id;
	// allocated bytes
	public int size;

	BufferObject next;

	BufferObject(int id) {
		this.id = id;
	}

	int bufferType;

	public void loadBufferData(Buffer buf, int newSize, int type) {
		boolean clear = false;

		if (type != bufferType) {
			if (bufferType != 0)
				clear = true;
			bufferType = type;
		}

		GL.glBindBuffer(type, id);

		// reuse memory allocated for vbo when possible and allocated
		// memory is less then four times the new data
		if (!clear && (size > newSize) && (size < newSize * 4)) {
			GL.glBufferSubData(type, 0, newSize, buf);
		} else {
			mBufferMemoryUsage += newSize - size;

			size = newSize;

			GL.glBufferData(type, size, buf, GL20.GL_DYNAMIC_DRAW);
		}
	}

	public void bindArrayBuffer() {
		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, id);
	}

	public void bindIndexBuffer() {
		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, id);
	}

	// ---------------------------- pool ----------------------------
	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	public static void checkBufferUsage(boolean force) {
		// try to clear some unused vbo when exceding limit
		if (mBufferMemoryUsage < LIMIT_BUFFERS)
			return;

		Log.d(TAG, "buffer object usage: "
				+ mBufferMemoryUsage / MB
				+ "MB, force: " + force);

		mBufferMemoryUsage -= BufferObject.limitUsage(1024 * 1024);

		Log.d(TAG, "now: " + mBufferMemoryUsage / MB + "MB");
	}

	private static BufferObject pool;
	static int counter = 0;

	public static synchronized BufferObject get(int size) {

		if (pool == null) {
			if (counter != 0)
				Log.d(TAG, "BUG: missing BufferObjects: " + counter);

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
			GlUtils.glDeleteBuffers(removed, vboIds);
			counter -= removed;
		}

		return freed;
	}

	static void createBuffers(int num) {
		int[] mVboIds = GlUtils.glGenBuffers(num);

		for (int i = 0; i < num; i++) {
			BufferObject bo = new BufferObject(mVboIds[i]);
			bo.next = pool;
			pool = bo;
		}
	}

	static synchronized void init(int num) {
		mBufferMemoryUsage = 0;
		pool = null;
		createBuffers(num);
		counter = num;
	}
}
