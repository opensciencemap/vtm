package org.oscim.utils.async;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;

/**
 * GWT emulation of AsynchExecutor, will call tasks immediately :D
 *
 * @author badlogic
 */
public class AsyncExecutor implements Disposable {
    private final TaskQueue mainloop;

    /**
     * Creates a new AsynchExecutor that allows maxConcurrent {@link Runnable}
     * instances to run in parallel.
     *
     * @param maxConcurrent
     */
    public AsyncExecutor(int maxConcurrent, TaskQueue mainloop) {
        this.mainloop = mainloop;
    }

    /**
     * Submits a {@link Runnable} to be executed asynchronously. If
     * maxConcurrent runnables are already running, the runnable
     * will be queued.
     *
     * @param task the task to execute asynchronously
     */
    public boolean post(Runnable task) {
        if (task instanceof AsyncTask) {
            ((AsyncTask) task).setTaskQueue(mainloop);
        }

        Gdx.app.postRunnable(task);

        return true;
    }

    /**
     * Waits for running {@link AsyncTask} instances to finish,
     * then destroys any resources like threads. Can not be used
     * after this method is called.
     */
    @Override
    public void dispose() {
    }
}
