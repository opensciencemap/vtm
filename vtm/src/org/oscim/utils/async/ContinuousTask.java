package org.oscim.utils.async;

import org.oscim.map.Map;

public abstract class ContinuousTask implements Runnable {
	private final Map mMap;

	protected boolean mRunning;
	protected boolean mWait;
	protected boolean mCancel;
	protected boolean mDelayed;

	protected long mMinDelay;

	public ContinuousTask(Map map, long minDelay) {
		mMap = map;
		mMinDelay = minDelay;
	}

	@Override
	public void run() {

		synchronized (this) {
			//System.out.println("run " + mRunning + " " 
			// + mCancel + " " + mDelayed + " " + mWait);

			if (mCancel) {
				mCancel = false;
				mRunning = false;
				mDelayed = false;
				mWait = false;
				cleanup();
				return;
			}
			if (mDelayed) {
				// entered on main-loop
				mDelayed = false;
				mWait = false;
				// unset running temporarily
				mRunning = false;
				submit(0);
				return;
			}
		}

		doWork();

		synchronized (this) {
			mRunning = false;

			if (mCancel)
				cleanup();
			else if (mWait)
				submit(mMinDelay);

			mCancel = false;
			mWait = false;
		}
	}

	public abstract void doWork();

	public abstract void cleanup();

	public synchronized void submit(long delay) {
		//System.out.println("submit " + mRunning + " " + mCancel + " " + delay);

		if (mRunning) {
			mWait = true;
			return;
		}
		mRunning = true;
		if (delay <= 0) {
			mMap.addTask(this);
			return;
		}

		mDelayed = true;
		mMap.postDelayed(this, delay);

	}

	public synchronized void cancel() {
		if (mRunning) {
			mCancel = true;
			return;
		}

		cleanup();
	}
}
