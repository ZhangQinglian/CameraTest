package me.hejmo.cameratest.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.hejmo.cameratest.media.MediaActivity;
import static me.hejmo.cameratest.media.ui.TalkbackContract.*;

/**
 * Created by scott on 3/31/16.
 *
 * @author zhangqinglian
 */
public class BaseActivity extends AppCompatActivity {


    private final Object mCreateFlag = new Object();

    private boolean mShouldWaitSetContentView = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //检查是否支持相机
        if (!checkCameraHardware(this)) {
            finish();
        }

        startMedia();

    }


    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mShouldWaitSetContentView = false;
        synchronized (mCreateFlag) {
            mCreateFlag.notify();
        }
    }


    private void startMedia(){
        Intent preIntent = getIntent();
        String role = preIntent.getExtras().getString(ROLE);
        Intent intent = new Intent(BaseActivity.this, MediaActivity.class);
        if(role.equals(RESPONDER)){
            intent.putExtra(IP,preIntent.getExtras().getString(IP));
        }
        intent.putExtra(ROLE,role);
        startActivity(intent);
        finish();
    }
}
