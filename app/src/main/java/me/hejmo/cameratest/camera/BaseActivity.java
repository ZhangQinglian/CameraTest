package me.hejmo.cameratest.camera;

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
public class BaseActivity extends AppCompatActivity {


    private final Object mCreateFlag = new Object();

    private boolean mShouldWaitSetContentView = true;

    private ARTPHelper mARTPHelper;

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
            CameraHolder.getInstance(this).setmCameraPermission(false);
        }else{

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mARTPHelper.onRequestPermissionsResult(requestCode, permissions, grantResults, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                CameraHolder.getInstance(BaseActivity.this).setmCameraPermission(true);
            }
        });
    }
}
