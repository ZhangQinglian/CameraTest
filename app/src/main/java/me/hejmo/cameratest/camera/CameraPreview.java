package me.hejmo.cameratest.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
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
    private CameraHolder mCameraHolder;
    private Handler mHandler;
    private Runnable mOpenCameraWorker = new Runnable() {
        @Override
        public void run() {
            mCameraHolder.openCamera();
            mCameraHolder.doCameraPreview(CameraPreview.this);
        }
    };


    public CameraPreview(Context context,AttributeSet attributeSet) {
        super(context,attributeSet);
        Log.d(TAG, "CameraPreview");
        mHolder = getHolder();
        mHandler = new Handler(Looper.getMainLooper());
        mCameraHolder = CameraApplication.getInstance().getCameraHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;

        mHandler.postDelayed(mOpenCameraWorker,50);
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mHandler.removeCallbacks(mOpenCameraWorker);
        mCameraHolder.doReleaseCamera();
    }

}
