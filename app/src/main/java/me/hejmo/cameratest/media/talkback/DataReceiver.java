package me.hejmo.cameratest.media.talkback;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by scott on 7/20/16.
 */
public class DataReceiver {

    private InputStream mInputStream;

    private ReadWorker mWorker;

    private ReceiverCallback mCallback;

    public interface ReceiverCallback{
        void onConfig(VideoEncodeConfig config);
        void onNewFrame(VideoEncodeFrame frame);
    }
    public DataReceiver(InputStream inputStream,ReceiverCallback callback){
        mCallback = callback;
        mInputStream = inputStream;

    }

    public void start(){
        if(mWorker == null){
            mWorker = new ReadWorker(mInputStream);
            mWorker.setRunning(true);
            mWorker.start();

        }
    }

    public void stop(){
        if(mWorker != null){
            mWorker.setRunning(false);
            mWorker = null;
        }
    }

    class ReadWorker extends Thread{

        private boolean running = false ;

        private InputStream inputStream;

        public ReadWorker(InputStream is){
            this.inputStream = is;
        }

        public void setRunning(boolean runnung){
            this.running = runnung;
        }
        @Override
        public void run() {
            int type ;
            while(running){

                try {
                    type = inputStream.read();
                    if(type == -1){
                        running = false ;
                        break;
                    }
                    Log.d("talkback","type = " + type);
                    if(type == ITalkback.VIDEO_ENCODE_CONFIGURE){
                        VideoEncodeConfig config = VideoEncodeConfig.getConfig(inputStream);
                        mCallback.onConfig(config);
                        Log.d("talkback","--------- config  = " + config.toString() + "  total = " + config.getBytes().length);
                    }
                    if(type == ITalkback.VIDEO_ENCODE_CONFIGURE){
                        VideoEncodeFrame frame = VideoEncodeFrame.getFrame(inputStream);
                        mCallback.onNewFrame(frame);
                        Log.d("talkback","--------- frame  = " + frame.toString() + "  total = " + frame.getBytes().length);
                    }

                } catch (IOException e) {
                    if(inputStream != null){
                        Log.d("talkback","input stream close");
                        try {
                            inputStream.close();
                        } catch (IOException ee) {
                            ee.printStackTrace();
                        }
                        inputStream = null;
                    }
                }

            }
            if(inputStream != null){
                Log.d("talkback","input stream close");
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                inputStream = null;
            }
        }
    }
}
