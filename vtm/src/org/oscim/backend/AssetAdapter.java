package org.oscim.backend;

import java.io.InputStream;

/**
 * The Class AssetAdapter.
 */
public abstract class AssetAdapter {

	public static AssetAdapter g;

	/**
	 * Open file as stream.
	 *
	 * @param name the name
	 * @return the input stream
	 */
	public abstract InputStream openFileAsStream(String name);

}
