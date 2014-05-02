package org.oscim.layers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.map.ViewController;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapEventLayerTest {
	private MapEventLayer layer;
	private Map mockMap;
	private ViewController mockViewport;
	private Animator mockAnimator;
	private ArgumentCaptor<Float> argumentCaptor;

	@Before
	public void setUp() throws Exception {
		mockMap = Mockito.mock(Map.class);
		mockViewport = Mockito.mock(ViewController.class);
		mockAnimator = Mockito.mock(Animator.class);
		layer = new MapEventLayer(mockMap);
		when(mockMap.viewport()).thenReturn(mockViewport);
		when(mockMap.animator()).thenReturn(mockAnimator);
		when(mockMap.getHeight()).thenReturn(6);
		argumentCaptor = ArgumentCaptor.forClass(float.class);
	}

	@Test
	public void shouldNotBeNull() throws Exception {
		assertThat(layer).isNotNull();
	}

	@Test
	public void doubleTouchDragUp_shouldDecreaseContentScale() throws Exception {
		layer.onTouchEvent(new TestMotionEvent(MotionEvent.ACTION_DOWN, 1, 1));
		layer.onGesture(Gesture.DOUBLE_TAP, new TestMotionEvent(MotionEvent.ACTION_MOVE, 1, 0));
		layer.onTouchEvent(new TestMotionEvent(MotionEvent.ACTION_MOVE, 1, 0));
		verify(mockViewport).scaleMap(argumentCaptor.capture(), any(float.class), any(float.class));
		assertThat(argumentCaptor.getValue()).isLessThan(1);
	}

	@Test
	public void doubleTouchDragDown_shouldIncreaseContentScale() throws Exception {
		layer.onTouchEvent(new TestMotionEvent(MotionEvent.ACTION_DOWN, 1, 1));
		layer.onGesture(Gesture.DOUBLE_TAP, new TestMotionEvent(MotionEvent.ACTION_MOVE, 1, 2));
		layer.onTouchEvent(new TestMotionEvent(MotionEvent.ACTION_MOVE, 1, 2));
		verify(mockViewport).scaleMap(argumentCaptor.capture(), any(float.class), any(float.class));
		assertThat(argumentCaptor.getValue()).isGreaterThan(1);
	}

	class TestMotionEvent extends MotionEvent {
		final int action;
		final float x;
		final float y;

		public TestMotionEvent(int action, float x, float y) {
			this.action = action;
			this.x = x;
			this.y = y;
		}

		@Override
		public long getTime() {
			return 0;
		}

		@Override
		public int getAction() {
			return action;
		}

		@Override
		public float getX() {
			return x;
		}

		@Override
		public float getY() {
			return y;
		}

		@Override
		public float getX(int idx) {
			return x;
		}

		@Override
		public float getY(int idx) {
			return y;
		}

		@Override
		public int getPointerCount() {
			return 0;
		}
	}
}
