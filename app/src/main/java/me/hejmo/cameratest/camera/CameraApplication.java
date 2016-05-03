package me.hejmo.cameratest.camera;

import android.app.Application;

/**
 * Created by scott on 4/1/16.
 * @author zhangqinglian
 */
public class CameraApplication extends Application{

    CameraHolder mCameraHolder ;

    private static CameraApplication sInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        mCameraHolder = CameraHolder.getInstance(this);
        sInstance = this;
    }


    public CameraHolder getCameraHolder(){
        return mCameraHolder;
    }

    public static CameraApplication getInstance(){
        return sInstance;
    }
}
