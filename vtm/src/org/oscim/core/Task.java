package org.oscim.core;

public abstract class Task implements Runnable {

	boolean isCanceled;

	@Override
	public void run() {
		run(isCanceled);
	}

	public void run(boolean canceled) {

	}

	public void cancel() {
		isCanceled = true;
	}

	public void reset() {
		isCanceled = false;
	}
}
