package me.hejmo.cameratest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理者，向线程池中提交各种耗时任务
 */
public enum ThreadPoolManager {
    INSTANCE;
    private ExecutorService mExecutorService = null;

    private ThreadPoolManager() {
        // 最多有Integer.MAX_VALUE个线程，当线程等待10秒未执行任务时终止
        mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }

    public void execute(Runnable r) {
        mExecutorService.execute(r);
    }

    // CoffeeMateActivity退出时close
    public void close() {
        mExecutorService.shutdown();
    }
}
