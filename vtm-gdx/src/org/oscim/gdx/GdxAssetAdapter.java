package org.oscim.gdx;

import java.io.InputStream;

import org.oscim.backend.AssetAdapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class GdxAssetAdapter extends AssetAdapter{

	@Override
    public InputStream openFileAsStream(String fileName) {
		FileHandle file =  Gdx.files.internal(fileName);
		if (file == null)
			throw new IllegalArgumentException("missing file " + fileName);

		return file.read();
    }
}
