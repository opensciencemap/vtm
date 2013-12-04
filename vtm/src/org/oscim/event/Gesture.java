package org.oscim.event;

public interface Gesture {

	static final class Press implements Gesture {
	}

	static final class LongPress implements Gesture {
	}

	static final class Tap implements Gesture {
	}

	static final class DoubleTap implements Gesture {
	}

	public static Gesture PRESS = new Press();
	public static Gesture LONG_PRESS = new LongPress();
	public static Gesture TAP = new Tap();
	public static Gesture DOUBLE_TAP = new DoubleTap();
}
