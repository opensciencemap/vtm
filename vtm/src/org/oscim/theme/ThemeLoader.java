/*
 * Copyright 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oscim.theme;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.Log;
import org.oscim.utils.IOUtils;

public class ThemeLoader {
	private final static String TAG = ThemeLoader.class.getName();

	/**
	 * Load internal theme, see {@link InternalRenderTheme}.
	 *
	 * @param internalRenderTheme ...
	 * @return ...
	 */
	public static IRenderTheme load(InternalRenderTheme internalRenderTheme) {
		 return load((Theme) internalRenderTheme);
	}


	/**
	 * Load theme from XML file.
	 *
	 * @param renderThemePath ..
	 * @return ...
	 * @throws FileNotFoundException ...
	 */
	public static IRenderTheme load(String renderThemePath) throws FileNotFoundException {
		return load(new ExternalRenderTheme(renderThemePath));
	}

	public static IRenderTheme load(Theme theme) {

		InputStream inputStream = null;
		try {
			inputStream = theme.getRenderThemeAsStream();
			IRenderTheme t = RenderThemeHandler.getRenderTheme(inputStream);
			t.scaleTextSize(CanvasAdapter.textScale + (CanvasAdapter.dpi / 240 - 1) * 0.5f);

			return t;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		return null;
	}

}
