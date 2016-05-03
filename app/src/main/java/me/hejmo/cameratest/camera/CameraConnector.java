package me.hejmo.cameratest.camera;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by scott on 4/5/16.
 * @author zhangqinglian
 */
public abstract class CameraConnector extends Thread implements Closeable{


    protected boolean mKeepRunning = true;

    public static final int STATE_RUNNING = 0x01;

    public static final int STATE_CLOSE = 0x02;

    public int mCurrentState = -1;

    public CameraConnector(String name){
        super(name);
    }

    protected void changeStatus(int state){
        mCurrentState = state;
    }

}
