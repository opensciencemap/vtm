/*
 * Copyright 2012, 2013 Hannes Janetzek
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

package org.oscim.utils.geom;

import org.oscim.core.GeometryBuffer;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.Pool;

/**
 * Visvalingam-Wyatt simplification
 * 
 * based on:
 * https://github.com/mbloch/mapshaper/blob/master/src/mapshaper-visvalingam.js
 * */
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
			it = push(i, area(geom.points, i - 2, i, i + 2));
			prev.next = it;
			it.prev = prev;

			prev = it;
		}

		Item last = push(geom.pointPos - 2, Float.MAX_VALUE);

		//		sorter.doSort(heap, DistanceComparator, 0, size);
		//		for (int i = 0; i < size; i++)
		//			heap[i].index = i;

		last.prev = prev;
		prev.next = last;

		last.next = first;
		first.prev = last;

		int cnt = 0;
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
			cnt++;
		}
		//System.out.println("dropped: " + cnt + "/" + (geom.pointPos >> 1));
		first.prev.next = null;
		first.prev = null;
		it = first;

		float[] points = new float[geom.pointPos];
		System.arraycopy(geom.points, 0, points, 0, geom.pointPos);

		geom.clear();
		geom.startPolygon();

		while (it != null) {
			float x = points[it.id];
			float y = points[it.id + 1];
			geom.addPoint(x, y);
			it = it.next;
		}

		first = pool.release(first);
	}

	public static float area(float[] a, int p1, int p2, int p3) {

		float area = GeometryUtils.area(a, p1, p2, p3);
		double dotp = GeometryUtils.dotProduct(a, p1, p2, p3);
		//return (float) (area * (0.5 + 0.5 * (1 - dotp * dotp)));

		dotp = Math.abs(dotp);
		double weight = dotp < 0.5 ? 0.1 : dotp < 1 ? 0.3 : 1;
		return (float) (area * weight);
	}

	private void update(GeometryBuffer geom, Item it) {
		float area = area(geom.points, it.prev.id, it.id, it.next.id);
		update(it, area);
		//remove(it);
		//it.area = area(geom.points, it.prev.id, it.id, it.next.id);
		//push(it);
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

	public void update(Item it, float area) {
		if (area < it.area) {
			it.area = area;
			up(it.index);
		} else {
			it.area = area;
			down(it.index);
		}
	}

	public int remove(Item removed) {
		if (size == 0)
			throw new IllegalStateException("size == 0");

		int i = removed.index;
		Item obj = heap[--size];
		heap[size] = null;

		/* if min obj was popped */
		if (i == size)
			return i;

		/* else put min obj in place of the removed item */
		obj.index = i;
		heap[i] = obj;

		if (obj.area < removed.area) {
			up(i);
		} else
			down(i);

		return i;
	};

	private void up(int i) {
		Item it = heap[i];
		while (i > 0) {
			int up = ((i + 1) >> 1) - 1;
			Item parent = heap[up];

			if (it.area >= parent.area)
				break;

			parent.index = i;
			heap[i] = parent;

			it.index = i = up;
			heap[i] = it;
		}
	}

	private void down(int i) {
		Item it = heap[i];
		while (true) {
			int right = (i + 1) << 1;
			int left = right - 1;
			int down = i;

			Item child = heap[down];

			if (left < size && heap[left].area < child.area)
				child = heap[down = left];

			if (right < size && heap[right].area < child.area)
				child = heap[down = right];

			if (down == i)
				break;

			heap[child.index = i] = child;
			heap[it.index = i = down] = it;
		}
	}
	//	final static Comparator<Item> DistanceComparator = new Comparator<Item>() {
	//		@Override
	//		public int compare(Item a, Item b) {
	//
	//			if (a.area > b.area) {
	//				return -1;
	//			}
	//			if (a.area < b.area) {
	//				return 1;
	//			}
	//			return 0;
	//		}
	//	};
	//	TimSort<Item> sorter = new TimSort<Item>();
	//
	//	Item push(int id, float area) {
	//		Item it = pool.get();
	//		//heap[size] = it;
	//		//it.index = size;
	//		it.area = area;
	//		it.id = id;
	//
	//		heap[size++] = it;
	//		return it;
	//	}
	//
	//	Item pop() {
	//		return heap[--size];
	//	}
	//
	//	void update(Item it, float area) {
	//		boolean up = it.area < area;
	//		it.area = area;
	//		if (up) {
	//			sorter.doSort(heap, DistanceComparator, 0, it.index + 1);
	//			for (int i = 0, end = it.index + 1; i < end; i++)
	//				heap[i].index = i;
	//		} else {
	//			sorter.doSort(heap, DistanceComparator, it.index, size);
	//			for (int i = it.index; i < size; i++)
	//				heap[i].index = i;
	//		}
	//	}

}
