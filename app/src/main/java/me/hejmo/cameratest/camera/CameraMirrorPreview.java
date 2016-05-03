package me.hejmo.cameratest.camera;

import android.content.Context;
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
public class CameraMirrorPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "camera_test";
    private SurfaceHolder mHolder;
    private boolean mEnable = false ;

    public CameraMirrorPreview(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "CameraPreview");
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        mEnable = true;
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mEnable = false ;
    }

    public boolean isSurfaceEnable(){
        return mEnable;
    }

    public SurfaceHolder getSurfaceHolder(){
        return mHolder;
    }

}
