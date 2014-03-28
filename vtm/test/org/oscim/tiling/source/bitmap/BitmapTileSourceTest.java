package org.oscim.tiling.source.bitmap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.OkHttpEngine;

import static org.fest.assertions.api.Assertions.assertThat;

public class BitmapTileSourceTest {
	private BitmapTileSource tileSource;

	@Before
	public void setUp() throws Exception {
		tileSource = new TestBitmapTileSource("http://tile.openstreetmap.org", 0, 18);
	}

	@Test
	public void shouldNotBeNull() throws Exception {
		assertThat(tileSource).isNotNull();
	}

	@Test
	public void shouldUseLwHttp() throws Exception {
		LwHttp lwHttp = Mockito.mock(LwHttp.class);
		tileSource.setHttpEngine(lwHttp);
		ITileDataSource dataSource = tileSource.getDataSource();
		dataSource.destroy();
		Mockito.verify(lwHttp).close();
	}

	@Test
	public void shouldUseOkHttp() throws Exception {
		OkHttpEngine okHttp = Mockito.mock(OkHttpEngine.class);
		tileSource.setHttpEngine(okHttp);
		ITileDataSource dataSource = tileSource.getDataSource();
		dataSource.destroy();
		Mockito.verify(okHttp).close();
	}

	class TestBitmapTileSource extends BitmapTileSource {
		public TestBitmapTileSource(String url, int zoomMin, int zoomMax) {
			super(url, zoomMin, zoomMax);
		}
	}
}
