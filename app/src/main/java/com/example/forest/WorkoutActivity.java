package com.example.forest;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.SyncStateContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.util.Date;

public class WorkoutActivity extends Activity {
    private HeartRateService heartRateService;
    private LocationService locationService;
    private ServiceConnection connection;
    private ServiceConnection locationConnection;
    private DataCruncher cruncher = new DataCruncher();
    private LocationManager locationManager;
    private PowerManager.WakeLock wakeLock;

    // Heartrate
    private TextView min;
    private TextView max;
    private TextView cnt;
    private TextView avg;
    private TextView cur;
    private TextView dur;
    private TextView wst;

    // Location
    private TextView gst;
    private TextView spd;
    private TextView dst;
    private TextView asp;
    private TextView cpc;
    private TextView apc;
    private TextView lsp;
    private TextView alt;

    // Buttons
    private Button rec;
    private final DataRecorder recorder = new DataRecorder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // No screen rotation, don't want to deal with it
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        recorder.setBaseDir(this.getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsoluteFile());

        // Heartrate
        min = findViewById(R.id.txtWorkoutMinBpm);
        max = findViewById(R.id.txtWorkoutMaxBpm);
        cnt = findViewById(R.id.txtWorkoutSamples);
        avg = findViewById(R.id.txtWorkoutAvgBpm);
        cur = findViewById(R.id.txtWorkoutCurBpm);
        dur = findViewById(R.id.txtWorkoutDuration);
        wst = findViewById(R.id.txtWorkoutStatus);

        // Location
        gst = findViewById(R.id.txtWorkoutLocationStatus);
        spd = findViewById(R.id.txtWorkoutSpeed);
        asp = findViewById(R.id.txtWorkoutAvgSpeed);
        dst = findViewById(R.id.txtWorkoutDistance);
        cpc = findViewById(R.id.txtCurrentPace);
        apc = findViewById(R.id.txtAvgPace);
        lsp = findViewById(R.id.txtLocationSamples);
        alt = findViewById(R.id.txtAltitude);
        rec = findViewById(R.id.btnRecorder);

        wst.setText(R.string.ble_status_disconnected);


        if (setupService() == false) {
            Log.d(Constants.TAG, "no bluetooth device connected");
        }

        setupLocation();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "com.example.forest::WAKE_LOCK");
        wakeLock.acquire();

        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder.running()) {
                    recorder.stop();
                    rec.setText(R.string.recorder_start);
                } else {
                    try {
                        recorder.start();
                        rec.setText(R.string.recorder_stop);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void setupLocation() {
        locationConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                locationService = ((LocationService.LocalBinder) service).getService();
                locationService.init();
                gst.setText("Pending");
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                locationService = null;
                Log.d(Constants.TAG, "service disconnected");
                gst.setText("Disconnected");
            }
        };

        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, locationConnection, BIND_AUTO_CREATE);
        Log.d(Constants.TAG, "location service bound");

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            unbindService(connection);
        }
        if (locationConnection != null) {
            unbindService(locationConnection);
        }
        unregisterReceiver(receiver);
        wakeLock.release();
    }

    private boolean setupService() {
        Intent data = getIntent();
        final BluetoothDevice device = data.getParcelableExtra(Constants.EXTRA_DEVICE);
        if (device == null) {
            connection = null;
            return false;
        }

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                heartRateService = ((HeartRateService.LocalBinder) service).getService();
                heartRateService.init(device);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                heartRateService = null;
                Log.d(Constants.TAG, "service disconnected");
            }
        };

        Intent intent = new Intent(this, HeartRateService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        Log.d(Constants.TAG, "service bound");
        return true;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case Constants.ACTION_CONNECTED:
                    Log.d(Constants.TAG, "connected to device");
                    wst.setText(R.string.ble_status_connected);
                    break;
                case Constants.ACTION_SERVICE_DISCOVERED:
                    Log.d(Constants.TAG, "service found");
                    wst.setText(R.string.ble_status_services);
                    break;
                case Constants.ACTION_CHARACTERISTIC_DISCOVERED:
                    Log.d(Constants.TAG, "Characteristic discovered");
                    wst.setText(R.string.ble_status_characteristics);
                    break;
                case Constants.ACTION_DISCONNECTED:
                    Log.d(Constants.TAG, "ble disconnected");
                    wst.setText(R.string.ble_status_disconnected);
                    break;
                case Constants.ACTION_DATA:
                    int data = intent.getIntExtra(Constants.EXTRA_DATA, 0);
                    Log.d(Constants.TAG, String.format("data received: %d", data));
                    cruncher.recordHeartRate(data);
                    min.setText(cruncher.getMinBpm());
                    max.setText(cruncher.getMaxBpm());
                    cnt.setText(cruncher.getSamples());
                    avg.setText(cruncher.getAvgBpm());
                    cur.setText(cruncher.getCurBpm());
                    dur.setText(cruncher.getDuration());
                    try {
                        recorder.recordHeartrate(data);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.ACTION_LOCATION_STATUS_CHANGED:
                    String status = intent.getStringExtra(Constants.EXTRA_DATA);
                    gst.setText(status);
                    break;
                case Constants.ACTION_LOCATION_DATA:
                    Location location = intent.getParcelableExtra(Constants.EXTRA_DATA);
                    cruncher.recordLLocation(location);
                    spd.setText(cruncher.getSpeed());
                    asp.setText(cruncher.getAvgSpeed());
                    dst.setText(cruncher.getDistance());
                    apc.setText(cruncher.getAvgPace());
                    cpc.setText(cruncher.getPace());
                    alt.setText(cruncher.getAltitude());
                    lsp.setText(cruncher.getLocationSamples());
                    try {
                        recorder.recordLocation(location);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    gst.setText("Online");
                    break;
                default:
                    Log.d(Constants.TAG, String.format("unknown action: %s", action));
                    break;
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_DATA);
        intentFilter.addAction(Constants.ACTION_CONNECTED);
        intentFilter.addAction(Constants.ACTION_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_SERVICE_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_CHARACTERISTIC_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_LOCATION_STATUS_CHANGED);
        intentFilter.addAction(Constants.ACTION_LOCATION_DATA);
        return intentFilter;
    }
}
