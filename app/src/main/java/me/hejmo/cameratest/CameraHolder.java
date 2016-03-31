package me.hejmo.cameratest;

import android.hardware.Camera;

/**
 * Created by scott on 3/31/16.
 * @author zhangqinglian
 */
public class CameraHolder {

    private Camera mCamera ;

    private static CameraHolder sCameraHodler ;

    private CameraHolder(){}

    public static CameraHolder getInstance(){
        if(sCameraHodler == null){
            sCameraHodler = new CameraHolder();
        }
        return sCameraHodler;
    }

    public Camera openCamera(){
        mCamera = CameraUtils.getCameraInstance();
        return mCamera ;
    }
}
