package org.oscim.utils.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;
import org.oscim.utils.pool.Inlist.List;

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

	static class Thing extends Inlist<Thing> {
		final int value;

		public Thing(int val) {
			value = val;
		}
	};

}
