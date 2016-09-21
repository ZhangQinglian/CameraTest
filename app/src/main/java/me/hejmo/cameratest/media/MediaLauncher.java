package me.hejmo.cameratest.media;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
        TextView textIp = (TextView) findViewById(R.id.text_ip);
        final EditText editIp = (EditText) findViewById(R.id.edit_ip);
        assert initiator != null;
        assert responder != null;
        initiator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaLauncher.this, BaseActivity.class);
                intent.putExtra(Contract.ROLE,Contract.INITIATOR);
                startActivity(intent);
            }
        });

        responder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = editIp.getEditableText().toString();
                if(ip == null || ip.trim().length() == 0){
                    return ;
                }
                Intent intent = new Intent(MediaLauncher.this, BaseActivity.class);
                intent.putExtra(Contract.ROLE,Contract.RESPONDER);
                intent.putExtra(Contract.IP,ip);
                startActivity(intent);
            }
        });
        textIp.setText(getIp());
        editIp.setText(getIp().substring(0,getIp().lastIndexOf(".") + 1));
    }

    private String getIp() {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wm.getConnectionInfo();
        return intToIp(info.getIpAddress());
    }

    public String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }
}
