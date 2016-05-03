package me.hejmo.cameratest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

/**
 * 视频缩略图的管理类
 */
public enum ThumbManager {
    INSTANCE;

    // 将生成的缩略图数据保存到内存中，这些map都是线程同步的，key－value：原图path-缩略图的path
    private Map<String, String> mPhotoMap;

    private ThumbManager() {
        mPhotoMap = Collections.synchronizedMap(new HashMap<String, String>());
    }


    private boolean createPhotoThumb(String imagePath,String thumbDir) {

        File srcFile = new File(imagePath);
        if(!srcFile.exists()){
            return false ;
        }
        // 图片的路径
        // 缩略图的名称前缀、名称、路径
        final String thumbNamePrefix = String.valueOf(imagePath.hashCode());
        String thumbName = thumbNamePrefix + "_" + new File(imagePath).lastModified();
        String thumbPath = thumbDir + File.separator + thumbName;
        // 如果缩略图存在，将缩略图数据添加到内存
        if (new File(thumbPath).exists()) {
            mPhotoMap.put(imagePath, thumbPath);
            return true;
        }
        // 否则删除可能存在的过期缩略图（原图可能修改过），然后重新生成
        File[] files = new File(thumbDir).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(thumbNamePrefix);
            }
        });
        if (files != null && files.length > 0) {
            for (File file : files) {
                file.delete();
            }
        }
        // 开始生成缩略图
        // 获取原图的大小
        int imageWidth = 0;
        int imageHeight = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 如果DmImage对象未保存原图大小，需要解析图片获取大小
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        imageWidth = options.outWidth;
        imageHeight = options.outHeight;
        // 设置图片需要压缩的比例，压缩后图片长宽比例不变；设置解析质量优先于解析速度
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        if (imageWidth > 80 || imageHeight > 80) {
            final int widthRatio = Math.round((float) imageWidth / (float) 80);
            final int heightRatio = Math.round((float) imageHeight / (float) 80);
            options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        options.inPreferQualityOverSpeed = false;
        // 解析图片
        Bitmap thumb = BitmapFactory.decodeFile(imagePath, options);
        if (thumb == null) {
            return false;
        }
        try {
            // 旋转图片
            int orientation = getExifOrientation(imagePath);
            if (orientation != 0) {
                thumb = rotateBitmap(thumb, orientation);
            }
            // 继续裁减图片，使得长宽符合网页图片大小
            thumb = ThumbnailUtils.extractThumbnail(thumb, (int) (80), (int) (80),
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            // 保存图片
            FileOutputStream fos = new FileOutputStream(thumbPath);
            thumb.compress(CompressFormat.PNG, 100, fos);
            fos.close();
            // 将缩略图数据添加到内存
            mPhotoMap.put(imagePath, thumbPath);
            // 回收图片内存，避免OOM
            thumb.recycle();
            thumb = null;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取图片的旋转角度
     */
    public static int getExifOrientation(String filepath) {
        if (!new File(filepath).exists()) {
            return 0;
        }
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
        }
        if (null != exif) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
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
                default:
                    break;
                }
            }
        }
        return degree;
    }

    /**
     * 旋转图片
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        // 旋转图片
        if (null != bitmap && degree != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        }
        return bitmap;
    }

    /**
     * 获取图片的缩略图
     * 
     * @param path
     *            图片的路径
     * @return 缩略图
     */
    public Bitmap getPhotoThumb(String path,String thumbDir) {
        if (createPhotoThumb(path,thumbDir)) {
            return BitmapFactory.decodeFile(mPhotoMap.get(path));
        } else {
            return null;
        }
    }

    public File getPhotoThumbFile(String path,String thumbDir) {
        if (createPhotoThumb(path,thumbDir)) {
            return new File(mPhotoMap.get(path));
        } else {
            return null;
        }
    }

}
