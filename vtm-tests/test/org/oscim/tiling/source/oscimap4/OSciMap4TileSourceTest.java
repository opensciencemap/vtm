package org.oscim.tiling.source.oscimap4;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.HttpEngine;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;

import static org.fest.assertions.api.Assertions.assertThat;

public class OSciMap4TileSourceTest {
    private OSciMap4TileSource tileSource;

    @Before
    public void setUp() throws Exception {
        tileSource = new OSciMap4TileSource("http://www.example.org/tiles/vtm");
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(tileSource).isNotNull();
    }

    @Test
    public void shouldUseLwHttp() throws Exception {
        LwHttp lwHttp = Mockito.mock(LwHttp.class);
        tileSource.setHttpEngine(new TestHttpFactory(lwHttp));
        ITileDataSource dataSource = tileSource.getDataSource();
        dataSource.dispose();
        Mockito.verify(lwHttp).close();
    }

    @Test
    public void shouldUseOkHttp() throws Exception {
        OkHttpEngine okHttp = Mockito.mock(OkHttpEngine.class);
        tileSource.setHttpEngine(new TestHttpFactory(okHttp));
        ITileDataSource dataSource = tileSource.getDataSource();
        dataSource.dispose();
        Mockito.verify(okHttp).close();
    }

    /**
     * Test factory that allows the specific {@link HttpEngine} instance to be
     * set.
     */
    class TestHttpFactory implements HttpEngine.Factory {
        final HttpEngine engine;

        public TestHttpFactory(HttpEngine engine) {
            this.engine = engine;
        }

        @Override
        public HttpEngine create(UrlTileSource tileSource) {
            return engine;
        }
    }
}
