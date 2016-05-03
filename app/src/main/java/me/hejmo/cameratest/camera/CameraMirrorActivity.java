package me.hejmo.cameratest.camera;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import me.hejmo.cameratest.R;

public class CameraMirrorActivity extends AppCompatActivity {


    private CameraCover mCameraCover;
    private CameraMirrorPreview mCameraMirroPreivew;
    private RelativeLayout mCameraActionArea ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_mirror);
        initView();

        Executors.newSingleThreadExecutor().execute(new ReceiverCameraFrameTask());
    }

    private void initView() {
        mCameraCover = (CameraCover) findViewById(R.id.camera_cover);
        mCameraMirroPreivew = (CameraMirrorPreview) findViewById(R.id.camera_mirror_preview);
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCameraCover.startShutter();
                    }
                }
        );
        mCameraActionArea = (RelativeLayout) findViewById(R.id.camera_action_area);
        modifyActionArea(mCameraActionArea);
    }

    private class ReceiverCameraFrameTask implements Runnable {

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(12111);
                while (true) {
                    Log.d("camera_test", "serverSocket before");
                    Socket s = serverSocket.accept();
                    showCoverView();
                    Log.d("camera_test", "serverSocket after");
                    CameraMirrorDrawerTask drawerTask = new CameraMirrorDrawerTask();
                    Executors.newSingleThreadExecutor().execute(drawerTask);
                    CameraReceiver cameraReceiver = new CameraReceiver(s, drawerTask);
                    cameraReceiver.run();
                }
            } catch (IOException e1) {
                Log.d(CameraPreview.TAG, e1.getMessage());
            }
        }
    }

    private class CameraMirrorDrawerTask implements Runnable, CameraReceiver.OnCameraFrameCallback {

        private BlockingDeque<CameraFrame> mInCommingFrames;

        private Matrix m = new Matrix();
        // 设置旋转角度
        private Paint paint = new Paint();

        public CameraMirrorDrawerTask() {
            mInCommingFrames = new LinkedBlockingDeque<>(3);
            m.postRotate(90);
        }

        @Override
        public void run() {
            while (true) {
                try {

                    CameraFrame frame = mInCommingFrames.take();
                    if (mCameraMirroPreivew.isSurfaceEnable()) {
                        Canvas canvas = mCameraMirroPreivew.getSurfaceHolder().lockCanvas();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(frame.getData(), 0, frame.getData().length);
                        if(bitmap != null){
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),m, true);
                            bitmap = Bitmap.createScaledBitmap(bitmap,mCameraMirroPreivew.getWidth(),mCameraMirroPreivew.getHeight(),true);
                            canvas.drawBitmap(bitmap, 0, 0, paint);
                            bitmap.recycle();
                        }


                        mCameraMirroPreivew.getSurfaceHolder().unlockCanvasAndPost(canvas);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void newCameraFrame(CameraFrame frame) {
            try {
                mInCommingFrames.add(frame);

            } catch (IllegalStateException e) {
                Log.d(CameraPreview.TAG, "mInCommingFrames is full ,clear");
                mInCommingFrames.clear();
            }
        }
    }


    private void modifyActionArea(RelativeLayout actionArea){
        FrameLayout.LayoutParams LLP = (FrameLayout.LayoutParams) actionArea.getLayoutParams();
        if(hasNavbar()){
            LLP.setMargins(0,0,0,getNavbarH());
            actionArea.setLayoutParams(LLP);
        }

    }

    private void showCoverView(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCameraCover.setVisibility(View.INVISIBLE);
            }
        });
    }
    private boolean hasNavbar(){
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
        boolean hasHardwareButtons = hasBackKey && hasHomeKey;
        return !hasHardwareButtons;
    }

    private int getNavbarH(){
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

}
