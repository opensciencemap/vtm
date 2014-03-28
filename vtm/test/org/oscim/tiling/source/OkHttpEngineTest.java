package org.oscim.tiling.source;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oscim.core.Tile;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;

public class OkHttpEngineTest {
	private OkHttpEngine engine;
	private MockWebServer server;
	private MockResponse mockResponse;

	@Before
	public void setUp() throws Exception {
		mockResponse = new MockResponse();
		mockResponse.setBody("TEST RESPONSE".getBytes());
		server = new MockWebServer();
		server.enqueue(mockResponse);
		server.play();
		engine = new OkHttpEngine(server.getUrl("/tiles/vtm"));
	}

	@After
	public void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	public void shouldNotBeNull() throws Exception {
		assertThat(engine).isNotNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendRequest_shouldRejectNullTile() throws Exception {
		engine.sendRequest(null, null);
	}

	@Test
	public void sendRequest_shouldAppendXYZToPath() throws Exception {
		engine.sendRequest(new OSciMap4TileSource("http://www.example.org/tiles/vtm"),
				new Tile(1, 2, new Integer(3).byteValue()));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/tiles/vtm/3/1/2.vtm");
	}

	@Test
	public void read_shouldReturnResponseStream() throws Exception {
		engine.sendRequest(new OSciMap4TileSource("http://www.example.org/tiles/vtm"),
				new Tile(1, 2, new Integer(3).byteValue()));

		InputStream responseStream = engine.read();
		String response = new BufferedReader(new InputStreamReader(responseStream)).readLine();
		assertThat(response).isEqualTo("TEST RESPONSE");
	}

	@Test(expected = IOException.class)
	public void close_shouldCloseInputStream() throws Exception {
		engine.sendRequest(new OSciMap4TileSource("http://www.example.org/tiles/vtm"),
				new Tile(1, 2, new Integer(3).byteValue()));
		engine.close();

		// Calling read after the stream is closed should throw an exception.
		InputStream responseStream = engine.read();
		responseStream.read();
	}

	@Test(expected = IOException.class)
	public void requestCompleted_shouldCloseInputStream() throws Exception {
		engine.sendRequest(new OSciMap4TileSource("http://www.example.org/tiles/vtm"),
				new Tile(1, 2, new Integer(3).byteValue()));
		engine.requestCompleted(true);

		// Calling read after the stream is closed should throw an exception.
		InputStream responseStream = engine.read();
		responseStream.read();
	}

	@Test
	public void requestCompleted_shouldReturnValueGiven() throws Exception {
		assertThat(engine.requestCompleted(true)).isTrue();
		assertThat(engine.requestCompleted(false)).isFalse();
	}
}
