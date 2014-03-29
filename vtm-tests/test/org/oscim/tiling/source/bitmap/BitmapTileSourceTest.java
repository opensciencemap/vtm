package org.oscim.tiling.source.bitmap;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory;
import org.oscim.tiling.source.UrlTileDataSource;

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
		LwHttpFactory lwHttp = Mockito.mock(LwHttpFactory.class);
		tileSource.setHttpEngine(lwHttp);
		ITileDataSource dataSource = tileSource.getDataSource();
		dataSource.destroy();
		Mockito.verify(lwHttp).close();
	}

	@Test
	public void shouldUseOkHttp() throws Exception {
		OkHttpFactory okHttp = Mockito.mock(OkHttpFactory.class);
		tileSource.setHttpEngine(okHttp);
		UrlTileDataSource dataSource = (UrlTileDataSource) tileSource.getDataSource();
		dataSource.destroy();
		//Mockito.verify(dataSource.mConn).close();
	}

	class TestBitmapTileSource extends BitmapTileSource {
		public TestBitmapTileSource(String url, int zoomMin, int zoomMax) {
			super(url, zoomMin, zoomMax);
		}
	}
}
