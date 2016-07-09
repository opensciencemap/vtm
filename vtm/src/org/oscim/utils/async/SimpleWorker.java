package org.oscim.utils.async;

import org.oscim.map.Map;

/**
 * Simple 'Double Buffering' worker for running Tasks on AsyncExecutor
 * thread.
 */
public abstract class SimpleWorker<T> implements Runnable {

    protected final Map mMap;

    protected boolean mRunning;
    protected boolean mWait;
    protected boolean mCancel;
    protected boolean mDelayed;

    protected long mMinDelay;

    /**
     * Stuff which can be processed on the worker thread.
     */
    protected T mTaskTodo;

    /**
     * Stuff that is done an ready for being fetched by poll().
     */
    protected T mTaskDone;

    /**
     * Stuff that is ready - will not be modified in the worker.
     */
    protected T mTaskLocked;

    public SimpleWorker(Map map, long minDelay, T t1, T t2) {
        mMap = map;
        mMinDelay = minDelay;

        mTaskTodo = t1;
        mTaskLocked = t2;
    }

    @Override
    public void run() {

        synchronized (this) {
            if (mCancel) {
                mCancel = false;
                mRunning = false;
                mDelayed = false;
                mWait = false;
                if (mTaskTodo != null)
                    cleanup(mTaskTodo);
                finish();
                return;
            }

            // FIXME: mTaskTodo == null?
            if (mDelayed || mTaskTodo == null) {

                if (mDelayed && mTaskTodo != null)
                    onMainLoop(mTaskTodo);

                // entered on main-loop
                mDelayed = false;
                // unset running temporarily
                mRunning = false;
                submit(0);
                return;
            }
        }

        boolean done = doWork(mTaskTodo);

        synchronized (this) {
            mRunning = false;

            if (mCancel) {
                cleanup(mTaskTodo);
                finish();
                mCancel = false;
            } else if (done) {
                mTaskDone = mTaskTodo;
                mTaskTodo = null;
            } else if (mWait) {
                // only submit if not 'done'
                // as otherwise there is no
                // mStuffTodo
                submit(mMinDelay);
                mWait = false;
            }
        }
    }

    public abstract boolean doWork(T task);

    public abstract void cleanup(T task);

    public void finish() {

    }

    /**
     * do stuff on main-loop before executing the task
     */
    public void onMainLoop(T task) {

    }

    /**
     * If delay > 0 onMainLoop will be called before Task
     * is passed to worker-thread
     */
    public synchronized void submit(long delay) {

        if (mRunning) {
            mWait = true;
            return;
        }

        mRunning = true;
        if (delay <= 0) {
            mMap.addTask(this);
            return;
        }

        if (!mDelayed) {
            mDelayed = true;
            mMap.postDelayed(this, delay);
        }
    }

    public synchronized T poll() {
        if (mTaskDone == null)
            return null;

        cleanup(mTaskLocked);
        mTaskTodo = mTaskLocked;

        mTaskLocked = mTaskDone;
        mTaskDone = null;

        if (mWait) {
            submit(mMinDelay);
            mWait = false;
        }

        return mTaskLocked;
    }

    public synchronized void cancel(boolean clear) {
        if (mRunning) {
            mCancel = true;
            return;
        }

        if (mTaskTodo != null)
            cleanup(mTaskTodo);
        finish();
    }

    public synchronized boolean isRunning() {
        return mRunning;
    }

}
