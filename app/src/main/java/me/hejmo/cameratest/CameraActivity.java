package me.hejmo.cameratest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class CameraActivity extends QuickActivity {

    private CameraPreview mPreview;
    private CameraHolder mCameraHolder;

    private interface OpenCameraCallback {
        void finish();
    }

    private class OpenCameraThread extends Thread {

        private OpenCameraCallback mCallback;

        public OpenCameraThread(OpenCameraCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            mCameraHolder.openCamera();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCallback.finish();
                }
            });

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCameraHolder = CameraHolder.getInstance(this);
        super.onCreate(savedInstanceState);
        Log.d(CameraPreview.TAG, " onCreate ");
        setContentView(R.layout.activity_main);
        mPreview = (CameraPreview) findViewById(R.id.camera_preview);
    }


    private void initCameraView() {
        Log.d(CameraPreview.TAG,"  initCameraView");
        mCameraHolder.doCameraPreview(mPreview);
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCameraHolder.tryToTakePhoto(false);
                    }
                }
        );
    }

    private void releaseCameraView() {
        mCameraHolder.doReleaseCamera();
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        captureButton.setOnClickListener(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(CameraPreview.TAG, " onPause ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(CameraPreview.TAG, " onStop ");
        releaseCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(CameraPreview.TAG, " onRestart ");
        new OpenCameraThread(new OpenCameraCallback() {
            @Override
            public void finish() {
                initCameraView();
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
    }

    private void releaseCamera() {
        if (mPreview != null) {
            releaseCameraView();
        }
    }


    @Override
    protected void onCreateTaskAsync() {
        mCameraHolder.openCamera();
        Log.d(CameraPreview.TAG, "open camera success");
    }

    @Override
    protected void onCreateTaskAsyncFinish() {
        initCameraView();
    }
}
