package com.example.forest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataRecorder {
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private File baseDir;
    private JSONObject workout;
    private JSONArray heartrate;
    private JSONArray location;
    private boolean started = false;

    public DataRecorder() {
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public void start() throws JSONException {
        this.started = true;
        workout = new JSONObject();
        heartrate = new JSONArray();
        location = new JSONArray();

        workout.put("timestamp", new Date().getTime() / 1000L);
        workout.put("location", location);
        workout.put("heartrate", heartrate);
    }

    public void stop() {
        this.started = false;
        String fileName = "workout_" + format.format(new Date()) + ".json";

        try {
            File file = new File(baseDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(workout.toString());
            writer.flush();
            writer.close();
            workout = null;
            heartrate = null;
            location = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void recordHeartrate(int bpm) throws JSONException {
        if (!this.started) {
            return;
        }

        JSONObject obj = new JSONObject();
        obj.put("timestamp", new Date().getTime() / 1000L);
        obj.put("bpm", bpm);
        heartrate.put(obj);
    }

    public boolean running() {
        return this.started;
    }

    public void recordLocation(double lat, double lon, double speed) throws JSONException {
        if (!this.started) {
            return;
        }

        JSONObject obj = new JSONObject();
        obj.put("timestamp", new Date().getTime() / 1000L);
        obj.put("lat", lat);
        obj.put("lon", lon);
        obj.put("speed", speed);
        location.put(obj);
    }
}
