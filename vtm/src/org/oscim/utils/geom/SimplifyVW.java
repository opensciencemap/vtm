package org.oscim.utils.geom;

import org.oscim.core.GeometryBuffer;
import org.oscim.utils.GeometryUtils;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.Pool;

public class SimplifyVW {

	class Item extends Inlist<Item> {
		int index;
		int id;
		float area;

		Item prev;
	}

	Pool<Item> pool = new Pool<Item>() {
		@Override
		protected Item createItem() {
			return new Item();
		}
	};

	Item[] heap = new Item[100];
	int size = 0;

	public static void main(String[] args) {
		SimplifyVW s = new SimplifyVW();
		float[] p2 = { 0, 0, 10, 0, 10, 10, 10, 10, 0, 10, 0, 0 };

		GeometryBuffer geom = new GeometryBuffer(p2, new short[2]);

		geom.pointPos = 10;
		s.simplify(geom, 0.5f);

	}

	public void simplify(GeometryBuffer geom, float minArea) {
		Item prev = null;
		Item first = null;
		Item it;
		
		size = 0;
		
		if (heap.length < geom.pointPos >> 1)
			heap = new Item[geom.pointPos >> 1];

		first = prev = push(0, Float.MAX_VALUE);

		for (int i = 2; i < geom.pointPos - 2; i += 2) {
			it = push(i, GeometryUtils.area(geom.points, i - 2, i, i + 2));
			prev.next = it;
			it.prev = prev;

			prev = it;
		}

		Item last = push(geom.pointPos - 2, Float.MAX_VALUE);
		last.prev = prev;
		prev.next = last;

		last.next = first;
		first.prev = last;

		while ((it = pop()) != null) {
			if (it.area > minArea)
				break;

			if (it.prev == it.next)
				break;

			it.prev.next = it.next;
			it.next.prev = it.prev;
			
			if (it.prev != first)
				update(geom, it.prev);

			if (it.next != first)
				update(geom, it.next);
			
			it = pool.release(it);
		}

		geom.clear();
		geom.startPolygon();

		first.prev.next = null;
		it = first;
		
		while (it != null) {
			geom.addPoint(geom.points[it.id], geom.points[it.id + 1]);
			//System.out.println(first.id + " " + first.area);
			it = it.next;
		}
		first = pool.release(first);
	}

	private void update(GeometryBuffer geom, Item it) {
		remove(it);
		it.area = GeometryUtils.area(geom.points, it.prev.id, it.id, it.next.id);
		//System.out.println("< " + it.area);
		push(it);
	}

	public void push(Item it) {
		heap[size] = it;
		it.index = size;
		up(size++);
	}

	public Item push(int id, float area) {
		Item it = pool.get();
		heap[size] = it;
		it.index = size;
		it.area = area;
		it.id = id;
		up(size++);
		return it;
	}

	public Item pop() {
		if (size == 0)
			return null;

		Item removed = heap[0];
		Item obj = heap[--size];
		heap[size] = null;

		if (size > 0) {
			heap[obj.index = 0] = obj;
			down(0);
		}
		return removed;
	}

	public int remove(Item removed) {
		if (size == 0)
			throw new IllegalStateException("size == 0");

		int i = removed.index;
		Item obj = heap[--size];
		heap[size] = null;

		if (i != size) {
			heap[obj.index = i] = obj;

			if (obj.area < removed.area)
				up(i);
			else
				down(i);
		}
		return i;
	};

	private void up(int i) {
		Item obj = heap[i];
		while (i > 0) {
			int up = ((i + 1) >> 1) - 1;
			Item parent = heap[up];

			if (obj.area >= parent.area)
				break;

			heap[parent.index = i] = parent;
			heap[obj.index = i = up] = obj;
		}
	}

	private void down(int i) {
		Item obj = heap[i];
		while (true) {
			int right = (i + 1) << 1;
			int left = right - 1;
			int down = i;

			Item child = heap[down];

			if (left < size && heap[left].area < child.area)
				child = heap[down = left];

			if (right < size && heap[right].area < child.area) {
				child = heap[down = right];
			}
			if (down == i)
				break;

			heap[child.index = i] = child;
			heap[obj.index = down] = obj;
			i = down;
		}
	}
}
