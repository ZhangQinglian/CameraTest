package me.hejmo.cameratest.camera;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import me.hejmo.cameratest.R;

public class Launcher extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        Button monitor = (Button) findViewById(R.id.monitor);
        Button camera = (Button) findViewById(R.id.camera);
        TextView ip = (TextView) findViewById(R.id.ip);
        final EditText ip_last = (EditText) findViewById(R.id.ip_last);
        monitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Launcher.this, CameraMirrorActivity.class);
                startActivity(intent);
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ipLast = ip_last.getEditableText().toString();
                if(ipLast == null || ipLast.trim().length()==0){
                    Toast.makeText(Launcher.this,"请输入对让ip的第四个字段",Toast.LENGTH_LONG).show();
                    ip_last.requestFocus();
                }else {
                    String myIp = getMyIp();
                    String otherIp = myIp.substring(0,myIp.lastIndexOf(".")) + "." + ipLast ;
                    Toast.makeText(Launcher.this,"对方ip为" + otherIp,Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Launcher.this, CameraActivity.class);
                    intent.putExtra("ip",otherIp);
                    startActivity(intent);
                }
            }
        });

        ip.setText("my ip: " + getMyIp());

    }

    private String getMyIp(){
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return intToIp(ipAddress);
    }
    private String intToIp(int i) {

        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

}
