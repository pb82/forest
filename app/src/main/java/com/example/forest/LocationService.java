package com.example.forest;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.content.ContextCompat;

public class LocationService extends Service implements LocationListener {
    private IBinder binder = new LocalBinder();
    private LocationManager locationManager;

    public LocationService() {
    }

    private void broadcast(String action, String status) {
        Intent intent = new Intent(action);
        intent.putExtra(Constants.EXTRA_DATA, status);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final Location location) {
        Intent intent = new Intent(Constants.ACTION_LOCATION_DATA);
        intent.putExtra(Constants.EXTRA_DATA, location);
        sendBroadcast(intent);
    }


    public void init() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, this);
        }
    }

    // Cleanup
    private void close() {
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

    @Override
    public void onLocationChanged(Location location) {
        broadcastUpdate(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        broadcast(Constants.ACTION_LOCATION_STATUS_CHANGED, s);
    }

    @Override
    public void onProviderEnabled(String s) {
        broadcast(Constants.ACTION_LOCATION_STATUS_CHANGED, s + " enabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        broadcast(Constants.ACTION_LOCATION_STATUS_CHANGED, s + " disabled");
    }

    public class LocalBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }
}
