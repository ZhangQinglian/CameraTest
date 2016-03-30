package me.hejmo.cameratest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import me.hejmo.cameratest.artphelper.ARTPHelper;

public class MainActivity extends AppCompatActivity {

    private CameraPreview mPreview;
    private ARTPHelper mARTPHelper;
    private Handler mHandler = new Handler();

    private interface OpenCameraCallback {
        void finish(boolean success, Camera camera);
    }

    private class OpenCameraThread extends Thread {

        private OpenCameraCallback mCallback;

        public OpenCameraThread(OpenCameraCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            final Camera camera = CameraUtils.getCameraInstance();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (camera == null) {
                        mCallback.finish(false, null);
                    } else {
                        mCallback.finish(true, camera);
                    }
                }
            });

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CameraPreview.TAG, " onCreate ");
        setContentView(R.layout.activity_main);
        mARTPHelper = new ARTPHelper(false);
        MyPermissionProcess permissionProcess = new MyPermissionProcess(mHandler, 1000);
        permissionProcess.start();
    }


    private void initCameraView(Camera cameara) {
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, cameara);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        if (mPreview.isCameraAccess()) {
            captureButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mPreview.tryToTakePhoto(false);
                        }
                    }
            );
        }
    }

    private void releaseCameraView() {
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        assert preview != null;
        preview.removeView(mPreview);
        mPreview.onDestory();
        mPreview = null;
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        captureButton.setOnClickListener(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(CameraPreview.TAG, " onPause ");
        releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(CameraPreview.TAG, " onStart ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(CameraPreview.TAG, " onRestart ");
        new OpenCameraThread(new OpenCameraCallback() {
            @Override
            public void finish(boolean success, Camera camera) {
                if (success) {
                    initCameraView(camera);
                }
            }
        }).start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(CameraPreview.TAG, " onResume ");
        mARTPHelper.requestPermissions(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mARTPHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.onDestory();
        }
    }

    private void releaseCamera() {
        if (mPreview != null) {
            mPreview.stopPreview();
            releaseCameraView();
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private class MyPermissionProcess extends ILinkedProcess {

        public MyPermissionProcess(@NonNull Handler handler, @NonNull int delay) {
            super(handler, delay);
        }

        @Override
        public void onStart() {
            mARTPHelper.writeExternalStorage().useCamera().accessFineLocation();
            mARTPHelper.requestPermissions(MainActivity.this);
        }

        @Override
        public void onProcess() {

        }

        @Override
        public void onFinish() {
            new OpenCameraThread(new OpenCameraCallback() {
                @Override
                public void finish(boolean success, Camera camera) {
                    if (success) {
                        initCameraView(camera);
                    }
                }
            }).start();
        }

        @Override
        public boolean isFinish() {
            boolean b = mARTPHelper.isAllPermissionGrant(MainActivity.this);
            Log.d(CameraPreview.TAG, " all permission grant ? " + b);
            return b;
        }
    }
}
