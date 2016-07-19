package me.hejmo.cameratest.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.LocalMediaScanner;
import me.hejmo.cameratest.R;

/**
 * Created by scott on 3/31/16.
 *
 * @author zhangqinglian
 */
public class CameraHolder {

    private Camera mCamera;

    private LocalMediaScanner mScanner;

    private MediaPlayer mCameraVoice;

    private Context mContext;

    private boolean mTakeNextPhoto = true;


    private boolean mCameraPermission = true;

    private static CameraHolder sCameraHolder;

    private CameraCallback mCallback;

    private CameraRawFrames mCameraFrames;

    private CameraSender mCameraSender;

    private MyOrientationEventListener myOrientationEventListener;

    private Camera.Parameters mParameters;

    private int mRotation = 0;

    private List<SavePictureTask> mSavePictureTasks = new ArrayList<>(5);

    private final Object mReleaseFlag = new Object();

    private SurfaceHolder mSurfaceHolder ;

    public interface CameraCallback {
        void onCameraOpened();

        void onCameraPreviewed();

        void onCameraReleased();

        void onPictureTaken(String picturePath);
    }

    private CameraHolder(Context context) {
        mContext = context;
        mScanner = new LocalMediaScanner(mContext);
        mCameraVoice = MediaPlayer.create(mContext, R.raw.shutter);
        mCameraFrames = new CameraRawFrames(1);
        myOrientationEventListener = new MyOrientationEventListener(mContext, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public static CameraHolder getInstance(Context context) {
        if (sCameraHolder == null) {
            sCameraHolder = new CameraHolder(context);
        }
        return sCameraHolder;
    }

    /**
     * 相机打开，预览，关闭等操作回调
     *
     * @param callback 回调类
     */
    public void setCameraCallback(CameraCallback callback) {
        mCallback = callback;
    }

    public void openCamera() {
        if (!mCameraPermission) {
            return;
        }
        mCamera = CameraUtils.getCameraInstance();
        if (mCallback != null) {
            mCallback.onCameraOpened();
        }

    }

    public void doCameraPreview(SurfaceView surfaceView) {
        if (myOrientationEventListener.canDetectOrientation()) {
            myOrientationEventListener.enable();
        }
        if (!mCameraPermission) {
            return;
        }
        if (mCamera != null) {
            mCamera.stopPreview();

            try {
                mSurfaceHolder = surfaceView.getHolder();
                mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                mSurfaceHolder.setKeepScreenOn(true);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.setPreviewCallback(mCameraFrames);
            } catch (IOException e) {
                e.printStackTrace();
            }

            setUpCamera(mCamera, surfaceView);

            mCamera.startPreview();

            if (mCallback != null) {
                mCallback.onCameraPreviewed();
            }
        }
    }

    public void doReleaseCamera() {
        synchronized (mReleaseFlag) {
            if (mCameraSender != null) {
                mCameraSender.close();
                mCameraSender = null;
            }

            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            if (mCallback != null) {
                mCallback.onCameraReleased();
            }
            if (myOrientationEventListener.canDetectOrientation()) {
                myOrientationEventListener.disable();
            }
        }

    }

    private void setUpCamera(Camera camera, SurfaceView surfaceView) {
        double scale = getPreviewSizeScale(surfaceView);
        mParameters = camera.getParameters();
        Camera.Size sizePicture = CameraUtils.getSuitablePictureSize(camera);
        Camera.Size sizePreView = CameraUtils.getSuitablePreviewSize(camera, scale);
        Log.d(CameraPreview.TAG,"pre.w = " + sizePreView.width + "  pre.h = " + sizePreView.height);
        Log.d(CameraPreview.TAG,"pic.w = " + sizePicture.width + "  pic.h = " + sizePicture.height);
        mParameters.setPictureSize(sizePicture.width, sizePicture.height);

        mParameters.setPreviewSize(sizePreView.width, sizePreView.height);
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mParameters.setRotation(0);
        //如果需要自动对焦，这句一定要加
        camera.cancelAutoFocus();
        camera.setParameters(mParameters);
        camera.setDisplayOrientation(CameraUtils.getBackCameraOrientation());
    }

    private double getPreviewSizeScale(SurfaceView surfaceView) {
        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();
        return height * 1.0 / width;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(CameraPreview.TAG, "" + Thread.currentThread().getName());
            camera.startPreview();
            mTakeNextPhoto = true;
            //save the picture in a new thread
            SavePictureTask task = new SavePictureTask(data,System.currentTimeMillis());
            Log.d(CameraPreview.TAG,"  post new SavePictureTask : " + task.taskTime);
            mSavePictureTasks.add(task);
            Executors.newSingleThreadExecutor().execute(task);

        }
    };

    private class SavePictureTask implements Runnable {

        private byte[] imageData;

        private long taskTime;
        public SavePictureTask(byte[] data,long time) {
            taskTime = time;
            imageData = data;
        }

        @Override
        public void run() {
            File pictureFile = CameraUtils.getOutputMediaFile(CameraUtils.MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(CameraPreview.TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            try {
                Bitmap rotationed = CameraUtils.rotaingImageView(mRotation, BitmapFactory.decodeByteArray(imageData,0,imageData.length));
                FileOutputStream fos = new FileOutputStream(pictureFile);
                rotationed.compress(Bitmap.CompressFormat.JPEG,100,fos);
                rotationed.recycle();
                fos.flush();
                fos.close();
                mScanner.scanFile(new String[]{pictureFile.getAbsolutePath()}, new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(pictureFile).toString()))});
                mCallback.onPictureTaken(pictureFile.getAbsolutePath());

            } catch (FileNotFoundException e) {
                Log.d(CameraPreview.TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(CameraPreview.TAG, "Error accessing file: " + e.getMessage());
            }finally {
                mSavePictureTasks.remove(this);
            }
        }
    }

    private Camera.ShutterCallback mShutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            //mCameraVoice.start();
        }
    };

    public void tryToTakePhoto(boolean shouldFocus) {
        if(mSavePictureTasks.size()>5){
            return ;
        }
        if (mTakeNextPhoto) {
            mTakeNextPhoto = false;
            if (shouldFocus) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            camera.takePicture(mShutter, null, mPicture);
                        }
                    }
                });
            } else {
                mCamera.takePicture(mShutter, null, mPicture);
            }
        }
    }

    public boolean ismCameraPermission() {
        return mCameraPermission;
    }

    public void setmCameraPermission(boolean mCameraPermission) {
        this.mCameraPermission = mCameraPermission;
    }


    public void registerCameraSender(CameraSender sender) {
        mCameraSender = sender;
        mCameraSender.start();
    }

    private class CameraRawFrames implements Camera.PreviewCallback {

        private int skip = 1;

        private long count = 0;

        public CameraRawFrames(int skip) {
            this.skip = skip;
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (data != null) {
                if (mCameraSender != null) {

                    if (count % skip == 0) {
                        mCameraSender.sendCameraFrame(new CameraFrame(data, System.currentTimeMillis()));
                    }
                    count++;
                    if (count == Long.MAX_VALUE) {
                        count = 0;
                    }
                }
            }
        }
    }

    public Point getCameraSize() {
        synchronized (mReleaseFlag) {
            if (mCamera != null) {
                return new Point(mCamera.getParameters().getPreviewSize().width, mCamera.getParameters().getPreviewSize().height);
            } else {
                return new Point(1920, 1080);
            }
        }

    }

    public int getCameraFormat() {
        synchronized (mReleaseFlag) {
            if (mCamera != null) {
                return mCamera.getParameters().getPreviewFormat();
            } else {
                return ImageFormat.YUY2;
            }
        }
    }


    class MyOrientationEventListener extends OrientationEventListener {

        public MyOrientationEventListener(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN) return;
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(CameraUtils.getBackCameraId(), info);
            orientation = (orientation + 45) / 90 * 90;
            int rotation = 0;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
            if(mRotation != rotation){
                Log.d(CameraPreview.TAG,"set orientation : " + mRotation + " -> " + rotation);
                mRotation = rotation;
            }

        }

    }
}

