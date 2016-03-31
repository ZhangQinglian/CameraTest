package me.hejmo.cameratest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * Created by scott on 3/31/16.
 * @author zhangqinglian
 */
public class QuickActivity extends AppCompatActivity {


    private final Object mCreateFlag = new Object();

    private boolean mShouldWaitSetContentView = true ;

    private class OnCreateTaskThread extends Thread{
        @Override
        public void run() {
            Log.d(CameraPreview.TAG, "OnCreateTaskThread running");
            onCreateTaskAsync();
            if(mShouldWaitSetContentView){
                try {
                    synchronized (mCreateFlag){
                        Log.d(CameraPreview.TAG,"OnCreateTaskThread wait");
                        mCreateFlag.wait();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onCreateTaskAsyncFinish();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                Log.d(CameraPreview.TAG,"OnCreateTaskThread not wait");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onCreateTaskAsyncFinish();
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CameraPreview.TAG," quick oncreate");
        OnCreateTaskThread t = new OnCreateTaskThread();
        t.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mShouldWaitSetContentView = false ;
        synchronized (mCreateFlag){
            mCreateFlag.notify();
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }

    protected void onCreateTaskAsync(){
    }

    protected void onCreateTaskAsyncFinish(){

    }
}
