package org.oscim.tiling.source.bitmap;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.HttpEngine;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

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
		dataSource.destroy();
		Mockito.verify(lwHttp).close();
	}

	@Test
	public void shouldUseOkHttp() throws Exception {
		OkHttpEngine okHttp = Mockito.mock(OkHttpEngine.class);
		tileSource.setHttpEngine(new TestHttpFactory(okHttp));
		UrlTileDataSource dataSource = (UrlTileDataSource) tileSource.getDataSource();
		dataSource.destroy();
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
