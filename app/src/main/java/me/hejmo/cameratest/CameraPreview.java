package me.hejmo.cameratest;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by scott on 3/29/16.
 *
 * @author zhangqinglian
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "camera_test";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private LocalMediaScanner mScanner;
    private MediaPlayer mCameraVoice ;
    private Handler mWork;

    public CameraPreview(Context context,Camera camera) {
        super(context);
        Log.d(TAG, "CameraPreview");
        mScanner = new LocalMediaScanner(context);
        mWork = new Handler();
        mCameraVoice = MediaPlayer.create(context, R.raw.shutter);
        startCamera(camera);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.setKeepScreenOn(true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        setUpCamera(mCamera);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        Log.d(TAG, "surfaceChanged");
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            camera.startPreview();
            File pictureFile = CameraUtils.getOutputMediaFile(CameraUtils.MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }


            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.flush();
                fos.close();
                mScanner.scanFile(new String[]{pictureFile.getAbsolutePath()}, new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(pictureFile).toString()))});
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


    private Camera.ShutterCallback mShutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            mCameraVoice.start();
        }
    };
    public boolean isCameraAccess() {
        return mCamera == null ? false : true;
    }

    public void tryToTakePhoto(boolean shouldFocus) {
        if(shouldFocus){
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        camera.takePicture(mShutter, null, mPicture);
                    }
                }
            });
        }else{
            mCamera.takePicture(mShutter, null, mPicture);
        }

    }

    public void stopPreview(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);

            releaseCamera();
            mCamera = null;

        }
    }
    private void releaseCamera() {
        Log.d(TAG, "releaseCamera");
        mCamera.release();
    }

    public void startCamera(Camera camera) {
        Log.d(TAG, "startCamera");
        mCamera = camera;
    }

    public void onDestory(){

        if(mCameraVoice != null){
            mCameraVoice.release();
        }

    }

    public void setUpCamera(Camera camera) {
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                CameraUtils.compressRawData(data, camera);
            }
        });
        double scale = getPreviewSizeScale();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size sizePicture = CameraUtils.getMaxPictureValue(camera);
        parameters.setPictureSize(sizePicture.width, sizePicture.height);
        Camera.Size sizePreView = CameraUtils.getMaxPreviewValue(camera,scale);
        parameters.setPreviewSize(sizePreView.width,sizePreView.height);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setRotation(90);
        //如果需要自动对焦，这句一定要加
        camera.cancelAutoFocus();
        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
    }

    private double getPreviewSizeScale(){
        int width = getWidth();
        int height = getHeight();
        return  height * 1.0 /width;
    }
}
