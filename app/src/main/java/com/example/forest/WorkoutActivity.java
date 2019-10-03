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

public class WorkoutActivity extends Activity implements LocationListener {
    private HeartRateService heartRateService;
    private ServiceConnection connection;
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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
            gst.setText(R.string.gps_status_pending);
        } else {
            gst.setText(R.string.gps_status_perms);
        }
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
                    Log.d(Constants.TAG, "characteristic found");
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
        return intentFilter;
    }

    @Override
    public void onLocationChanged(Location location) {
        gst.setText(R.string.gps_status_online);
        cruncher.recordLLocation(location);
        spd.setText(cruncher.getSpeed());
        asp.setText(cruncher.getAvgSpeed());
        dst.setText(cruncher.getDistance());
        apc.setText(cruncher.getAvgPace());
        cpc.setText(cruncher.getPace());
        lsp.setText(cruncher.getLocationSamples());
        try {
            recorder.recordLocation(location.getLatitude(), location.getLongitude(), location.getSpeed());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        gst.setText(s);
    }

    @Override
    public void onProviderEnabled(String s) {
        gst.setText(R.string.gps_status_enabled);
    }

    @Override
    public void onProviderDisabled(String s) {
        gst.setText(R.string.gps_status_disabled);
    }
}
