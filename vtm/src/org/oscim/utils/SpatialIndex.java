package org.oscim.utils;

import java.util.List;

import org.oscim.core.Box;

public interface SpatialIndex<T> {
	public interface SearchCb<T> {
		/**
		 *
		 * TODO should be able to return 'continue', 'stop',
		 * 'remove-current'
		 * 
		 * @return false to stop search
		 */

		boolean call(T item, Object context);
	}

	public void insert(Box box, T item);

	public boolean remove(Box box, T item);

	public List<T> search(Box bbox, List<T> results);

	public boolean search(Box bbox, SearchCb<T> cb, Object context);

	public int size();

	public void clear();
}
