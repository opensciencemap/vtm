package org.oscim.utils;

import org.oscim.backend.Log.Logger;

public class TestLogger implements Logger {

	@Override
	public void d(String tag, String msg) {
		System.out.println(tag + " " + msg);
	}

	@Override
	public void w(String tag, String msg) {
		System.out.println(tag + " " + msg);
	}

	@Override
	public void e(String tag, String msg) {
		System.out.println(tag + " " + msg);
	}

	@Override
	public void i(String tag, String msg) {
		System.out.println(tag + " " + msg);
	}

}
