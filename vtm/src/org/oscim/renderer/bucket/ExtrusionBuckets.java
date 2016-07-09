package org.oscim.renderer.bucket;

import org.oscim.backend.GL;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ShortBuffer;

public class ExtrusionBuckets extends TileData {
    static final Logger log = LoggerFactory.getLogger(ExtrusionBuckets.class);

    public ExtrusionBucket buckets;

    public boolean compiled;
    public long animTime;

    public final int zoomLevel;

    public final double x;
    public final double y;

    public BufferObject ibo;
    public BufferObject vbo;

    public ExtrusionBuckets(MapTile tile) {
        zoomLevel = tile.zoomLevel;
        x = tile.x;
        y = tile.y;
    }

    /**
     * Set new ExtrusionLayers and clear previous.
     */
    public void setBuckets(ExtrusionBucket el) {
        for (RenderBucket b = buckets; b != null; b = b.next)
            b.clear();

        buckets = el;
    }

    public ExtrusionBucket buckets() {
        return buckets;
    }

    @Override
    protected void dispose() {
        setBuckets(null);

        if (compiled) {
            ibo = BufferObject.release(ibo);
            vbo = BufferObject.release(vbo);
        }

    }

    public void prepare() {
        for (RenderBucket b = buckets; b != null; b = b.next)
            b.prepare();
    }

    public boolean compile() {

        if (buckets == null)
            return false;

        int sumIndices = 0;
        int sumVertices = 0;

        for (ExtrusionBucket b = buckets; b != null; b = b.next()) {
            sumIndices += b.numIndices;
            sumVertices += b.numVertices;
        }
        if (sumIndices == 0)
            return false;

        ShortBuffer vboData = MapRenderer.getShortBuffer(sumVertices * 4);
        ShortBuffer iboData = MapRenderer.getShortBuffer(sumIndices);

        for (ExtrusionBucket b = buckets; b != null; b = b.next())
            b.compile(vboData, iboData);

        int size = sumIndices * 2;
        if (iboData.position() != sumIndices) {
            int pos = iboData.position();
            log.error("invalid indice size: {} {}", sumIndices, pos);
            size = pos * 2;
        }
        ibo = BufferObject.get(GL.ELEMENT_ARRAY_BUFFER, size);
        ibo.loadBufferData(iboData.flip(), size);

        size = sumVertices * 4 * 2;
        if (vboData.position() != sumVertices * 4) {
            int pos = vboData.position();
            log.error("invalid vertex size: {} {}", sumVertices, pos);
            size = pos * 2;
        }

        vbo = BufferObject.get(GL.ARRAY_BUFFER, size);
        vbo.loadBufferData(vboData.flip(), size);

        compiled = true;

        return true;
    }

}
