package org.oscim.renderer;

import org.oscim.renderer.NativeBufferPool.BufferItem;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.Pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Pool to retrieve temporary native buffer on GL-Thread:
 * This pool ensures to not use the same buffer to upload data twice
 * within a frame.
 * - Contrary to what the OpenGL doc says data seems *not* to be
 * always* copied after glBufferData returns...
 * - Somehow it does always copy when using Android GL bindings
 * but not when using libgdx bindings (LWJGL or AndroidGL20)
 */
public class NativeBufferPool extends Pool<BufferItem> {

    static final class BufferItem extends Inlist<BufferItem> {
        ByteBuffer byteBuffer;
        ShortBuffer sBuffer;
        FloatBuffer fBuffer;
        IntBuffer iBuffer;

        int size;

        void growBuffer(int size) {
            //log.debug("grow buffer " + size);
            // 32kb min size
            if (size < (1 << 15))
                size = (1 << 15);

            byteBuffer = ByteBuffer
                    .allocateDirect(size)
                    .order(ByteOrder.nativeOrder());
            this.size = size;

            sBuffer = null;
            iBuffer = null;
            fBuffer = null;
        }
    }

    private BufferItem mUsedBuffers;

    @Override
    protected BufferItem createItem() {
        // unused;
        return null;
    }

    public BufferItem get(int size) {
        BufferItem b = mPool;

        if (b == null) {
            b = new BufferItem();
        } else {
            mPool = b.next;
            b.next = null;
        }
        if (b.size < size)
            b.growBuffer(size);

        mUsedBuffers = Inlist.push(mUsedBuffers, b);

        return b;
    }

    public void releaseBuffers() {
        mUsedBuffers = releaseAll(mUsedBuffers);
    }

    /**
     * Only use on GL Thread! Get a native ShortBuffer for temporary use.
     */
    public ShortBuffer getShortBuffer(int size) {
        BufferItem b = get(size * 2);

        if (b.sBuffer == null) {
            b.byteBuffer.clear();
            b.sBuffer = b.byteBuffer.asShortBuffer();
        } else {
            b.sBuffer.clear();
        }
        return b.sBuffer;
    }

    /**
     * Only use on GL Thread! Get a native FloatBuffer for temporary use.
     */
    public FloatBuffer getFloatBuffer(int size) {
        BufferItem b = get(size * 4);
        if (b.fBuffer == null) {
            b.byteBuffer.clear();
            b.fBuffer = b.byteBuffer.asFloatBuffer();
        } else {
            b.fBuffer.clear();
        }
        b.fBuffer.clear();

        return b.fBuffer;
    }

    /**
     * Only use on GL Thread! Get a native IntBuffer for temporary use.
     */
    public IntBuffer getIntBuffer(int size) {
        BufferItem b = get(size * 4);
        if (b.iBuffer == null) {
            b.byteBuffer.clear();
            b.iBuffer = b.byteBuffer.asIntBuffer();
        } else {
            b.iBuffer.clear();
        }
        return b.iBuffer;
    }

}
