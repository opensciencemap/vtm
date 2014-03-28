package org.oscim.tiling.source;

import org.oscim.core.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpEngine {
    InputStream read() throws IOException;

    boolean sendRequest(UrlTileSource tileSource, Tile tile) throws IOException;

    void close();

    void setCache(OutputStream os);

    boolean requestCompleted(boolean success);
}
