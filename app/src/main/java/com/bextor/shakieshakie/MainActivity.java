package com.bextor.shakieshakie;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.angads25.toggle.model.ToggleableView;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 800;
    private long lastUpdate;
    private float x, y, z, last_x, last_y, last_z;
    SensorManager sensorMgr;
    private boolean isTorchOn;
    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity
    private boolean flashIsOn;
    private long lastCommandTime;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.POST_NOTIFICATIONS};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        AdRequest adRequest = new AdRequest.Builder().build();


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

//        startService(new Intent(MainActivity.this,ShakeHandlerService.class));
        setContentView(R.layout.activity_main);

//        startService(new Intent(MainActivity.this,ShakeHandlerService.class));
        LabeledSwitch s = findViewById(R.id.switchs);

        s.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(ToggleableView toggleableView, boolean isOn) {
                if (isOn) {
                    startService();
                    //torchToggle("on");
                } else
                    endService();
                ;
            }
        });

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);


    }




    private void startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(new Intent(MainActivity.this, ShakeHandlerService.class));
        else
            startService(new Intent(MainActivity.this, ShakeHandlerService.class));

    }

    private void endService() {
        stopService(new Intent(MainActivity.this, ShakeHandlerService.class));
    }

    @Override
    public void onSensorChanged(SensorEvent se) {


    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    private void showInterstitial() {

    }


}