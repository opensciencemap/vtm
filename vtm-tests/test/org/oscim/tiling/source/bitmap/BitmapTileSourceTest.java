package org.oscim.tiling.source.bitmap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscim.layers.tile.bitmap.BitmapTileLayer.FadeStep;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.HttpEngine;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

import static org.fest.assertions.api.Assertions.assertThat;

public class BitmapTileSourceTest {
    private BitmapTileSource tileSource;

    @Before
    public void setUp() throws Exception {
        tileSource = new BitmapTileSource("http://tile.openstreetmap.org", 0, 18);
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
        UrlTileDataSource dataSource = (UrlTileDataSource) tileSource.getDataSource();
        dataSource.dispose();
        Mockito.verify(okHttp).close();
    }

    @Test
    public void shouldUseBuilderConfig() {
        BitmapTileSource ts = BitmapTileSource.builder()
                .url("http://example.com")
                .zoomMax(42)
                .zoomMin(23)
                .fadeSteps(new FadeStep[]{new FadeStep(0, 10, 0.5f, 1.0f)})
                .build();

        assertThat(ts.getUrl().getHost()).isEqualTo("example.com");
        assertThat(ts.getZoomLevelMin()).isEqualTo(23);
        assertThat(ts.getZoomLevelMax()).isEqualTo(42);
        assertThat(ts.getFadeSteps()).isNotNull();
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
