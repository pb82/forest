package com.example.forest;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import java.util.Date;

public class WorkoutActivity extends Activity {
    private HeartRateService heartRateService;
    private ServiceConnection connection;
    private DataCruncher cruncher = new DataCruncher();
    private TextView min;
    private TextView max;
    private TextView cnt;
    private TextView avg;
    private TextView cur;
    private TextView dur;
    private TextView wst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // No screen rotation, don't want to deal with it
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        min = findViewById(R.id.txtWorkoutMinBpm);
        max = findViewById(R.id.txtWorkoutMaxBpm);
        cnt = findViewById(R.id.txtWorkoutSamples);
        avg = findViewById(R.id.txtWorkoutAvgBpm);
        cur = findViewById(R.id.txtWorkoutCurBpm);
        dur = findViewById(R.id.txtWorkoutDuration);
        wst = findViewById(R.id.txtWorkoutStatus);

        wst.setText(R.string.ble_status_disconnected);

        if (setupService() == false) {
            Log.d(Constants.TAG, "no bluetooth device connected");
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
        unbindService(connection);
        unregisterReceiver(receiver);
    }

    private boolean setupService() {
        Intent data = getIntent();
        final BluetoothDevice device = data.getParcelableExtra(Constants.EXTRA_DEVICE);
        if (device == null) {
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
        return intentFilter;
    }
}
