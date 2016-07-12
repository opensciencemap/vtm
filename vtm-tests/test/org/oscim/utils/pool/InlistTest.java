package org.oscim.utils.pool;

import org.junit.Assert;
import org.junit.Test;
import org.oscim.utils.pool.Inlist.List;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class InlistTest {

    @Test
    public void shouldWork() {
        List<Thing> list = new List<Thing>();

        list.reverse();
        assertNull(list.pop());

        list.push(new Thing(1));
        list.push(new Thing(2));
        list.push(new Thing(3));
        list.push(new Thing(4));
        list.push(new Thing(5));

        /* iterate items */
        int i = 5;
        for (Thing it : list)
            assertEquals(it.value, i--);

        assertEquals(i, 0);

        /* iterate with insertion order */
        list.reverse();
        i = 1;
        for (Thing it : list)
            assertEquals(it.value, i++);

        assertEquals(i, 6);

        list.reverse();

        List<Thing> list2 = new List<Thing>();

        /* pop list and append to list2 */
        for (int j = 5; j > 0; j--) {
            Thing t = list.pop();
            assertEquals(t.value, j);
            Assert.assertNull(t.next);

            list2.append(t);
        }

        /* check nothing to iterate */
        for (Thing t : list)
            assert (t == null && t != null);

        assertNull(list.pop());
        assertNull(list.head());

        list.push(new Thing(6));

        /* move items from list2 to list */
        list.appendList(list2.clear());

        assertNull(list2.head());
        assertNull(list2.pop());

        list.reverse();

        list.push(new Thing(0));
        i = 0;
        for (Thing t : list)
            assertEquals(t.value, i++);

        assertEquals(i, 7);
    }

    @Test
    public void shouldRemoveFirstInIterator() {
        List<Thing> list = new List<Thing>();
        list.push(new Thing(1));
        list.push(new Thing(2));
        list.push(new Thing(3));
        list.push(new Thing(4));
        list.push(new Thing(5));

        out.println("\n shouldRemoveFirstInIterator");

        int cnt = 5;
        for (Thing t : list) {
            list.remove();
            cnt--;
            assertEquals(cnt, list.size());
        }
    }

    @Test
    public void shouldRemoveSomeInIterator() {
        List<Thing> list = new List<Thing>();
        list.push(new Thing(1));
        list.push(new Thing(2));
        list.push(new Thing(3));
        list.push(new Thing(4));
        list.push(new Thing(5));
        list.push(new Thing(6));
        out.println("\n shouldRemoveSomeInIterator");

        int pos = 0;
        for (Thing t : list) {
            if (pos++ % 2 == 0) {
                out.println(pos + " val:" + t.value);
                list.remove();
            }
        }

        assertEquals(3, list.size());

        for (Thing t : list) {
            out.println(t.value);
        }
    }

    @Test
    public void shouldRemoveLastInIterator() {
        List<Thing> list = new List<Thing>();
        list.append(new Thing(1));
        list.append(new Thing(2));
        out.println("\n shouldRemoveLastInIterator");

        int pos = 0;
        for (Thing t : list) {
            if (pos++ == 1) {
                out.println(pos + " val:" + t.value);
                list.remove();
            }
        }

        assertEquals(1, list.size());

        for (Thing t : list) {
            out.println(t.value);
        }
    }

    static class Thing extends Inlist<Thing> {
        final int value;

        public Thing(int val) {
            value = val;
        }
    }

    ;

}
