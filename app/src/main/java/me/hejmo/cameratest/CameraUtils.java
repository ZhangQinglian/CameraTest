package me.hejmo.cameratest;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by scott on 3/29/16.
 * @author zhangqinglian
 */
public class CameraUtils {


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public static void dumpCameraParams(Camera camera){
        Camera.Parameters parameters= camera.getParameters();
        Log.d(CameraPreview.TAG,parameters.flatten());
    }

    public static Camera.Size getMaxPreviewValue(Camera camera,double scale) {
        Camera.Size size = null;

        List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return rhs.width - lhs.width;
            }
        });
        Log.d(CameraPreview.TAG, "scale : " + scale);
        List<Integer> index = new ArrayList<>(10);
        for (Camera.Size s : sizes) {
            double sc = (s.width * 1.0 / s.height);
            Log.d(CameraPreview.TAG, "" + s.width + " * " + s.height + "  s = " + sc);

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


    public static Camera.Size getMaxPictureValue(Camera camera) {
        Camera.Size size = null;

        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        for (Camera.Size s : sizes) {
            if (size == null) {
                size = s;
            }
            if (s.width > size.width) {
                size = s;
            }
            //Log.d(CameraPreview.TAG, "" + s.width + " * " + s.height);
        }
        return size;
    }

    public static void compressRawData(byte[] data,Camera camera){
//        YuvImage yuvImage = new YuvImage(data, ImageFormat.JPEG,camera.getParameters().getPictureSize().width,camera.getParameters().getPictureSize().height,null);
        //Log.d(CameraPreview.TAG,"       frame size : " + data.length/1000 + "k");
    }


}
