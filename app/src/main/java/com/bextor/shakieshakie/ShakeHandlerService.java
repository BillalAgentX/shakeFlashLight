package com.bextor.shakieshakie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ShakeHandlerService extends Service implements SensorEventListener {

    private static final String TAG = "ShakeHandlerService";
    public static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 8466503;
    private static final String FOREGROUND_CHANNEL_ID = "SHAKE_HANDLER";

    private NotificationManager mNotificationManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    private boolean flashIsOn = false;
    private long lastCommandTime = 0;
    private static final float ACCELERATION_AMOUNT = 26.0f;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Start Foreground Service with properly constructed notification
        startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());

        // Initialize Sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mAccelerometer != null) {
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            String action = intent.getAction();
            if (ACTION.STOP_ACTION.equals(action)) {
                turnOffFlash();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: ", e);
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (se.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = se.values[0];
        float y = se.values[1];
        float z = se.values[2];

        mAccelLast = mAccelCurrent;
        mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
        float delta = mAccelCurrent - mAccelLast;
        mAccel = mAccel * 0.9f + delta; // Low-pass filter

        if (mAccel > ACCELERATION_AMOUNT) {
            long currentTime = System.currentTimeMillis();

            // Debounce shake triggers (1 second threshold)
            if (currentTime - lastCommandTime > 1000) {
                if (!flashIsOn) {
                    turnOnFlash();
                } else {
                    turnOffFlash();
                }
                lastCommandTime = currentTime;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void torchToggle(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (camManager == null) return;

            try {
                String cameraId = getCameraIdWithFlash(camManager);
                if (cameraId != null) {
                    camManager.setTorchMode(cameraId, enable);
                    flashIsOn = enable;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle torch mode", e);
            }
        }
    }

    private String getCameraIdWithFlash(CameraManager camManager) {
        try {
            for (String id : camManager.getCameraIdList()) {
                CameraCharacteristics characteristics = camManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (hasFlash != null && hasFlash && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving camera ID", e);
        }
        return null;
    }

    private void turnOnFlash() {
        torchToggle(true);
    }

    private void turnOffFlash() {
        torchToggle(false);
    }

    private Notification prepareNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mNotificationManager.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
                CharSequence name = getString(R.string.text_name_notification);
                NotificationChannel channel = new NotificationChannel(
                        FOREGROUND_CHANNEL_ID,
                        name,
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.enableVibration(false);
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        Intent stopIntent = new Intent(this, ShakeHandlerService.class);
        stopIntent.setAction(ACTION.STOP_ACTION);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, flags);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, pendingStopIntent);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        notificationBuilder
                .setCustomContentView(remoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        }

        return notificationBuilder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure torch is turned off when service stops
        turnOffFlash();

        // Unregister sensor listener to prevent memory leaks and battery drain
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    public static class ACTION {
        public static final String MAIN_ACTION = "test.action.main";
        public static final String START_ACTION = "test.action.start";
        public static final String STOP_ACTION = "test.action.stop";
    }
}