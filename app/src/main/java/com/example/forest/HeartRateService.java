package com.example.forest;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class HeartRateService extends Service {
    private IBinder binder = new LocalBinder();
    private BluetoothGattCallback callback;
    private BluetoothDevice device;
    private BluetoothGatt gatt;

    public HeartRateService() {
        callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    broadcast(Constants.ACTION_CONNECTED);
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(Constants.UUID_SERVICE_HEART_RATE);
                    if (service != null) {
                        broadcast(Constants.ACTION_SERVICE_DISCOVERED);
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(Constants.UUID_CHARACT_HEART_RATE);
                        if (characteristic != null) {
                            broadcast(Constants.ACTION_CHARACTERISTIC_DISCOVERED);
                            setCharacteristicNotification(gatt, characteristic, true);
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                broadcastUpdate(characteristic);
            }
        };
    }

    private void broadcast(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        int flag = characteristic.getProperties();
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
        }
        final int heartRate = characteristic.getIntValue(format, 1);

        Intent intent = new Intent(Constants.ACTION_DATA);
        intent.putExtra(Constants.EXTRA_DATA, heartRate);
        sendBroadcast(intent);
    }

    private boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic,boolean enable) {
        bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Constants.UUID_CHACACT_ENA_NOTIFY);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[]{0x00, 0x00});
        return bluetoothGatt.writeDescriptor(descriptor);
    }

    public void init(BluetoothDevice device) {
        this.device = device;
        gatt = device.connectGatt(this, false, callback);
    }

    // Cleanup
    private void close() {
        if (gatt != null) {
            gatt.disconnect();
            gatt = null;
            Log.d(Constants.TAG, "disconnected from device");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        HeartRateService getService() {
            return HeartRateService.this;
        }
    }
}
