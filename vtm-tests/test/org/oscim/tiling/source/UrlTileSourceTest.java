/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016-2017 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.tiling.source;

import org.junit.Before;
import org.junit.Test;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSource;

import static org.fest.assertions.api.Assertions.assertThat;

public class UrlTileSourceTest {
    private UrlTileSource tileSource;

    @Before
    public void setUp() throws Exception {
        tileSource = new TestTileSource("http://example.org/tiles/vtm", "/{Z}/{X}/{Z}.vtm");
    }

    @Test
    public void setApiKey_shouldAppendApiKey() throws Exception {
        tileSource.setApiKey("testkey");
        assertThat(tileSource.getTileUrl(new Tile(0, 0, (byte) 0))).endsWith("?key=testkey");
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(tileSource).isNotNull();
    }

    @Test
    public void shouldUseDefaultHttpEngine() throws Exception {
        TestTileDataSource dataSource = (TestTileDataSource) tileSource.getDataSource();
        assertThat(dataSource.getConnection()).isInstanceOf(LwHttp.class);
    }

    @Test
    public void shouldUseCustomHttpEngine() throws Exception {
        tileSource.setHttpEngine(new OkHttpEngine.OkHttpFactory());
        TestTileDataSource dataSource = (TestTileDataSource) tileSource.getDataSource();
        assertThat(dataSource.getConnection()).isInstanceOf(OkHttpEngine.class);
    }

    class TestTileSource extends UrlTileSource {
        public TestTileSource(String urlString, String tilePath) {
            super(urlString, tilePath);
        }

        @Override
        public ITileDataSource getDataSource() {
            return new TestTileDataSource(this, null, getHttpEngine());
        }
    }

    class TestTileDataSource extends UrlTileDataSource {
        public TestTileDataSource(UrlTileSource tileSource, ITileDecoder tileDecoder,
                                  HttpEngine conn) {
            super(tileSource, tileDecoder, conn);
        }

        public HttpEngine getConnection() {
            return mConn;
        }
    }
}
