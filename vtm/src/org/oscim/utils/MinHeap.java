package org.oscim.utils;

public class MinHeap {
    private int[] data;
    private float[] heap;

    public static final class Item {
        public int pos;
        public float prio;
        public int index;
    }

    int size;

    private final Item it = new Item();

    public MinHeap(float[] heap, int[] data) {
        this.heap = heap;
        this.data = data;
        size = 1;
    }

    public void push(float prio, int d) {
        int idx = size++;
        int n = idx >> 1;
        while (idx > 1 && heap[n] > prio) {
            heap[idx] = heap[n];
            data[idx] = data[n];
            idx = n;
            n >>= 1;
        }

        heap[idx] = prio;
        data[idx] = d;
    }

    public Item pop() {
        if (size == 1)
            return null;

        it.pos = data[1];
        it.prio = heap[1];

        int idx = --size;

        if (idx > 1) {
            heap[1] = heap[idx];
            data[1] = data[idx];
            heapify();
        }
        return it;
    }

    public int peek() {
        return data[1];
    }

    private void heapify() {
        float root = heap[1];
        int tmp = data[1];

        int last = size;
        int idx = 1;

        while (true) {
            // left child
            int c = idx << 1;
            if (c > last)
                break;
            // right child
            if (c + 1 <= last && heap[c + 1] < heap[c])
                c++;

            float val = heap[c];
            if (val >= root)
                break;

            heap[idx] = val;
            data[idx] = data[c];
            idx = c;
        }

        heap[idx] = root;
        data[idx] = tmp;
    }

    public static void main(String[] args) {
        MinHeap h = new MinHeap(new float[10], new int[10]);
        h.push(10, 10);
        h.push(12, 12);
        h.push(21, 21);
        h.push(31, 31);
        h.push(14, 14);
        h.push(2, 2);
        Item it;
        for (int i = 0; i < 10; i++) {

            it = h.pop();
            if (it != null)
                System.out.println(it.pos + " " + it.prio);
        }

    }
}
