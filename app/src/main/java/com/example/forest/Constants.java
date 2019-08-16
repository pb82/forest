package com.example.forest;

import java.util.UUID;

public final class Constants {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int SCAN_TIMEOUT = 10000;
    public static final String TAG = "FOREST_APP";
    public static final String EXTRA_DEVICE = "forest.device";
    public static final String EXTRA_DATA = "forest.data";
    public static final String ACTION_DATA = "forest.measurement";
    public static final String ACTION_CONNECTED = "forest.connected";
    public static final String ACTION_SERVICE_DISCOVERED = "forest.serivce.discovered";
    public static final String ACTION_CHARACTERISTIC_DISCOVERED = "forest.characteristic.discovered";
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACT_HEART_RATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHACACT_ENA_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
