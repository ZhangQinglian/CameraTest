package me.hejmo.cameratest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends QuickActivity {

    private CameraPreview mPreview;
    private Camera mCamera;

    private interface OpenCameraCallback {
        void finish(Camera camera);
    }

    private class OpenCameraThread extends Thread {

        private OpenCameraCallback mCallback;

        public OpenCameraThread(OpenCameraCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            final Camera camera = CameraHolder.getInstance().openCamera();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (camera == null) {
                        mCallback.finish(null);
                    } else {
                        mCallback.finish(camera);
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
            public void finish( Camera camera) {
                if (camera != null) {
                    initCameraView(camera);
                }
            }
        }).start();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(CameraPreview.TAG, " onResume ");
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

    @Override
    protected void onCreateTaskAsync() {
        mCamera = CameraHolder.getInstance().openCamera();
        Log.d(CameraPreview.TAG,"open camera success");
    }

    @Override
    protected void onCreateTaskAsyncFinish() {
        initCameraView(mCamera);
    }
}
