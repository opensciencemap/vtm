package org.oscim.utils;

import org.junit.Assert;
import org.junit.Test;
import org.oscim.utils.KeyMap.HashItem;

import java.util.ArrayList;

public class KeyMapTest {
    static class Item extends HashItem {
        int key;

        public Item(int i) {
            key = i;
        }

        @Override
        public boolean equals(Object obj) {
            Item it = (Item) obj;

            return it.key == key;
        }

        @Override
        public int hashCode() {
            return key;
        }
    }

    @Test
    public void shouldWork1() {
        KeyMap<Item> map = new KeyMap<Item>(512);
        KeyMap<Item> map2 = new KeyMap<Item>(512);

        ArrayList<Item> items = new ArrayList<Item>(1024);

        for (int i = 0; i < 10000; i++) {
            Item it = new Item(i);
            items.add(it);
            Assert.assertNull(map.put(it));
        }

        for (Item it : items) {
            Item it2 = map.remove(it);
            Assert.assertTrue(it == it2);
            map2.put(it2);
        }

        for (Item it : items) {
            Item it2 = map2.get(it);
            Assert.assertTrue(it == it2);
        }

        /* replace the items with itself */
        for (Item it : items) {
            Item it2 = map2.put(it);
            Assert.assertTrue(it == it2);
        }
    }

    @Test
    public void shouldWork2() {
        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            shouldWork1();
            System.out.println("time: " + ((System.currentTimeMillis() - start)));

        }
    }
}
