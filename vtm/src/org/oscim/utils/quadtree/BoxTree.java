package org.oscim.utils.quadtree;

import org.oscim.utils.pool.Inlist;
import org.oscim.utils.quadtree.BoxTree.BoxItem;
import org.oscim.utils.quadtree.BoxTree.BoxNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A BoxTree is made of BoxNodes which hold a list of
 * generic BoxItems which can hold a custom data item.
 * 
 * ... in case this generic isnt obvious at first sight.
 * */
public abstract class BoxTree<Box extends BoxItem<E>, E> extends QuadTree<BoxNode<Box>, Box> {

	final static Logger log = LoggerFactory.getLogger(BoxTree.class);
	static boolean dbg = false;

	protected final int extents;
	protected final int maxDepth;

	public static class BoxNode<T extends BoxItem<?>> extends Node<BoxNode<T>, T> {
		// for non-recursive traversal
		BoxNode<T> next;
		// TODO make final? or update to the actual used extent?
		public int x1;
		public int y1;
		public int x2;
		public int y2;

		//BoxItem<T> list;

		public boolean overlaps(T it) {
			return (x1 < it.x2) && (y1 < it.y2) && (it.x1 < x2) && (it.y1 < y2);
		}

		@Override
		public String toString() {
			return x1 + ":" + y1 + ":" + (x2 - x1);
		}
	}

	public static class BoxItem<T> extends Inlist<BoxItem<T>> {
		public BoxItem(int x1, int y1, int x2, int y2) {
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
		}

		@Override
		public String toString() {
			return x1 + "," + y1 + "/" + x2 + "," + y2;
		}

		public T item;

		public BoxItem() {

		}

		public int x1;
		public int x2;
		public int y1;
		public int y2;

		public boolean overlaps(BoxItem<T> it) {
			return (x1 < it.x2) && (it.x1 < x2) && (y1 < it.y2) && (it.y1 < y2);
		}
	}

	public interface Visitor<T> {
		boolean process(T item);
	}

	//	public class NodeVistor<T> implements Visitor<BoxNode<T>> {
	//
	//		@Override
	//		public boolean process(BoxNode<T> item) {
	//
	//			return false;
	//		}
	//
	//	}

	public BoxTree(int extents, int maxDepth) {
		super();
		// size is -extents to +extents
		this.root.x1 = -extents;
		this.root.y1 = -extents;
		this.root.x2 = extents;
		this.root.y2 = extents;

		this.extents = extents;
		this.maxDepth = maxDepth;
	}

	@Override
	public Box create(int x, int y, int z) {
		return null;
	}

	@Override
	public BoxNode<Box> create() {
		BoxNode<Box> node = new BoxNode<Box>();
		return node;
	}

	@Override
	public void removeItem(Box item) {

	}

