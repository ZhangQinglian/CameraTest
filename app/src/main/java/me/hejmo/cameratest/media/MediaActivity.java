package me.hejmo.cameratest.media;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.zqlite.android.logly.Logly;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

import me.hejmo.cameratest.R;

import me.hejmo.cameratest.media.ui.TalkbackFragment;
import me.hejmo.cameratest.media.ui.TalkbackPresenter;


public class MediaActivity extends AppCompatActivity {


    public static final Logly.Tag TAG = new Logly.Tag(Logly.FLAG_THREAD_NAME, "mediacodec", Logly.DEBUG);


    private String mRole;

    private String mIP;

    private TalkbackFragment mFragment;

    private TalkbackPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        //get role
        mRole= getIntent().getExtras().getString(Contract.ROLE);
        Logly.d(TAG, "role = " + mRole);
        if(Contract.RESPONDER.equals(mRole)){
            mIP = getIntent().getExtras().getString(Contract.IP);
        }
        mFragment = TalkbackFragment.newInstance(mRole,mIP);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.media_container,mFragment);
        fragmentTransaction.commit();
        mPresenter = new TalkbackPresenter(mFragment);
    }




}
