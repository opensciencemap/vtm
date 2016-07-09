package org.oscim.utils;

public class ThreadUtils {

    private static Thread MAIN_THREAD;

    public static void assertMainThread() {
        if (MAIN_THREAD != Thread.currentThread())
            throw new RuntimeException("Access from non-main thread!");
    }

    public static boolean isMainThread() {
        return MAIN_THREAD == Thread.currentThread();
    }

    public static void init() {
        MAIN_THREAD = Thread.currentThread();
    }

}
