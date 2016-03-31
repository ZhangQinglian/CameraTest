package me.hejmo.cameratest;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import me.hejmo.cameratest.artphelper.ARTPHelper;

public class CameraLauncher extends AppCompatActivity {

    private ARTPHelper mARTPHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mARTPHelper = new ARTPHelper(false);
        mARTPHelper.writeExternalStorage().useCamera().accessFineLocation();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            mARTPHelper.requestPermissions(this);
        }else{
            Intent intent = new Intent(CameraLauncher.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mARTPHelper.onRequestPermissionsResult(requestCode, permissions, grantResults, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraLauncher.this, "权限不足", Toast.LENGTH_SHORT).show();
            }
        }, new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(CameraLauncher.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
