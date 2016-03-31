package me.hejmo.cameratest;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by scott on 3/31/16.
 * @author zhangqinglian
 */
public class CameraHolder {

    private Camera mCamera ;

    private LocalMediaScanner mScanner;

    private MediaPlayer mCameraVoice ;

    private Context mContext;
    private static CameraHolder sCameraHodler ;

    private CameraHolder(Context context){
        mContext = context;
        mScanner = new LocalMediaScanner(context);
        mCameraVoice = MediaPlayer.create(context,R.raw.shutter);
    }

    public static CameraHolder getInstance(Context context){
        if(sCameraHodler == null){
            sCameraHodler = new CameraHolder(context);
        }
        return sCameraHodler;
    }

    public void openCamera(){
        mCamera = CameraUtils.getCameraInstance();
    }

    public void doCameraPreview(SurfaceView surfaceView){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);

            try {
                SurfaceHolder holder = surfaceView.getHolder();
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                holder.setKeepScreenOn(true);
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            setUpCamera(mCamera,surfaceView);
            mCamera.startPreview();
        }
    }

    public void doReleaseCamera(){
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private void setUpCamera(Camera camera,SurfaceView surfaceView) {
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                CameraUtils.compressRawData(data, camera);
            }
        });
        double scale = getPreviewSizeScale(surfaceView);
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size sizePicture = CameraUtils.getMaxPictureValue(camera);
        parameters.setPictureSize(sizePicture.width, sizePicture.height);
//        Camera.Size sizePreView = CameraUtils.getMaxPreviewValue(camera,scale);
//        parameters.setPreviewSize(sizePreView.width,sizePreView.height);
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setRotation(90);
        //如果需要自动对焦，这句一定要加
        camera.cancelAutoFocus();
        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
    }

    private double getPreviewSizeScale(SurfaceView surfaceView){
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        return  height * 1.0 /width;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            camera.startPreview();
            File pictureFile = CameraUtils.getOutputMediaFile(CameraUtils.MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(CameraPreview.TAG, "Error creating media file, check storage permissions: ");
                return;
            }


            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.flush();
                fos.close();
                mScanner.scanFile(new String[]{pictureFile.getAbsolutePath()}, new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(pictureFile).toString()))});
            } catch (FileNotFoundException e) {
                Log.d(CameraPreview.TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(CameraPreview.TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private Camera.ShutterCallback mShutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            //mCameraVoice.start();
        }
    };
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
}
