package me.hejmo.cameratest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import me.hejmo.cameratest.artphelper.ARTPHelper;


/**
 * Created by scott on 3/31/16.
 *
 * @author zhangqinglian
 */
public class QuickActivity extends AppCompatActivity {


    private final Object mCreateFlag = new Object();

    private boolean mShouldWaitSetContentView = true;

    private ARTPHelper mARTPHelper;

    private class OnCreateTaskThread extends Thread {
        @Override
        public void run() {
            Log.d(CameraPreview.TAG, "OnCreateTaskThread running");
            onCreateTaskAsync();
            if (mShouldWaitSetContentView) {
                try {
                    synchronized (mCreateFlag) {
                        Log.d(CameraPreview.TAG, "OnCreateTaskThread wait");
                        mCreateFlag.wait();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCreateTaskAsyncFinish();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(CameraPreview.TAG, "OnCreateTaskThread not wait");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCreateTaskAsyncFinish();
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CameraPreview.TAG, " quick oncreate");
        //检查是否支持相机
        if (!checkCameraHardware(this)) {
            finish();
        }

        //权限检测
        mARTPHelper = new ARTPHelper(false);
        mARTPHelper.writeExternalStorage().useCamera().accessFineLocation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !mARTPHelper.isAllPermissionGrant(this)) {
            mARTPHelper.requestPermissions(this);
        } else {
            OnCreateTaskThread t = new OnCreateTaskThread();
            t.start();
        }

    }


    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mShouldWaitSetContentView = false;
        synchronized (mCreateFlag) {
            mCreateFlag.notify();
        }
    }

    protected void onCreateTaskAsync() {
        //subclass maybe need this
    }

    protected void onCreateTaskAsyncFinish() {
        //subclass maybe need this
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mARTPHelper.onRequestPermissionsResult(requestCode, permissions, grantResults, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(QuickActivity.this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                OnCreateTaskThread t = new OnCreateTaskThread();
                t.start();
            }
        });
    }
}
