package org.oscim.tiling.source;

import com.squareup.okhttp.OkHttpClient;

import org.junit.Before;
import org.junit.Test;
import org.oscim.tiling.ITileDataSource;

import static org.fest.assertions.api.Assertions.assertThat;

public class UrlTileSourceTest {
	private UrlTileSource tileSource;

	@Before
	public void setUp() throws Exception {
		tileSource = new TestTileSource("http://example.org/tiles/vtm");
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
		tileSource.setHttpEngine(new OkHttpEngine(tileSource.getUrl()));
		TestTileDataSource dataSource = (TestTileDataSource) tileSource.getDataSource();
		assertThat(dataSource.getConnection()).isInstanceOf(OkHttpEngine.class);
	}

	class TestTileSource extends UrlTileSource {
		public TestTileSource(String urlString) {
			super(urlString);
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
