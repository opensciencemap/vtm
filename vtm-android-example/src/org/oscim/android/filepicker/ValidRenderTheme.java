/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.android.filepicker;

import org.oscim.theme.XmlThemeBuilder;
import org.oscim.tiling.TileSource.OpenResult;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Accepts all valid render theme XML files.
 */
public final class ValidRenderTheme implements ValidFileFilter {
    private OpenResult mOpenResult;

    @Override
    public boolean accept(File file) {
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file);
            XmlThemeBuilder renderThemeHandler = new XmlThemeBuilder();
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            xmlReader.setContentHandler(renderThemeHandler);
            xmlReader.parse(new InputSource(inputStream));
            mOpenResult = OpenResult.SUCCESS;
        } catch (ParserConfigurationException e) {
            mOpenResult = new OpenResult(e.getMessage());
        } catch (SAXException e) {
            mOpenResult = new OpenResult(e.getMessage());
        } catch (IOException e) {
            mOpenResult = new OpenResult(e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                mOpenResult = new OpenResult(e.getMessage());
            }
        }

        return mOpenResult.isSuccess();
    }

    @Override
    public OpenResult getFileOpenResult() {
        return mOpenResult;
    }
}
