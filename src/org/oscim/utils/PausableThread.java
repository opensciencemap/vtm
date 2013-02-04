/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.utils;

/**
 * An abstract base class for threads which support pausing and resuming.
 */
public abstract class PausableThread extends Thread {
	private boolean mPausing = true;
	private boolean mShouldPause;

	/**
	 * Causes the current thread to wait until this thread is pausing.
	 */
	public final void awaitPausing() {
		synchronized (this) {
			while (!isInterrupted() && !isPausing()) {
				try {
					wait(100);
				} catch (InterruptedException e) {
					// restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	@Override
	public void interrupt() {
		// first acquire the monitor which is used to call wait()
		synchronized (this) {
			super.interrupt();
		}
	}

	/**
	 * @return true if this thread is currently pausing, false otherwise.
	 */
	public final synchronized boolean isPausing() {
		return mPausing;
	}

	/**
	 * The thread should stop its work temporarily.
	 */
	public final synchronized void pause() {
		if (!mShouldPause) {
			mShouldPause = true;
			takeabreak();
			notify();
		}
	}

	/**
	 * The paused thread should continue with its work.
	 */
	public final synchronized void proceed() {
		if (mShouldPause) {
			mShouldPause = false;
			mPausing = false;
			afterPause();
			notify();
		}
	}

	@Override
	public final void run() {
		setName(getThreadName());
		setPriority(getThreadPriority());

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && (mShouldPause || !hasWork())) {
					try {
						if (mShouldPause) {
							mPausing = true;
						}
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			try {
				doWork();
			} catch (InterruptedException e) {
				// restore the interrupted status
				interrupt();
			}
		}

		afterRun();
	}

	/**
	 * Called once when this thread continues to work after a pause. The default
	 * implementation is empty.
	 */
	protected void afterPause() {
		// do nothing
	}

	protected void takeabreak() {
		// do nothing
	}

	/**
	 * Called once at the end of the {@link #run()} method. The default
	 * implementation is empty.
	 */
	protected void afterRun() {
		// do nothing
	}

	/**
	 * Called when this thread is not paused and should do its work.
	 *
	 * @throws InterruptedException
	 *             if the thread has been interrupted.
	 */
	protected abstract void doWork() throws InterruptedException;

	/**
	 * @return the name of this thread.
	 */
	protected abstract String getThreadName();

	/**
	 * @return the priority of this thread. The default value is
	 *         {@link Thread#NORM_PRIORITY}.
	 */
	protected int getThreadPriority() {
		return Thread.NORM_PRIORITY;
	}

	/**
	 * @return true if this thread has some work to do, false otherwise.
	 */
	protected abstract boolean hasWork();
}
