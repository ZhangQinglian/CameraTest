package me.hejmo.cameratest.camera;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.R;
import me.hejmo.cameratest.ThumbManager;

/**
 * 同玩相机被控制方，真实拍照
 */
public class CameraActivity extends BaseActivity implements CameraHolder.CameraCallback{

    private CameraHolder mCameraHolder;
    private Handler mHandler;
    private RelativeLayout mCameraActionArea ;
    private CircleImageView mPictureThumbnail;

    private String ip ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CameraPreview.TAG, " onCreate ");

        mCameraHolder = CameraApplication.getInstance().getCameraHolder();
        mCameraHolder.setCameraCallback(this);
        mHandler = new Handler(getMainLooper());
        setContentView(R.layout.activity_camera);
        initView();

    }


    private void initView() {
        Button captureButton = (Button) findViewById(R.id.button_capture);
        assert captureButton != null;
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCameraHolder.tryToTakePhoto(false);
                    }
                }
        );
        mCameraActionArea = (RelativeLayout) findViewById(R.id.camera_action_area);
        modifyActionArea(mCameraActionArea);

        //初始化缩略图UI
        mPictureThumbnail = (CircleImageView) findViewById(R.id.camera_picture_thumbnail);
        String latestPhotoPath = CameraUtils.getLatestPhotoPath(this);
        if(latestPhotoPath != null){
            Log.d("test"," latest path : " + latestPhotoPath);
            mPictureThumbnail.setImageBitmap(ThumbManager.INSTANCE.getPhotoThumb(latestPhotoPath,getCacheDir().getAbsolutePath()));
        }
        mPictureThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = CameraUtils.getLatestPhotoUri(CameraActivity.this);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(uri);
                startActivity(intent);
            }
        });


    }

    @Override
    public void onCameraOpened() {

    }

    @Override
    public void onCameraPreviewed() {
        Log.d(CameraPreview.TAG, "   onCameraPreviewed");
    }

    @Override
    public void onCameraReleased() {
        Log.d(CameraPreview.TAG, "   onCameraReleased");
    }

    @Override
    public void onPictureTaken(final String picturePath) {
       final Bitmap thumbnail = ThumbManager.INSTANCE.getPhotoThumb(picturePath,this.getCacheDir().getAbsolutePath());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPictureThumbnail.setImageBitmap(thumbnail);
            }
        });
    }

    @Override
    public void onBackPressed() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 300);
    }

    private void modifyActionArea(RelativeLayout actionArea){
        FrameLayout.LayoutParams LLP = (FrameLayout.LayoutParams) actionArea.getLayoutParams();
        if(hasNavbar()){
            LLP.setMargins(0,0,0,getNavbarH());
            actionArea.setLayoutParams(LLP);
        }

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