	@SuppressWarnings("unchecked")
	public int query(Box box) {
		if (box.x1 > box.x2 || box.y1 > box.y2)
			throw new IllegalArgumentException();

		int x1 = box.x1;
		int x2 = box.x2;
		int y1 = box.y1;
		int y2 = box.y2;

		BoxNode<Box> cur, c;
		BoxNode<Box> stack = root;
		int result = 0;

		boolean drop = false;

		while (stack != null) {

			/** pop cur from stack */
			cur = stack;
			stack = stack.next;

			/** process overlapping items from cur node */
			Box prev = cur.item;

			for (Box it = cur.item; it != null; it = (Box) it.next) {
				if ((x1 <= it.x2) && (x2 >= it.x1) &&
				        (y1 <= it.y2) && (y2 >= it.y1)) {

					result = process(box, it);

					if (result > 0) {
						if (dbg)
							log.debug("{} overlap {} {}", result, box, it);
						drop = true;
						//break O;
					}

					if (result < 0) {
						result = 0;
						if (dbg)
							log.debug("remove overlap {} {}", box, it);
						// remove this itemchild = cur.child11;
						//cur.item = Inlist.remove(cur.item, it);
						if (it == cur.item)
							prev = cur.item = it;
						else
							prev.next = it.next;

						continue;
					}
				}
				prev = it;
			}

			/** put children on stack which overlap with box */
			if ((c = cur.child00) != null &&
			        (x1 < c.x2) && (y1 < c.y2) &&
			        (c.x1 < x2) && (c.y1 < y2)) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child01) != null &&
			        (x1 < c.x2) && (y1 < c.y2) &&
			        (c.x1 < x2) && (c.y1 < y2)) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child10) != null &&
			        (x1 < c.x2) && (y1 < c.y2) &&
			        (c.x1 < x2) && (c.y1 < y2)) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child11) != null &&
			        (x1 < c.x2) && (y1 < c.y2) &&
			        (c.x1 < x2) && (c.y1 < y2)) {
				c.next = stack;
				stack = c;
			}
		}

		/** dont keep dangling references */
		/* gwt optimizer found this cannot be reached :) */
		//while (stack != null)
		//	stack = stack.next;

		return drop ? 1 : 0;
	}

	public abstract boolean collectAll(BoxNode<Box> node);

	public int all() {
		return all(root);
	}

	public int all(BoxNode<Box> node) {

		BoxNode<Box> cur, c;
		BoxNode<Box> stack = node;

		while (stack != null) {
			cur = stack;
			stack = stack.next;

			if (cur.item != null && !collectAll(cur))
				break;

			if ((c = cur.child00) != null) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child01) != null) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child10) != null) {
				c.next = stack;
				stack = c;
			}
			if ((c = cur.child11) != null) {
				c.next = stack;
				stack = c;
			}
		}

		// dont keep dangling references
		while (stack != null)
			stack = stack.next;

		return 0;
	}

	public abstract int process(Box box, Box it);

	public BoxNode<Box> create(BoxNode<Box> parent, int i) {
		BoxNode<Box> node = new BoxNode<Box>();
		int size = (parent.x2 - parent.x1) >> 1;
		node.x1 = parent.x1;
		node.y1 = parent.y1;

		if (i == 0) {
			// top-left
			parent.child00 = node;
		} else if (i == 1) {
			// bottom-left
			parent.child10 = node;
			node.y1 += size;
		} else if (i == 2) {
			// top-right
			parent.child01 = node;
			node.x1 += size;
		} else {
			// bottom-right
			parent.child11 = node;
			node.x1 += size;
			node.y1 += size;
		}

		node.x2 = node.x1 + size;
		node.y2 = node.y1 + size;

		node.parent = parent;

		return node;
	}

	public void insert(Box box) {
		if (box.x1 > box.x2 || box.y1 > box.y2)
			throw new IllegalArgumentException();

		BoxNode<Box> cur = root;
		BoxNode<Box> child = null;

		// tile position in tree
		//int px = 0, py = 0;
		int idX = 0, idY = 0;
		int x1 = box.x1;
		int x2 = box.x2;
		int y1 = box.y1;
		int y2 = box.y2;

		for (int level = 0; level <= maxDepth; level++) {
			// half size of tile at current z
			//int hsize = (extents >> level);
			int hsize = (cur.x2 - cur.x1) >> 1;

			// center of tile (shift by -extents)
			//int cx = px + hsize - extents;
			//int cy = py + hsize - extents;
			int cx = cur.x1 + hsize;
			int cy = cur.y1 + hsize;

			child = null;
			//int childPos = -1;
			//log.debug(cx + ":" + cy + " " + hsize);
			if (x2 <= cx) {
				if (y2 <= cy) {
					if ((child = cur.child00) == null)
						child = create(cur, 0);
				}
				// should be else?
				if (y1 >= cy) {
					if ((child = cur.child10) == null)
						child = create(cur, 1);
					idX++;
				}
			}
			if (x1 >= cx) {
				if (y2 <= cy) {
					if ((child = cur.child01) == null)
						child = create(cur, 2);
					idY++;
				}
				if (y1 >= cy) {
					if ((child = cur.child11) == null)
						child = create(cur, 3);
					idX++;
					idY++;
				}
			}
			//log.debug("child {}", child);

			if (cur == minNode && child != null)
				minNode = cur;

			if (child == null || level == maxDepth) {
				// push item onto list of this node
				box.next = cur.item;
				cur.item = box;

				if (dbg)
					log.debug("insert at: " + level + " / " + idX + ":"
					        + idY + " -- " + x1 + ":" + y1
					        + " /" + (x2) + "x" + (y2));
				break;
			}
			cur = child;
		}

	}

	public void setMinNode(int x1, int y1, int x2, int y2) {
		/* TODO find lowest node that fully contains the region
		 * and set it as start for following queries */
	}

	public abstract boolean process(BoxNode<Box> nodes);

	public void clear() {
		root.child00 = null;
		root.child01 = null;
		root.child10 = null;
		root.child11 = null;
		root.item = null;

		//root = create();
		//root.parent = root;
	}

	static class Item extends BoxItem<Integer> {
		public Item(int x1, int y1, int x2, int y2) {
			super(x1, y1, x2, y2);
		}

		public Item() {
			// TODO Auto-generated constructor stub
		}
	}

	//	static {
	//	System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
	//}
	//	public static void main(String[] args) {
	//
	//		BoxTree<Item, Integer> tree = new BoxTree<Item, Integer>(4096, 12) {
	//
	//			@Override
	//			public int process(Item box, Item it) {
	//				System.out.println("found ... " + box + "\t\t" + it);
	//				return 1;
	//			}
	//
	//			@Override
	//			public boolean process(BoxNode<Item> nodes) {
	//				System.out.println("found ... ");
	//				//for (BoxItem it = nodes.item; it != null; it = it.next) {
	//				//	System.out.println("it: " + it.x1 + "/" + it.y1);
	//				//}
	//
	//				// TODO Auto-generated method stub
	//				return false;
	//			}
	//
	//			@Override
	//			public boolean collectAll(BoxNode<Item> nodes) {
	//				for (Item it = nodes.item; it != null; it = (Item) it.next) {
	//					System.out.println("all: " + it);
	//				}
	//				return false;
	//			}
	//
	//		};
	//		//[VtmAsyncExecutor] DEBUG org.oscim.utils.quadtree.BoxTree - insert at: 8 / 9:0 -- -631:266 /106x187
	//
	//		tree.insert(new Item(-631, 266, -631 + 106, 266 + 187));
	//
	//		//tree.insert(new Item(-40, -40, -32, -32));
	//		//		tree.insert(new Item(-60, -60, -40, -40));
	//		//		tree.insert(new Item(100, 10, 200, 100));
	//		//		tree.insert(new Item(100, 200, 200, 300));
	//		//		tree.insert(new Item(100, 200, 1000, 1000));
	//		//
	//		//		tree.query(new Item(-100, -100, 10, 10));
	//		//tree.query(new Item(10, 10, 100, 100));
	//
	//		tree.all();
	//	}
}
