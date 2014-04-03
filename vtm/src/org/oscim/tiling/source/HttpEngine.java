package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.oscim.core.Tile;

public interface HttpEngine {

	InputStream read() throws IOException;

	boolean sendRequest(Tile tile) throws IOException;

	void close();

	void setCache(OutputStream os);

	boolean requestCompleted(boolean success);

	public interface Factory {
		public abstract HttpEngine create(UrlTileSource tileSource);
	}

}
