package me.hejmo.cameratest;

import android.os.Handler;
import android.support.annotation.NonNull;

public abstract class ILinkedProcess {

    private Handler mWorkHandler;

    private long mDelay;

    private boolean mContinue = true;

    private ILinkedProcess mProcess;

    /**
     *
     * @param handler 用于处理事务
     * @param delay 每隔多少秒触发一次检测
     */
    public ILinkedProcess(@NonNull Handler handler, @NonNull int delay) {
        mWorkHandler = handler;
        mDelay = delay;
    }

    /**
     *
     * @param handler 用于处理事务
     * @param delay 每隔多少毫秒触发一次检查
     * @param process 当当前事务结束后需进行的下一个事务
     */
    public ILinkedProcess(@NonNull Handler handler, @NonNull int delay, ILinkedProcess process) {
        this(handler, delay);
        mProcess = process;
    }

    /**
     * 开始执行
     */
    public void start() {
        onStart();
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {

                if (isFinish()) {
                    onFinish();
                    if (mProcess != null) {
                        mProcess.start();
                    }
                } else {
                    onProcess();
                    if (mContinue) {
                        mWorkHandler.postDelayed(this, mDelay);
                    }
                }
            }
        });
    }

    /**
     * 停止状态检测
     */
    public void stop() {
        mContinue = false;
    }

    /**
     * start 回调
     */
    public abstract void onStart();

    /**
     * 当isFinish为false时执行
     */
    public abstract void onProcess();

    /**
     * 当isFinish为true的时候执行
     */
    public abstract void onFinish();

    /**
     * 条件判断
     * @return
     */
    public abstract boolean isFinish();
}
