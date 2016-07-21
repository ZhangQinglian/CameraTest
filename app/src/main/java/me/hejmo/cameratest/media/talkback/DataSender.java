package me.hejmo.cameratest.media.talkback;

import android.util.Log;
import android.util.TimeUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author qinglian.zhang
 */
public class DataSender {

    interface SenderCallback{
        byte[] getData();
    }

    private OutputStream mOutputStream;

    private SenderWorker mWorker;

    private SenderCallback mCallback;
    public DataSender(OutputStream outputStream,SenderCallback callback){

        mOutputStream = outputStream;
        mCallback = callback;

    }

    public void start(){
        if(mWorker == null){
            mWorker = new SenderWorker(mOutputStream);
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

    class SenderWorker extends Thread{

        private boolean running = false ;

        private OutputStream outputStream;

        public SenderWorker(OutputStream os){
            this.outputStream = os;
        }

        public void setRunning(boolean runnung){
            this.running = runnung;
        }
        @Override
        public void run() {
            while(running){
                try {
                    if(outputStream != null){
                        if(mCallback != null){
                            byte[] data = mCallback.getData();
                            Log.d("talkback","total size = " + data.length + "   type + " + data[0]);

                            outputStream.write(data);
                            outputStream.flush();
                        }
                    }
                } catch (IOException e) {

                    if(outputStream != null){
                        Log.d("talkback","output stream close");
                        try {
                            outputStream.close();
                        } catch (IOException ee) {
                            ee.printStackTrace();
                        }
                        outputStream = null;
                    }
                }
            }
            if(outputStream != null){
                Log.d("talkback","output stream close");
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputStream = null;
            }

        }
    }

}
