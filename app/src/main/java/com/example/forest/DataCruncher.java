package com.example.forest;

import android.location.Location;

public class DataCruncher {
    private static final double EARTH_RADIUS_KM = 6371;

    private float data_average = 0;
    private int data_latest = 0;
    private int data_count = 0;
    private int data_min = 999;
    private int data_max = 0;
    private long timestamp = System.currentTimeMillis();
    private long seconds;
    private long hours;
    private long minutes;
    private Location lastPosition = null;
    private double distance_total = 0;
    private double current_speed = 0;
    private double avg_speed = 0;
    private int location_samples = 0;
    private double current_pace = 0;
    private double avg_pace = 0;
    private double current_altitude = 0;

    public DataCruncher() {

    }

    private double degToRad(double degrees) {
        return degrees * Math.PI / 180;
    }

    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = degToRad(lat2 - lat1);
        double dLon = degToRad(lon2 - lon1);
        lat1 = degToRad(lat1);
        lat2 = degToRad(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private String paceToTime(double pace) {
        double minutes = Math.floor(pace);
        double seconds = (pace * 60) % 60;
        return String.format("%.0fm %.0fs", minutes, seconds);
    }

    public String getMaxBpm() {
        return String.format("%d", data_max);
    }

    public String getMinBpm() {
        return String.format("%d", data_min);
    }

    public String getAvgBpm() {
        return String.format("%.0f", data_average);
    }

    public String getCurBpm() {
        return String.format("%d", data_latest);
    }

    public String getSamples() {
        return String.format("%d", data_count);
    }

    public String getLocationSamples() {
        return String.format("%d", location_samples);
    }


    public String getDuration() {
        long s = seconds % 60;
        long m = minutes % 60;
        return String.format("%dh %dm %ds", hours, m, s);
    }

    public String getSpeed() {
        return String.format("%.2f km/h", current_speed);
    }

    public String getAvgSpeed() {
        return String.format("%.2f km/h", avg_speed);
    }

    public String getDistance() {
        return String.format("%.2f km", distance_total);
    }

    public String getAltitude() {
        return String.format("%.0f m", current_altitude);
    }

    public String getPace() {
        return paceToTime(current_pace);
    }

    public String getAvgPace() {
        return paceToTime(avg_pace);
    }

    public void recordLLocation(Location location) {
        if (lastPosition != null) {
            distance_total += distanceInKm(
                    lastPosition.getLatitude(),
                    lastPosition.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude());
        }

        location_samples++;
        current_altitude = location.getAltitude();
        current_speed = location.getSpeed() * 3.6;
        avg_speed = (((location_samples - 1) * avg_speed) + current_speed) / location_samples;

        if (current_speed > 0) {
            current_pace = 60 / current_speed;
        }

        if (avg_speed > 0) {
            avg_pace = 60 / avg_speed;
        }
        lastPosition = location;
    }

    public void recordHeartRate(int measurement) {
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
