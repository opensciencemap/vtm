/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.utils.pool;

import java.util.Iterator;

import javax.annotation.CheckReturnValue;

/**
 * Utility class for making poolable objects.
 * Instead of using an additional list to hold pool items just extend this
 * class.
 * <p/>
 * Also handy for objects that exist in only *one list* at a time, if you
 * are *REALLY* sure about it. Better do not use it! :)
 */
public class Inlist<T extends Inlist<T>> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class List<T extends Inlist<?>> implements Iterable<T>, Iterator<T> {
        private Inlist head;
        private Inlist cur;

        /**
         * Insert single item at start of list.
         * item.next must be null.
         */
        public void push(T it) {
            if (it.next != null)
                throw new IllegalArgumentException("item.next must be null");

            ((Inlist) it).next = head;
            head = it;
        }

        /**
         * Insert item at start of list.
         */
        public T pop() {
            if (head == null)
                return null;

            Inlist it = head;
            head = it.next;
            it.next = null;
            return (T) it;
        }

        /**
         * Reverse list.
         */
        public void reverse() {
            Inlist tmp;
            Inlist itr = head;
            head = null;

            while (itr != null) {
                /* keep next */
                tmp = itr.next;

                /* push itr onto new list */
                itr.next = head;
                head = itr;

                itr = tmp;
            }
        }

        /**
         * Append item, O(n) - use push() and
         * reverse() to iterate in insertion order!
         */
        public void append(T it) {
            head = Inlist.appendItem(head, it);
        }

        /**
         * Append Inlist.
         */
        public void appendList(T list) {
            head = Inlist.appendList(head, list);
        }

        /**
         * Remove item from list.
         */
        public void remove(T it) {
            cur = null;
            head = Inlist.remove(head, it);
        }

        /**
         * Clear list.
         *
         * @return head of list
         */
        public T clear() {
            Inlist ret = head;
            head = null;
            cur = null;
            return (T) ret;
        }

        /**
         * @return first node in list
         */
        public T head() {
            return (T) head;
        }

        /**
         * Iterator: Has next item
         */
        @Override
        public boolean hasNext() {
            return cur != null;
        }

        /**
         * Iterator: Get next item
         */
        @Override
        public T next() {
            if (cur == null)
                throw new IllegalStateException();

            Inlist tmp = cur;
            cur = cur.next;
            return (T) tmp;
        }

        /**
         * Iterator: Remove current item
         */
        @Override
        public void remove() {
            /* iterator is at first position */
            if (head.next == cur) {
                head = head.next;
                return;
            }

            Inlist prev = head;
            while (prev.next.next != cur)
                prev = prev.next;

            prev.next = cur;
        }

        /**
         * NB: Only one iterator at a time possible!
         */
        @Override
        public Iterator<T> iterator() {
            cur = head;
            return this;
        }

        public int size() {
            return Inlist.size(head);
        }
    }

    public T next;

    public T next() {
        return next;
    }

    /**
     * Push 'item' onto 'list'.
     *
     * @param list the list
     * @param item the item
     * @return the new head of 'list' (item)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @CheckReturnValue
    public static <T extends Inlist<?>> T push(T list, T item) {
        if (item.next != null)
            throw new IllegalArgumentException("'item' is a list");

        ((Inlist) (item)).next = list;
        return item;
    }

    /**
     * Get size of 'list'.
     *
     * @param list the list
     * @return the number of items in 'list'
     */
    public static <T extends Inlist<?>> int size(T list) {
        int count = 0;
        for (Inlist<?> l = list; l != null; l = l.next)
            count++;
        return count;
    }

    /**
     * Removes the 'item' from 'list'.
     *
     * @param list the list
     * @param item the item
     * @return the new head of 'list'
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @CheckReturnValue
    public static <T extends Inlist<?>> T remove(T list, T item) {
        if (item == list) {
            Inlist head = item.next;
            item.next = null;
            return (T) head;
        }

        for (Inlist prev = list, it = list.next; it != null; it = it.next) {
            if (it == item) {
                prev.next = item.next;
                item.next = null;
                return list;
            }
            prev = it;
        }

        return list;
    }

    /**
     * Gets the 'item' with index 'i'.
     *
     * @param list the list
     * @param i    the index
     * @return the item or null
     */
    @SuppressWarnings({"unchecked"})
    @CheckReturnValue
    public static <T extends Inlist<?>> T get(T list, int i) {
        if (i < 0)
            return null;

        while (--i > 0 && list != null)
            list = (T) list.next;

        if (i == 0)
            return list;

        return null;
    }

    /**
     * Append 'item' to 'list'. 'item' may not be in another list,
     * i.e. item.next must be null
     *
     * @param list the list
     * @param item the item
     * @return the new head of 'list'
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @CheckReturnValue
    public static <T extends Inlist<?>> T appendItem(T list, T item) {

        if (item.next != null)
            throw new IllegalArgumentException("'item' is list");

        if (list == null)
            return item;

        Inlist<?> it = list;
        while (it.next != null)
            it = it.next;

        ((Inlist) it).next = item;

        return list;
    }

    /**
     * Append list 'other' to 'list'.
     *
     * @param list  the list
     * @param other the other
     * @return the head of 'list'
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @CheckReturnValue
    public static <T extends Inlist> T appendList(T list, T other) {

        if (list == null)
            return other;

        if (other == null)
            return list;

        for (Inlist it = list; ; it = it.next) {
            if (it.next == null) {
                ((Inlist) it).next = other;
                break;
            }
            //else if (it.next == other) {
            //    throw new IllegalArgumentException("'other' already in 'list'");
            //}
        }

        return list;
    }

    /**
     * Get last item in from list.
     *
     * @param list the list
     * @return the last item
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    public static <T extends Inlist<?>> T last(T list) {
        while (list != null) {
            if (list.next == null)
                return list;
            list = (T) list.next;
        }
        return null;
    }

    /**
     * Prepend 'item' relative to 'other'.
     *
     * @param list  the list
     * @param item  the item
     * @param other the other list
     * @return the new head of list
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @CheckReturnValue
    public static <T extends Inlist<?>> T prependRelative(T list, T item, T other) {

        if (item.next != null)
            throw new IllegalArgumentException("'item' is list");

        if (list == null)
            throw new IllegalArgumentException("'list' is null");

        if (list == other) {
            ((Inlist) item).next = list;
            return item;
        }

        T it = list;

        while (it != null && it.next != other)
            it = (T) it.next;

        if (it == null)
            throw new IllegalArgumentException("'other' not in 'list'");

        ((Inlist) item).next = it.next;
        ((Inlist) it).next = item;

        return list;
    }
}
