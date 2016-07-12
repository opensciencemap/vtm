/*
 * Copyright 2016 devemux86
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
package org.oscim.utils;

import org.junit.Assert;
import org.junit.Test;
import org.oscim.core.Box;
import org.oscim.utils.SpatialIndex.SearchCb;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class RTreeTest {

    public class Item {
        final int val;
        final double[] min;
        final double[] max;

        Item(final double[] min, final double[] max, int val) {
            this.val = val;
            this.min = min.clone();
            this.max = max.clone();
        }

        @Override
        public String toString() {
            //            return val + "/"
            //                    + Arrays.toString(min) + "/"
            //                    + Arrays.toString(max);
            return String.valueOf(val);
        }
    }

    @Test
    public void shouldRemove() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {1, 1};
        Item it = new Item(min, max, 1);
        t.insert(min, max, it);
        Assert.assertEquals(1, t.size());
        t.remove(min, max, it);
        Assert.assertEquals(0, t.size());
    }

    @Test
    public void shouldWork() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {1, 1};
        for (int i = 0; i < 1000; i++) {

            Item it = new Item(min, max, i);
            //out.println("insert: " + it.val);
            t.insert(min, max, it);

            min[0]++;
            min[1]++;

            max[0]++;
            max[1]++;

        }
        Assert.assertEquals(1000, t.size());

        min[0] = 0;
        min[1] = 0;
        //        max[0] = 4;
        //        max[1] = 4;

        final ArrayList<Item> results = new ArrayList<Item>();

        t.search(min, max, new SearchCb<RTreeTest.Item>() {
                    @Override
                    public boolean call(Item item, Object context) {
                        //out.println("found: " + item);
                        results.add(item);
                        return true;
                    }
                },
                null);

        for (int i = 999; i >= 0; i--) {
            Item it = results.remove(i);
            //boolean removed =
            t.remove(it.min, it.max, it);
            //out.println("REMOVED: " + it + " " + removed);

            Assert.assertEquals(i, t.size());
        }
        Assert.assertEquals(0, t.size());
    }

    @Test
    public void shouldStack1() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {1, 1};

        int numItems = 10000;
        for (int i = 0; i < numItems; i++) {

            Item it = new Item(min, max, i);
            //out.println("insert: " + it.val);
            t.insert(min, max, it);

            min[0]++;
            min[1]++;

            max[0]++;
            max[1]++;

        }
        assertEquals(numItems, t.size());

        min[0] = 0;
        min[1] = 0;

        final ArrayList<Item> results = new ArrayList<Item>();

        t.search(min, max, new SearchCb<RTreeTest.Item>() {
            @Override
            public boolean call(Item item, Object context) {
                //out.println("found: " + item);
                results.add(item);
                return true;
            }
        }, null);

        assertEquals(results.size(), numItems);

        //        for (int i = 999; i >= 0; i--) {
        //            Item it = results.remove(i);
        //            boolean removed = t.remove(it.min, it.max, it);
        //            //out.println("REMOVED: " + it + " " + removed);
        //
        //            Assert.assertEquals(i, t.count());
        //        }
        //        Assert.assertEquals(0, t.count());
    }

    @Test
    public void shouldStack2() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {1, 1};

        int numItems = 10000;
        for (int i = 0; i < numItems; i++) {

            Item it = new Item(min, max, i);
            //out.println("insert: " + it.val);
            t.insert(min, max, it);

            min[0]++;
            min[1]++;

            max[0]++;
            max[1]++;

        }
        assertEquals(numItems, t.size());

        min[0] = 0;
        min[1] = 0;

        final ArrayList<Item> results = new ArrayList<Item>();

        Box bbox = new Box(min[0], min[1], max[0], max[1]);

        t.search(bbox, results);

        assertEquals(numItems, results.size());

        //        for (int i = 999; i >= 0; i--) {
        //            Item it = results.remove(i);
        //            boolean removed = t.remove(it.min, it.max, it);
        //            //out.println("REMOVED: " + it + " " + removed);
        //
        //            Assert.assertEquals(i, t.count());
        //        }
        //        Assert.assertEquals(0, t.count());
    }

    @Test
    public void shouldWork0() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {0, 0};
        Random r = new Random((long) (Math.PI * 10000000));
        ArrayList<Item> items = new ArrayList<Item>();

        for (int i = 0; i < 10000; i++) {
            min[0] = r.nextDouble() * 10000 - 5000;
            min[1] = Math.random() * 10000 - 5000;
            max[0] = min[0] + Math.random() * 100;
            max[1] = min[1] + Math.random() * 100;

            Item it = new Item(min, max, i);
            t.insert(min, max, it);
            items.add(it);
        }

        Assert.assertEquals(10000, t.size());

        /*SearchCb<RTreeTest.Item> cb = new SearchCb<RTreeTest.Item>() {
            @Override
            public boolean call(Item item, Object context) {
                //out.println("found: " + item);
                //results.add(item);
                return true;
            }
        };

        int counter = 0;

        for (int i = 0; i < 10000; i++) {
            counter += t.search(min, max, cb, null);
        }

        System.out.println("found: " + counter);*/
    }

    @Test
    public void shouldWork1() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {0, 0};

        ArrayList<Item> items = new ArrayList<Item>();

        for (int i = 0; i < 10000; i++) {
            min[0] = Math.random() * 10000 - 5000;
            min[1] = Math.random() * 10000 - 5000;
            max[0] = min[0] + Math.random() * 1000;
            max[1] = min[1] + Math.random() * 1000;

            Item it = new Item(min, max, i);
            t.insert(min, max, it);
            items.add(it);
        }

        Assert.assertEquals(10000, t.size());

        for (Item it : items)
            t.remove(it.min, it.max, it);

        Assert.assertEquals(0, t.size());
    }

    @Test
    public void shouldWork2() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {0, 0};

        ArrayList<Item> items = new ArrayList<Item>();

        int numItems = 10000;
        for (int i = 0; i < numItems; i++) {
            min[0] = Math.random() * 10000 - 5000;
            min[1] = Math.random() * 10000 - 5000;
            max[0] = min[0] + Math.random() * 1000;
            max[1] = min[1] + Math.random() * 1000;

            Item it = new Item(min, max, i);
            t.insert(min, max, it);
            items.add(it);
        }

        Assert.assertEquals(numItems, t.size());

        for (Item it : items)
            t.remove(it.min, it.max, it);

        Assert.assertEquals(0, t.size());
    }

    @Test
    public void shouldWork3() {
        RTree<Item> t = new RTree<Item>();
        double[] min = {0, 0};
        double[] max = {0, 0};

        ArrayList<Item> items = new ArrayList<Item>();

        for (int i = 0; i < 1000; i++) {
            min[0] = Math.random() * 10000 - 5000;
            min[1] = Math.random() * 10000 - 5000;
            max[0] = min[0] + Math.random() * 1000;
            max[1] = min[1] + Math.random() * 1000;

            Item it = new Item(min, max, i);
            t.insert(min, max, it);
            items.add(it);
        }

        int cnt = 0;

        for (@SuppressWarnings("unused")
        Item it : t) {
            //System.out.println(it.val);
            cnt++;
        }

        Assert.assertEquals(1000, cnt);

        Assert.assertEquals(1000, t.size());

    }

    public static void main(String[] args) {
        RTreeTest t = new RTreeTest();
        t.shouldWork2();
    }
}
