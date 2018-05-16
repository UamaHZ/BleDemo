package com.example.ble.bledemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.clj.fastble.BleManager;

public class MainActivity extends AppCompatActivity implements RefreshListener{

    BleDoorManager bleDoorManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bleDoorManager=new BleDoorManager(this);
        BleDoorManager.init(getApplication());
        findViewById(R.id.tx_click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBlePermission();
            }
        });
    }
    /**
     * ACCESS_COARSE_LOCATION 请求码
     */
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100;
    /**
     * 权限判断
     */
    private void checkBlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            } else {
                //具有权限
                bleDoorManager.startScanAndConnect();
            }
        } else {
            //系统不高于6.0直接执行
            bleDoorManager.startScanAndConnect();
        }
    }

    /**
     * 对返回的值进行处理，相当于StartActivityForResult
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    /**
     * 权限处理的回调
     */

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //同意权限
                bleDoorManager.startScanAndConnect();
            } else {
                // 权限拒绝，提示用户开启权限
                Toast.makeText(MainActivity.this,"未获取到相应权限",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean startConnect() {
        return false;
    }

    @Override
    public void completeOneCycleProcess(String receiveNotify) {
        BleManager.getInstance().disconnectAllDevice();
        if ("0".equalsIgnoreCase(receiveNotify)) {
            Toast.makeText(this,"开门成功",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this,"开门失败",Toast.LENGTH_LONG).show();
        }
    }
}
