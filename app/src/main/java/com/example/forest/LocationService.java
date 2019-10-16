package com.example.forest;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class LocationService extends Service implements LocationListener {
    private IBinder binder = new LocalBinder();
    private LocationManager locationManager;
    private static int NOTIFICATION_ID = 2349832;

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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5, this);
        }
    }

    // Cleanup
    private void close() {
        locationManager.removeUpdates(this);
        locationManager = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "forest::location";
        String channelName = "Forest Location Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
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
