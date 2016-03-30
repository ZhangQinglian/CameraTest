package me.hejmo.cameratest;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class LocalMediaScanner implements MediaScannerConnectionClient {

    private MediaScannerConnection mediaScanConn = null;

    private String[] mFilePaths;
    private String[] mMimeTypes;

    private int mCount = 0;

    public LocalMediaScanner(Context context) {
        mediaScanConn = new MediaScannerConnection(context, this);
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
