/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.renderer.elements.VertexData.Chunk;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO override append() etc to update internal (cur) state.
 */
public class VertexData extends Inlist.List<Chunk> {
	static final Logger log = LoggerFactory.getLogger(VertexData.class);

	public static class Chunk extends Inlist<Chunk> {
		public final short[] vertices = new short[SIZE];
		public int used;
	};

	private static class Pool extends SyncPool<Chunk> {
		public Pool() {
			super(MAX_POOL);
		}

		@Override
		protected Chunk createItem() {
			return new Chunk();
		}

		@Override
		protected boolean clearItem(Chunk it) {
			it.used = 0;
			return true;
		}
	}

	public int countSize() {
		if (cur == null)
			return 0;

		cur.used = used;

		int size = 0;
		for (Chunk it = head(); it != null; it = it.next)
			size += it.used;

		return size;
	}

	@Override
	public Chunk clear() {
		if (cur == null)
			return null;

		cur.used = used;
		used = SIZE; /* set SIZE to get new item on add */
		cur = null;
		vertices = null;

		return super.clear();
	}

	private static final int MAX_POOL = 500;

	private final static Pool pool = new Pool();

	/* Must be multiple of
	 * 4 (LineLayer/PolygonLayer),
	 * 24 (TexLineLayer - one block, i.e. two segments)
	 * 24 (TextureLayer) */
	public static final int SIZE = 360;

	public void dispose() {
		pool.releaseAll(super.clear());
		used = SIZE; /* set SIZE to get new item on add */
		cur = null;
		vertices = null;
	}

	public int compile(ShortBuffer sbuf) {
		if (cur == null)
			return 0;

		cur.used = used;

		int size = 0;
		for (Chunk it = head(); it != null; it = it.next) {
			size += it.used;
			sbuf.put(it.vertices, 0, it.used);
		}
		dispose();
		//log.debug("compiled {}", size);
		return size;
	}

	private Chunk cur;

	/* set SIZE to get new item on add */
	private int used = SIZE;

	private short[] vertices;

	private void getNext() {
		if (cur == null) {
			cur = pool.get();
			push(cur);
		} else {
			if (cur.next != null)
				throw new IllegalStateException("seeeked...");

			cur.used = SIZE;
			cur.next = pool.get();
			cur = cur.next;
		}
		vertices = cur.vertices;
		used = 0;
	}

	public void add(short a) {
		if (used == SIZE)
			getNext();

		vertices[used++] = a;
	}

	public void add(short a, short b) {
		if (used == SIZE)
			getNext();

		vertices[used + 0] = a;
		vertices[used + 1] = b;
		used += 2;
	}

	public void add(short a, short b, short c) {
		if (used == SIZE)
			getNext();

		vertices[used + 0] = a;
		vertices[used + 1] = b;
		vertices[used + 2] = c;
		used += 3;
	}

	public void add(short a, short b, short c, short d) {
		if (used == SIZE)
			getNext();

		vertices[used + 0] = a;
		vertices[used + 1] = b;
		vertices[used + 2] = c;
		vertices[used + 3] = d;
		used += 4;
	}

	public void add(short a, short b, short c, short d, short e, short f) {
		if (used == SIZE)
			getNext();

		vertices[used + 0] = a;
		vertices[used + 1] = b;
		vertices[used + 2] = c;
		vertices[used + 3] = d;
		vertices[used + 4] = e;
		vertices[used + 5] = f;
		used += 6;
	}

	public static VertexData get() {
		return new VertexData();
	}

	/** When changing the position releaseChunk to update internal state */
	public Chunk obtainChunk() {
		if (used == SIZE)
			getNext();
		cur.used = used;

		return cur;
	}

	public void releaseChunk() {
		used = cur.used;
	}

	public void seek(int offset) {
		used += offset;
		cur.used = used;

		if (used > SIZE || used < 0)
			throw new IllegalStateException("seekkeed: " + offset + ":" + used);
	}

	public boolean empty() {
		return cur == null;
	}

}
