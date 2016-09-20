package me.hejmo.cameratest.media;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import me.hejmo.cameratest.R;
import me.hejmo.cameratest.artphelper.ARTPHelper;
import me.hejmo.cameratest.camera.BaseActivity;

public class MediaLauncher extends AppCompatActivity {

    final ARTPHelper artpHelper = new ARTPHelper(true);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_launcher);

        Button initiator = (Button) findViewById(R.id.btn_initiator);
        Button responder = (Button) findViewById(R.id.btn_responder);
        assert initiator != null;
        assert responder != null;
        initiator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaLauncher.this, BaseActivity.class);
                intent.putExtra("role","initiator");
                startActivity(intent);
            }
        });

        responder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaLauncher.this, BaseActivity.class);
                intent.putExtra("role","responder");
                startActivity(intent);
            }
        });
    }
}
