package me.hejmo.cameratest.camera;

import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by scott on 4/5/16.
 * @author zhangqinglian
 */
public class CameraSender extends CameraConnector{

    private CameraFramesOutputStream mCameraFramesOutputStream;

    private BlockingQueue<CameraFrame> mOutGoingFrames;

    public CameraSender(Socket out){
        super("CameraSender");
        mCameraFramesOutputStream = new CameraFramesOutputStream(out);
        mOutGoingFrames = new LinkedBlockingQueue<>(3);
    }


    public void sendCameraFrame(CameraFrame f){
        //并不是每一帧都是必须的，所以这里如果队列已满选择丢弃而不是等待
        //如队列已满，则对队列进行清理
        try {
            boolean b = mOutGoingFrames.add(f);
        }catch (IllegalStateException e){
            mOutGoingFrames.clear();
            Log.d(CameraPreview.TAG," mOutGoingFrames is full,clear");
        }
    }

    @Override
    public void run() {
        changeStatus(STATE_RUNNING);
        while (mKeepRunning){
            try {
                //如果队列没有数据选择等待
                CameraFrame f = mOutGoingFrames.take();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                CameraUtils.compressRawData(f.getData(),30, CameraApplication.getInstance().getCameraHolder().getCameraSize(), byteArrayOutputStream);
                byte[] buffer = byteArrayOutputStream.toByteArray();
                Log.d(CameraPreview.TAG,"    frame size = " + buffer.length);
                mCameraFramesOutputStream.write(CameraFrame.getSizeFromShort(buffer.length));
                mCameraFramesOutputStream.write(byteArrayOutputStream.toByteArray());

                mCameraFramesOutputStream.flush();
            } catch (IOException | InterruptedException e) {
                Log.d(CameraPreview.TAG, e.getMessage());
            }
        }
    }

    @Override
    public void close(){
        Log.d(CameraPreview.TAG,"   camera sender close");
        mKeepRunning = false;
        try {
            mCameraFramesOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        changeStatus(STATE_CLOSE);
    }
}
