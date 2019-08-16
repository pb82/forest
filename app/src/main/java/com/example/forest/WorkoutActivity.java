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
    private TextView min;
    private TextView max;
    private TextView cnt;
    private TextView avg;
    private TextView cur;
    private TextView dur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        // No screen rotation, don't want to deal with it
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        min = findViewById(R.id.text_heart_rate_min);
        max = findViewById(R.id.text_heart_rate_max);
        cnt = findViewById(R.id.text_heart_rate_cnt);
        avg = findViewById(R.id.text_heart_rate_avg);
        cur = findViewById(R.id.text_heart_rate_cur);
        dur = findViewById(R.id.text_heart_rate_dur);

        if (setupService() == false) {
            Log.e(Constants.TAG, "error setting up service connection");
            finish();
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
                    break;
                case Constants.ACTION_SERVICE_DISCOVERED:
                    Log.d(Constants.TAG, "service found");
                    break;
                case Constants.ACTION_CHARACTERISTIC_DISCOVERED:
                    Log.d(Constants.TAG, "characteristic found");
                    break;
                case Constants.ACTION_DATA:
                    int data = intent.getIntExtra(Constants.EXTRA_DATA, 0);
                    Log.d(Constants.TAG, String.format("data received: %d", data));
                    add(data);
                    min.setText(String.format("%d min bpm", data_min));
                    max.setText(String.format("%d max bpm", data_max));
                    cnt.setText(String.format("%d samples", data_count));
                    avg.setText(String.format("%d avg bpm", (int) data_average));
                    cur.setText(String.format("%d cur bpm", data_latest));
                    dur.setText(String.format("%d h %d m %d s duration", hours, minutes, seconds));
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

    public float data_average = 0;
    public int data_latest = 0;
    public int data_count = 0;
    public int data_min = 999;
    public int data_max = 0;
    public long timestamp = System.currentTimeMillis();
    public long seconds;
    public long hours;
    public long minutes;

    public void add(int measurement) {
        data_latest = measurement;

        if (measurement < data_min) {
            data_min = measurement;
        }

        if (measurement > data_max) {
            data_max = measurement;
        }

        data_count++;
        data_average = (((data_count - 1) * data_average) + measurement) / data_count;

        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        seconds = diff / 1000;
        minutes = seconds / 60;
        hours = minutes / 60;
    }
}
