/*******************************************************************************
 * Copyright 2013 Mario Zechner <badlogicgames@gmail.com>
 * Copyright 2013 Nathan Sweet <nathan.sweet@gmail.com>
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.oscim.utils.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Allows asnynchronous execution of {@link AsyncTask} instances on a separate
 * thread.
 * Needs to be disposed via a call to {@link #dispose()} when no longer used, in
 * which
 * case the executor waits for running tasks to finish. Scheduled but not yet
 * running tasks will not be executed.
 *
 * @author badlogic
 */
public class AsyncExecutor {
    private final ExecutorService executor;
    private final TaskQueue mainloop;

    /**
     * Creates a new AsynchExecutor that allows maxConcurrent {@link Runnable}
     * instances to run in parallel.
     *
     * @param maxConcurrent number of threads.
     */
    public AsyncExecutor(int maxConcurrent, TaskQueue mainloop) {
        this.mainloop = mainloop;
        executor = Executors.newFixedThreadPool(maxConcurrent, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "VtmAsyncExecutor");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        });
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

        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return true;
    }

    /**
     * Waits for running {@link AsyncTask} instances to finish,
     * then destroys any resources like threads. Can not be used
     * after this method is called.
     */
    public void dispose() {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Couldn't shutdown loading thread");
        }
    }
}
