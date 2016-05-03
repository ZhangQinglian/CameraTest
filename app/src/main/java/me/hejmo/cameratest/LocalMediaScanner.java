package me.hejmo.cameratest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import me.hejmo.cameratest.camera.CameraHolder;
import me.hejmo.cameratest.camera.CameraPreview;

public class LocalMediaScanner implements MediaScannerConnectionClient {

    private MediaScannerConnection mediaScanConn = null;

    private String[] mFilePaths;
    private String[] mMimeTypes;

    private int mCount = 0;
    private Context mContext;

    public LocalMediaScanner(Context context) {
        mContext = context;
        mediaScanConn = new MediaScannerConnection(mContext, this);
    }

    public void scanFile(String[] filePaths, String[] mimeTypes) {
        mFilePaths = filePaths;
        mMimeTypes = mimeTypes;
        mediaScanConn.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        for (int i = 0; i < mFilePaths.length; i++) {
            mediaScanConn.scanFile(mFilePaths[i], mMimeTypes[i]);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mCount++;
        if (mCount == mFilePaths.length) {
            mediaScanConn.disconnect();
            mCount = 0;
            mFilePaths = null;
            mMimeTypes = null;
        }
    }

}
