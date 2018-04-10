package com.example.thinkpad.sensordatarecord;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG = "MainActivity";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Toast.makeText(this, "Recording Sensor Data Now...", Toast.LENGTH_SHORT).show();
        File sensorDataDir = new File("/sdcard/sensor_data_recording");
        if (!sensorDataDir.exists()) {
            boolean firstCreate = sensorDataDir.mkdirs();
            Log.d(TAG, "onCreate: mkdirs: /sdcard/sensor_data_recoding");
        }
        fileName = createDataFile();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);//SENSOR_DELAY_GAME:0.02s//SENSOR_DELAY_NORMAL:200000microsecond = 0.2s
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        String dataString = x+":"+y+":"+z;
        appendMethodB(fileName, dataString);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static void appendMethodB(String fileName, String content) {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createDataFile() {
        String fileName = "/sdcard/sensor_data_recording/" + System.currentTimeMillis();
        File sensorDataFile = new File(fileName);
        if (!sensorDataFile.exists()) {
            Log.d(TAG, "onCreate: dataFile not be created???!!!!");
        }
        return fileName;
    }
}
