package com.bextor.shakieshakie;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bextor.shakieshakie.databinding.ActivityMainBinding;

import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    public static final String TAG = "HomeActivity";
    public static final String id1 = "test_channel_01";

    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.POST_NOTIFICATIONS};

    private ActivityMainBinding binding;
    private ActivityResultLauncher<String[]> rpl;

    private ShakeHandlerService mService;
    private boolean mBound = false;

    /**
     * Receives the binder from the service and registers a listener so the
     * activity can update its UI based on service events.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ShakeHandlerService.LocalBinder binder = (ShakeHandlerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            logthis("Service connected");

            // Sync UI with the current service state.


            // Register a listener to react to shake events / flash changes.
            mService.setListener(new ShakeHandlerService.ServiceListener() {
                @Override
                public void onFlashToggled(boolean isOn) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isOn)
                            binding.flIconContainer.setBackground(getResources().getDrawable(R.drawable.flash_light_on));
                            else
                                binding.flIconContainer.setBackground(getResources().getDrawable(R.drawable.flash_light_off));

                        }
                    });
                }

                @Override
                public void onShakeCountChanged(int count) {
                    runOnUiThread(() -> logthis("Shake count: " + count));
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mService = null;
            logthis("Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // For notifications permission now required in API 33+.
        rpl = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        boolean granted = true;
                        for (Map.Entry<String, Boolean> x : isGranted.entrySet()) {
                            logthis(x.getKey() + " is " + x.getValue());
                            if (!x.getValue()) granted = false;
                        }
                        if (granted) logthis("Permissions granted for API 33+");
                    }
                });

        createChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!allPermissionsGranted()) {
                rpl.launch(REQUIRED_PERMISSIONS);
            }
        }

    /*    // Toggle switch now starts/stops the service.
        binding.switchs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startShakeService();
                } else {
                    stopShakeService();
                }
            }
        });*/
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service if it is already running.
        Intent intent = new Intent(this, ShakeHandlerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            if (mService != null) {
                mService.setListener(null);
            }
            unbindService(mConnection);
            mBound = false;
        }
    }

    /** Start the shake handler as a foreground service. */
    private void startShakeService() {
        Intent intent = new Intent(this, ShakeHandlerService.class);
        intent.putExtra("times", 5);
        ContextCompat.startForegroundService(this, intent);
    }

    /** Stop the shake handler service. */
    private void stopShakeService() {
        Intent intent = new Intent(this, ShakeHandlerService.class);
        intent.setAction(ShakeHandlerService.ACTION.STOP_ACTION);
        startService(intent);
    }

    /** For API 26+ create notification channels. */
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel(
                    id1,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            mChannel.setDescription(getString(R.string.channel_description));
            mChannel.enableLights(true);
            mChannel.setShowBadge(true);
            nm.createNotificationChannel(mChannel);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void logthis(String msg) {
        Log.d(TAG, msg);
    }
}