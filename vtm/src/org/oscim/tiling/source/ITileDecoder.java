package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.core.Tile;

public interface ITileDecoder {

	boolean decode(Tile tile, ITileDataSink sink, InputStream is, int contentLength)
	        throws IOException;
}
