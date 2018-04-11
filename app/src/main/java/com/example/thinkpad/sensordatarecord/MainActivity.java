package com.example.thinkpad.sensordatarecord;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.thinkpad.sensordatarecord.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final String TAG1 = "OhMySensor";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private String fileName;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,};
    private AlertDialog dialog;

    public static void appendMethodB(String fileName, String content) {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Button button = new Button("Start Recording", "Stop Recording");
        binding.setButtonText(button);
        binding.setHandlers(new MyHandlers());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            if (i != PackageManager.PERMISSION_GRANTED) {
                showDialogTipUserRequestPermission();
            }
        }
        File sensorDataDir = new File("/sdcard/sensor_data_recording");
        if (!sensorDataDir.exists()) {
            boolean firstCreate = sensorDataDir.mkdirs();
            Log.d(TAG, "onCreate: mkdirs: /sdcard/sensor_data_recording");
        }
        fileName = createDataFile();
    }

    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(this)
                .setTitle("Need Storage Permission")
                .setMessage("我需要存储权限")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    private void startRequestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 321);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    boolean b = shouldShowRequestPermissionRationale(permissions[0]);
                    if (!b) {
                        showDialogTipUserGoToAppSetting();
                    } else {
                        finish();
                    }
                } else {
                    Toast.makeText(this, "获取权限成功", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void showDialogTipUserGoToAppSetting() {
        dialog = new AlertDialog.Builder(this)
                .setTitle("Storage not Accessible")
                .setMessage("请在-应用设置-权限-中，允许SensorDataRecord使用存储权限来保存用户数据")
                .setPositiveButton("这就去开权限", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int i = ContextCompat.checkSelfPermission(this, permissions[0]);
                if (i != PackageManager.PERMISSION_GRANTED) {
                    //tt: permission not granted
                    showDialogTipUserRequestPermission();
                } else {
                    //tt: permission granted
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 123);
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
        String dataString = x + "," + y + "," + z + "\r\n";
        Log.d(TAG1, "onSensorChanged: x = " + x);
        Log.d(TAG1, "onSensorChanged: y = " + y);
        Log.d(TAG1, "onSensorChanged: z = " + z);

        appendMethodB(fileName, dataString);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private String createDataFile() {
        String fileName = "/sdcard/sensor_data_recording/" + getFormatDate("createFile") + ".csv";
        File sensorDataFile = new File(fileName);
        if (!sensorDataFile.exists()) {
            Log.d(TAG, "onCreate: dataFile not be created???!!!!");
            try {
                sensorDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "createDataFile: ", e);
            }
        }
        return fileName;
    }

    public String getFormatDate(String fromWhere) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-sss");
        String dateString = formatter.format(date);
        if (fromWhere.equals("createFile")) {
            return "sensorData" + dateString;
        }
        return null;
    }

    public void startRecordingSensor() {
        Log.d(TAG, "onCreate: startRecording " + System.currentTimeMillis());
        File sensorDataDir = new File("/sdcard/sensor_data_recording");
        if (!sensorDataDir.exists()) {
            boolean firstCreate = sensorDataDir.mkdirs();
        }
        Toast.makeText(MainActivity.this, "Recording Sensor Data Now...", Toast.LENGTH_SHORT).show();
        mSensorManager.registerListener(MainActivity.this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);//SENSOR_DELAY_GAME:0.02s//SENSOR_DELAY_NORMAL:200000microsecond = 0.2s
    }

    public void stopRecordingSensor() {
        Log.d(TAG, "onCreate: stopRecording " + System.currentTimeMillis());
        Toast.makeText(MainActivity.this, "Stop Recording Now...", Toast.LENGTH_SHORT).show();
        mSensorManager.unregisterListener(MainActivity.this);
    }

    public class MyHandlers {
        public void onClickStart(View view) {
            startRecordingSensor();
        }

        public void onClickStop(View view) {
            stopRecordingSensor();
        }
    }
}
