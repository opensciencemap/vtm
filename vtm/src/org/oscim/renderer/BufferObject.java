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

	private final static GL20 GL = GLAdapter.get();

	private static final int MB = 1024 * 1024;
	private static final int LIMIT_BUFFERS = 16 * MB;

	// GL identifier
	public int id;
	// allocated bytes
	public int size;

	BufferObject next;

	int target;

	BufferObject(int target, int id) {
		this.id = id;
		this.target = target;
	}

	public void loadBufferData(Buffer buf, int newSize) {
		boolean clear = false;

		if (buf.position() != 0) {
			Log.d(TAG, "rewind your buffer: " + buf.position());
		}

		GL.glBindBuffer(target, id);

		// reuse memory allocated for vbo when possible and allocated
		// memory is less then four times the new data
		if (!clear && (size > newSize) && (size < newSize * 4)) {
			GL.glBufferSubData(target, 0, newSize, buf);
		} else {
			mBufferMemoryUsage += newSize - size;
			size = newSize;
			GL.glBufferData(target, size, buf, GL20.GL_DYNAMIC_DRAW);
		}
	}

	public void bind() {
		GL.glBindBuffer(target, id);
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

	private final static BufferObject pool[] = new BufferObject[2];
	private final static int counter[]  = new int[2];

	public static synchronized BufferObject get(int target, int size) {

		int t = (target == GL20.GL_ARRAY_BUFFER) ? 0 : 1;

		if (pool[t] == null) {
			if (counter[t] != 0)
				Log.d(TAG, "BUG: missing BufferObjects: " + counter);

			createBuffers(target, 10);
			counter[t] += 10;
		}
		counter[t]--;

		if (size != 0) {
			// find an item that has bound more than 'size' bytes.
			// this has the advantage that either memory can be reused or
			// a large unused block will be replaced by a smaller one.
			BufferObject prev = null;
			for (BufferObject bo = pool[t]; bo != null; bo = bo.next) {
				if (bo.size > size) {
					if (prev == null)
						pool[t] = bo.next;
					else
						prev.next = bo.next;

					bo.next = null;
					return bo;
				}
				prev = bo;
			}
		}

		BufferObject bo = pool[t];
		pool[t] = pool[t].next;
		bo.next = null;
		return bo;
	}

	public static synchronized void release(BufferObject bo) {
		if (bo == null)
			return;

		// if (counter > 200) {
		// Log.d(TAG, "should clear some buffers " + counter);
		// }
		int t = (bo.target == GL20.GL_ARRAY_BUFFER) ? 0 : 1;

		bo.next = pool[t];
		pool[t] = bo;
		counter[t]++;
	}

	// Note: only call from GL-Thread
	static synchronized int limitUsage(int reduce) {

		int vboIds[] = new int[10];
		int freed = 0;

		for (int t = 0; t < 2; t++) {

			int removed = 0;
			BufferObject prev = pool[t];

			if (prev == null) {
				Log.d(TAG, "nothing to free");
				return 0;
			}

			for (BufferObject bo = pool[t].next; bo != null;) {
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
				counter[t] -= removed;
			}

		}

		return freed;
	}

	static void createBuffers(int target, int num) {
		int[] mVboIds = GlUtils.glGenBuffers(num);

		int t = (target == GL20.GL_ARRAY_BUFFER) ? 0 : 1;

		for (int i = 0; i < num; i++) {
			BufferObject bo = new BufferObject(target, mVboIds[i]);
			bo.next = pool[t];
			pool[t] = bo;
		}
	}


	static synchronized void clear() {
		mBufferMemoryUsage = 0;

		pool[0] = null;
		pool[1] = null;
		counter[0] = 0;
		counter[1] = 0;
	}

	static synchronized void init(int num) {

		createBuffers(GL20.GL_ARRAY_BUFFER, num);
		counter[0] += num;
	}
}
