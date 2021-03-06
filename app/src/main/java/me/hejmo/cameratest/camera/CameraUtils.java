package me.hejmo.cameratest.camera;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by scott on 3/29/16.
 *
 * @author zhangqinglian
 */
public class CameraUtils {


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final String[] ID_PROJECTION = {MediaStore.Images.ImageColumns._ID};

    public static final String[] DATA_PROJECTION = {MediaStore.Images.ImageColumns.DATA};

    public static final String IMAGE_DATA_WHERE = MediaStore.Images.ImageColumns.DATA+" like ?";

    /**
     * Create a file Uri for saving an image or video
     */
    public static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static Uri getLatestPhotoUri(Context context){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri,ID_PROJECTION,IMAGE_DATA_WHERE,new String[]{mediaStorageDir.getAbsolutePath()+"%"},MediaStore.Images.ImageColumns.DATE_ADDED + " desc");
        if(c != null){
            try{
                while(c.moveToNext()){
                    int id = c.getInt(0);
                    if(id >0){
                        return Uri.withAppendedPath(uri,String.valueOf(id));
                    }
                }
            }finally {
                c.close();
            }
        }
        return uri;
    }

    public static String getLatestPhotoPath(Context context){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri,DATA_PROJECTION,IMAGE_DATA_WHERE,new String[]{mediaStorageDir.getAbsolutePath()+"%"},MediaStore.Images.ImageColumns.DATE_ADDED + " desc");
        if(c != null){
            try{
                while(c.moveToNext()){
                    String data = c.getString(0);
                    if(data != null && data.trim().length() > 0){
                        return data ;
                    }
                }
            }finally {
                c.close();
            }
        }
        return null ;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            if(getBackCameraId() != -1){
                c = Camera.open(getBackCameraId());
            }else{
                c = Camera.open();
            }
            // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static int getBackCameraOrientation(){
        Camera.CameraInfo info = new Camera.CameraInfo();
        int backCameraId ;
        for(int i = 0;i<Camera.getNumberOfCameras();i++){
            Camera.getCameraInfo(i,info);
            if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
                return info.orientation;
            }
            Log.d("info","  facing" + info.facing + "      orientation + " + info.orientation);
        }
        //default 90
        return 90;
    }

    public static int getBackCameraId(){
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return -1;
    }
    public static void dumpCameraParams(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Log.d(CameraPreview.TAG, parameters.flatten());
    }

    public static Camera.Size getSuitablePreviewSize(Camera camera, double scale) {
        Camera.Size size = null;

        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                //从小到大排列
                return lhs.width - rhs.width;
            }
        });
        Log.d(CameraPreview.TAG, "scale : " + scale);
        List<Integer> index = new ArrayList<>(10);
        for (Camera.Size s : sizes) {
            double sc = (s.width * 1.0 / s.height);

            if (size == null) {
                size = s;
            }
            if (sc == scale) {
                size = s;
                break;
            }

        }
        return size;
    }

    public static Camera.Size getSuitablePictureSize(Camera camera) {
        Camera.Size size = null;


        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                //从小到大排列
                return  rhs.width-lhs.width;
            }
        });
        for (Camera.Size s : sizes) {
            if(s.width < 2000 && s.height<2000){
                if (size == null) {
                    size = s;
                }
                if (s.width > size.width) {
                    size = s;
                }
            }
        }
        Log.d(CameraPreview.TAG, "picture size " + size.width + " * " + size.height);
        return size;
    }

    public static void compressRawData(byte[] data, int quality,Point pictureSize, ByteArrayOutputStream byteArrayOutputStream) {

        YuvImage yuvImage = new YuvImage(data, CameraApplication.getInstance().getCameraHolder().getCameraFormat(), pictureSize.x, pictureSize.y, null);

        yuvImage.compressToJpeg(new Rect(0, 0, pictureSize.x, pictureSize.y), quality, byteArrayOutputStream);
    }


    public static int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();;
        matrix.postRotate(angle);
        System.out.println("angle2=" + angle);
        // 创建新的图片
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }
}
