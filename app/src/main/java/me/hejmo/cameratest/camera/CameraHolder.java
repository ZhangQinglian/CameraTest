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

    public Camera openCamera() {
        if (!mCameraPermission) {
            return null;
        }
        mCamera = CameraUtils.getCameraInstance();
        if (mCallback != null) {
            mCallback.onCameraOpened();
        }
        return mCamera;
    }

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
            } catch (IOException e) {
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

    public boolean ismCameraPermission() {
        return mCameraPermission;
    }

    public void setmCameraPermission(boolean mCameraPermission) {
        this.mCameraPermission = mCameraPermission;
    }

    private class CameraRawFrames implements Camera.PreviewCallback {

        private int skip = 1;

        private long count = 0;

        public CameraRawFrames(int skip) {
            this.skip = skip;
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

        }
    }

}

