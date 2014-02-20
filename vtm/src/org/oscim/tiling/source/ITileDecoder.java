package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;

public interface ITileDecoder {

	boolean decode(Tile tile, ITileDataSink sink, InputStream is)
	        throws IOException;
}
