package com.inf2c.doppleapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.net.ConnectivityManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

//Dopple class imports
import com.inf2c.doppleapp.apiExport.apiExport;
import com.inf2c.doppleapp.ble.*;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;
import com.inf2c.doppleapp.heart_rate.HeartBeat;
import com.inf2c.doppleapp.logging.DoppleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();


    private BluetoothAdapter bluetoothAdapter;
    private Fragment heartBeatFragment;

    //dopple object references
    private DoppleLowEnergyScanner leScanner;
    private List<BLEDeviceClass> devicesList = new ArrayList<>();

    private boolean checkIfServiceIsRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!uuidExists()) {
            createUUID();
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String id = settings.getString("UUID", "0");
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},0);
        //check if the service is running, if true -> start the data activity
        if(checkIfServiceIsRunning(BLEConnectionService.class)){
            Intent dataScreen = new Intent(getApplicationContext(), DataActivity.class);
            startActivity(dataScreen);
        }
        //Init things
        initBluetooth();
        assignListeners();

        //init scanner
        leScanner = new DoppleLowEnergyScanner(bluetoothAdapter, this);
        leScanner.addListener(scannerCallback);
        initScan();
       // sampleData();
    }

    /**
     * Check if the uuid was set in the phone storage, if not -> create and save a new one
     * @return true or false
     */
    private boolean uuidExists() {
        try {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            String id = settings.getString("UUID", "0");
            if(id != null && id != "" && id != "0") {
                return true;
            } else {
                return false;
            }
        } catch(Exception e) {
            return false;
        }
    }

    /**
     * Creates a new UUID
     */
    private void createUUID() {
        //Generate UUID
        UUID id = UUID.randomUUID();
        System.out.println(id);

        //Save UUID
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("UUID", id.toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupHeartRateServiceEventReceiver();
        if(leScanner.isScanning())
        {
            findViewById(R.id.scannerLoadingScreen).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.scannerLoadingScreen).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(serviceUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void sampleData(){
        BLEDeviceClass device = new BLEDeviceClass();
        device.DeviceName = "Sample Earbuds";
        device.MacAddress = "12:34:56:78";
        device.data = "test";
        BLEDeviceClass deviceHR = new BLEDeviceClass();
        deviceHR.DeviceName = "Sample Heart Rate Monitor";
        deviceHR.MacAddress = "FF:00:12:34";
        deviceHR.data = "";

        devicesList.add(device);
        devicesList.add(deviceHR);
        BluetoothDeviceAdapter ad = new BluetoothDeviceAdapter(this, devicesList, leScanner);
        ad.addListener(scannerCallback);
        ListView lv = findViewById(R.id.simpleListView);
        lv.setAdapter(ad);
    }

    /**
     * Function that fixes all the requirements for bluetooth
     */
    private void initBluetooth(){
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE wordt niet ondersteund", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Zet je bluetooth aan", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This function starts the scanning for BLE devices
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initScan(){
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            //Toast.makeText(this, "Scanning...", Toast.LENGTH_LONG).show();
            leScanner.scanLeDevice();

            if(leScanner.isScanning())
            {
                findViewById(R.id.scannerLoadingScreen).setVisibility(View.VISIBLE);
            }
            else
            {
                findViewById(R.id.scannerLoadingScreen).setVisibility(View.GONE);
            }
        }
    }

    /**
     * Function that connects all listener events to the UI items
     */
    private void assignListeners(){
        //settings button
        Button btnOpenSettings = findViewById(R.id.btnGoToSettings);
        btnOpenSettings.setOnClickListener(view -> openBluetoothSettings());

        findViewById(R.id.viewSessionButton).setOnClickListener(view -> {
            Intent sessionScreen = new Intent(getApplicationContext(), SessionsActivity.class);
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_NAME", "Niet verbonden");
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", "");
            startActivity(sessionScreen);
        });
    }

    private void removeHeartbeatFragment(){
        if(heartBeatFragment != null) {
            heartBeatFragment.getFragmentManager().beginTransaction().remove(heartBeatFragment).commit();
        }
    }

    private void createHeartbeatFragment(){
        heartBeatFragment = HeartBeat.getInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragmentTarget, heartBeatFragment).commitNow();

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_NAME));
            sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_BATTERY));
        }, 3000);
    }

    /**
     * Opens the bluetooth settings menu on the phone
     */
    private void openBluetoothSettings(){
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);
    }

    /**
     * Callback for the scanner
     */
    private DoppleLowEnergyCallback scannerCallback = new DoppleLowEnergyCallback() {
        @Override
        public void onDeviceListReceived(List<BLEDeviceClass> items) {}

        @Override
        public void onJayBirdDeviceReceived(BLEDeviceClass item) {
            for(BLEDeviceClass d: devicesList){
                if(d.MacAddress.equals(item.MacAddress)){
                    return;
                }
            }
            devicesList.add(item);
            ListView lv = findViewById(R.id.simpleListView);
            BluetoothDeviceAdapter ad = new BluetoothDeviceAdapter(getApplicationContext(), devicesList, leScanner);
            ad.addListener(scannerCallback);
            lv.setAdapter(ad);
        }

        @Override
        public void onStartDeviceConnection(final BLEDeviceClass device) {
            if(!device.data.isEmpty()){
                //start a new activity with this new device
                leScanner.stopLeScanDevice();
                runOnUiThread(() -> {
                    Intent dataScreen = new Intent(getApplicationContext(), DataActivity.class);
                    dataScreen.putExtra("EXTRA_DOPPLE_DEVICE_NAME", device.DeviceName);
                    dataScreen.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", device.MacAddress);
                    startActivity(dataScreen);
                });
            }
            else{
                //connect to heart rate monitor
                runOnUiThread(() -> {
                    Intent gattServiceIntent = new Intent(MainActivity.this, BLEHeartRateService.class);
                    gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_NAME", device.DeviceName);
                    gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", device.MacAddress);
                    startService(gattServiceIntent);
                });
                createHeartbeatFragment();

                ListView lv = findViewById(R.id.simpleListView);
                BluetoothDeviceAdapter ad = new BluetoothDeviceAdapter(getApplicationContext(), devicesList, leScanner);
                ad.addListener(scannerCallback);
                lv.setAdapter(ad);
            }
        };
    };



    private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver(){
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action){
                case BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT:
                    String heartRate = intent.getStringExtra(BLEHeartRateService.EXTRA_DATA);
                    setHeartRate(heartRate);
                    break;
                case BLEHeartRateService.DOPPLE_ACTION_SET_BATTERY:
                    DoppleLog.d(TAG, "Set battery");
                    setBattery(intent.getIntExtra(BLEHeartRateService.EXTRA_DATA, 0));
                    break;
                case BLEHeartRateService.DOPPLE_ACTION_SET_MONITOR_NAME:
                    DoppleLog.d(TAG, "Set name");
                    setHeartbeatFragmentName(intent.getStringExtra(BLEHeartRateService.EXTRA_DATA));
                    break;
            }
        }
    };

    private void setupHeartRateServiceEventReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_BATTERY);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_MONITOR_NAME);
        registerReceiver(serviceUpdateReceiver, filter);
    }

    /**
     * Function to set the Heart Rate on the UI
     * @param heartRate steps as int
     */
    private void setHeartRate(final String heartRate){
        ((HeartBeat)heartBeatFragment).setBPM(heartRate);
    }

    private void setHeartbeatFragmentName(String name){
        ((HeartBeat)heartBeatFragment).setText(name);
    }

    private void setBattery(int batteryLevel){
        ((HeartBeat)heartBeatFragment).setBattery(batteryLevel);
    }
}
