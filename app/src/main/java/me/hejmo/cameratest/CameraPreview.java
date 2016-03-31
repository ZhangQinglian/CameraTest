package me.hejmo.cameratest;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by scott on 3/29/16.
 *
 * @author zhangqinglian
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "camera_test";
    private SurfaceHolder mHolder;

    public CameraPreview(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);
        Log.d(TAG, "CameraPreview");
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }


}
