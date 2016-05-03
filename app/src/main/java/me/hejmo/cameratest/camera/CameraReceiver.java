package me.hejmo.cameratest.camera;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by scott on 4/5/16.
 *
 * @author zhangqinglian
 */
public class CameraReceiver extends CameraConnector {

    private CameraFramesInputStream mCameraFrameInputStream;

    private OnCameraFrameCallback mCallback;

    public interface OnCameraFrameCallback {
        void newCameraFrame(CameraFrame frame);
    }

    public CameraReceiver(Socket socket, OnCameraFrameCallback callback) {
        super("CameraReceiver");
        mCallback = callback;
        mCameraFrameInputStream = new CameraFramesInputStream(socket);
    }

    @Override
    public void close() throws IOException {
        mKeepRunning = false;
        mCameraFrameInputStream.close();
        mCameraFrameInputStream = null;
        changeStatus(STATE_CLOSE);

    }

    @Override
    public void run() {
        changeStatus(STATE_RUNNING);
        while (mKeepRunning) {
            try {
                byte[] buffer = new byte[1024 * 256];
                byte[] frameSize = new byte[4];
                int len = 0;
                if (mCameraFrameInputStream != null) {
                    len = mCameraFrameInputStream.read(frameSize, 0, 4);
                } else {
                    break;
                }
                if (len == -1) {
                    Log.d("camera_test", "  read len = -1 error1");
                    close();
                    break;
                }
                int total = CameraFrame.getSizeFromBytes(frameSize);
                Log.d("camera_test", " frame size = " + total);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(total);
                int current = 0;
                while (current < total) {
                    if (total - current > buffer.length) {
                        len = mCameraFrameInputStream.read(buffer);
                        if (len == -1) {
                            Log.d("camera_test", "  read len = -1 error2");
                            close();
                            break;
                        }
                        baos.write(buffer, 0, len);
                        current += len;
                    } else {
                        len = mCameraFrameInputStream.read(buffer, 0, total - current);
                        if (len == -1) {
                            Log.d("camera_test", "  read len = -1 error3");
                            close();
                            break;
                        }
                        baos.write(buffer, 0, len);
                        current += len;
                    }
                    //Log.d("camera_test", "current = " + current + "   total = " + total);
                }
                byte[] frames = baos.toByteArray();
                if (mCallback != null) {
                    mCallback.newCameraFrame(new CameraFrame(frames, System.currentTimeMillis()));
                }
            } catch (IOException e) {
                Log.d("camera_test", e.getMessage());
            }
        }
    }

}
