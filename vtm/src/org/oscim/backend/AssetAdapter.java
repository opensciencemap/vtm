package org.oscim.backend;

import java.io.InputStream;

public abstract class AssetAdapter {
	public static AssetAdapter g;

	public abstract InputStream openFileAsStream(String name);

}
