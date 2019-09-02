package com.example.forest;

public class DataCruncher {
    private float data_average = 0;
    private int data_latest = 0;
    private int data_count = 0;
    private int data_min = 999;
    private int data_max = 0;
    private long timestamp = System.currentTimeMillis();
    private long seconds;
    private long hours;
    private long minutes;

    public DataCruncher() {

    }

    public String getMaxBpm() {
        return String.format("%d", data_max);
    }

    public String getMinBpm() {
        return String.format("%d", data_min);
    }

    public String getAvgBpm() {
        return String.format("%d", data_average);
    }

    public String getCurBpm() {
        return String.format("%d", data_latest);
    }

    public String getSamples() {
        return String.format("%d", data_count);
    }

    public String getDuration() {
        long s = seconds % 60;
        long m = minutes % 60;
        return String.format("%d h %d m %d s duration", hours, m, s);
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
