package me.hejmo.cameratest.media;

import android.os.Bundle;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.zqlite.android.logly.Logly;

import me.hejmo.cameratest.R;

import me.hejmo.cameratest.media.ui.TalkbackFragment;
import me.hejmo.cameratest.media.ui.TalkbackPresenter;

import static me.hejmo.cameratest.media.ui.TalkbackContract.*;

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
        mRole= getIntent().getExtras().getString(ROLE);
        Logly.d(TAG, "role = " + mRole);
        if(RESPONDER.equals(mRole)){
            mIP = getIntent().getExtras().getString(IP);
        }
        mFragment = TalkbackFragment.newInstance(mRole,mIP);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.media_container,mFragment);
        fragmentTransaction.commit();
        mPresenter = new TalkbackPresenter(mFragment);
    }

}
