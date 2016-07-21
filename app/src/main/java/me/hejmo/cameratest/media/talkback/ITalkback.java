package me.hejmo.cameratest.media.talkback;


import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author qinglian.zhang
 */
public abstract class ITalkback implements Runnable, Closeable,DataSender.SenderCallback{

    public static final byte VIDEO_ENCODE_CONFIGURE = 0x01;

    public static final byte VIDEO_ENCODE_FRAME = 0x02;

    public static final String ROLE_INITIATOR = "initiator";

    public static final String ROLE_RESPONDER = "responder";

    public BlockingQueue<VideoEncodeConfig> mConfigs = new LinkedBlockingQueue<>(1);

    public BlockingQueue<VideoEncodeFrame> mFrames = new LinkedBlockingQueue<>();

    Socket mSocket ;

    private DataSender mSender;

    private DataReceiver mReceiver;

    private TalkbackCallback mCallback;

    private String mRole;

    public interface TalkbackCallback extends DataReceiver.ReceiverCallback{
        void onTalkbackConnected();
        void onTalkbackStart();
    }

    public ITalkback(TalkbackCallback callback,String role){
        mCallback = callback;
        mRole = role;
    }
    public void addVideoEncodeConfigure(VideoEncodeConfig configure){
        //Log.d("talkback","add config");
        mConfigs.add(configure);
    }

    public void addVideoEncodeFrame(VideoEncodeFrame frame){
        //Log.d("talkback","add frame");
        mFrames.add(frame);
    }

    @Override
    public byte[] getData(){
        byte[] data = new byte[0];
        try {
            if(mConfigs != null){
                //Log.d("talkback"," take config before");
                VideoEncodeConfig config = mConfigs.take();
                //Log.d("talkback"," take config after");
                data = config.getBytes();
                mConfigs = null;
                return data;
            }
           // Log.d("talkback"," take frame before");
            VideoEncodeFrame frame = mFrames.take();
            //Log.d("talkback"," take frame after");
            data = frame.getBytes();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return data;
    }

    public void onConnected(Socket socket){
        try {
            //if(mRole.equals("initiator")){
                mSender = new DataSender(socket.getOutputStream(),this);
            //}
           // if(mRole.equals("responder")){
                mReceiver = new DataReceiver(socket.getInputStream(),mCallback);
          //  }
            mCallback.onTalkbackConnected();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startTalkback(){
        if(mSender != null){
            mSender.start();
        }
        if(mReceiver != null){
            mReceiver.start();
        }
        mCallback.onTalkbackStart();
    }

    public void stopTalkback(){
        Log.d("talkback","stopTalkback");
        if(mSender != null){
            mSender.stop();
        }
        if(mReceiver != null){
            mReceiver.stop();
        }

        if(mSocket != null){
            Log.d("talkback","socket close");
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket =null;
        }
    }

}
