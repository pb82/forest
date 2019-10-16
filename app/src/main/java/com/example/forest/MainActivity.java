package com.example.forest;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.forest.Constants.REQUEST_ENABLE_BT;
import static com.example.forest.Constants.SCAN_TIMEOUT;


public class MainActivity extends Activity {
    private BluetoothAdapter bluetoothAdapter;
    private TextView scanStatusText;
    private RecyclerView deviceList;
    private RecyclerView.LayoutManager deviceListLayout;
    private DeviceListAdapter deviceListAdapter;
    private Handler handler = new Handler();
    private Button skipBluetooth;

    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceListAdapter.addDevice(bluetoothDevice);
                    deviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // No screen rotation, don't want to deal with it
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        scanStatusText = findViewById(R.id.scan_status_text);
        skipBluetooth = findViewById(R.id.btnSkipBluetooth);

        // We need bluetooth low energy support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        } else {
            setupBluetooth();
        }

        // We need gps location support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            Toast.makeText(this, R.string.gps_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        setupList();

        // Alllow users to skip scanning for bluetooth devices and only record
        // location
        skipBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Constants.TAG, "skip bluetooth scan");
                Intent intent = new Intent(MainActivity.this, WorkoutActivity.class);
                startActivity(intent);
            }
        });
    }

    // Setup the list of bluetooth devices
    private void setupList() {
        deviceList = findViewById(R.id.device_list);
        deviceList.setHasFixedSize(true);

        deviceListLayout = new LinearLayoutManager(this);
        deviceList.setLayoutManager(deviceListLayout);

        deviceListAdapter = new DeviceListAdapter(new DeviceListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BluetoothDevice device) {
                Log.d(Constants.TAG, String.format("clicked on %s", device.getAddress()));
                Intent intent = new Intent(MainActivity.this, WorkoutActivity.class);
                intent.putExtra(Constants.EXTRA_DEVICE, device);
                startActivity(intent);
            }
        });
        deviceList.setAdapter(deviceListAdapter);
    }

    // Make sure we can obtain a bluetooth adapter
    // Terminate otherwise
    private void setupBluetooth() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter == null) {
            scanStatusText.setText(R.string.scan_status_disabled);
            return;
        }

        if (bluetoothAdapter.isEnabled() == false) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scan();
        }
    }

    // Scan for Bluetooth LE devices
    // The callback will take care of adding them to the list of devices
    private void scan() {
        scanStatusText.setText(R.string.scan_status_scanning);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(scanCallback);
                scanStatusText.setText(R.string.scan_status_done);
            }
        }, SCAN_TIMEOUT);

        bluetoothAdapter.startLeScan(scanCallback);
    }
}
