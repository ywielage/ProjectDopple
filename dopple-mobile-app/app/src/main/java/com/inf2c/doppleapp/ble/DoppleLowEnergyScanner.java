package com.inf2c.doppleapp.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class DoppleLowEnergyScanner {
    private final static String TAG = DoppleLowEnergyScanner.class.getSimpleName();
    private List<DoppleLowEnergyCallback> listeners = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private AppCompatActivity mainActivity;
    private Handler handler;


    public DoppleLowEnergyScanner(BluetoothAdapter e, AppCompatActivity ac){
        this.bluetoothAdapter = e;
        this.mainActivity = ac;
        handler = new Handler();
        ActivityCompat.requestPermissions(this.mainActivity, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},0);
    }

    /**
     * adds the event listeners
     * @param toAdd event listener object
     */
    public void addListener(DoppleLowEnergyCallback toAdd) {
        listeners.add(toAdd);
    }

    private void returnBLEDeviceList(List<BLEDeviceClass> e) {
        //get the gps data
        for(DoppleLowEnergyCallback li: listeners){
            li.onDeviceListReceived(e);
        }
    }

    private void returnBLEDevice(BLEDeviceClass device){
        for(DoppleLowEnergyCallback li: listeners){
            li.onJayBirdDeviceReceived(device);
        }
    }

    private boolean scanning;

    public boolean isScanning(){
        return scanning;
    }

    private void setIsScanning(boolean scan){
        scanning = scan;
    }

    /**
     * Starts scanning for devices.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void scanLeDevice(){
        ScanFilter.Builder builder = new ScanFilter.Builder();
        List<ScanFilter> filter = new ArrayList<>();
        filter.add(builder.build());
        final ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.startScan(filter, builderScanSettings.build(), scannerCallback);
        setIsScanning(true);
    }

    public void stopLeScanDevice(){
        scanner.stopScan(scannerCallback);
        setIsScanning(false);
    }

    private ScanCallback scannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice fndDevice = result.getDevice();
            BLEDeviceClass object = new BLEDeviceClass();
            ScanRecord record = result.getScanRecord();
            byte[] manData = record.getManufacturerSpecificData(0x01DA);
            if(manData != null){
                object.DeviceName = fndDevice.getName();
                object.MacAddress = fndDevice.getAddress();
                object.data = manData.toString();

                returnBLEDevice(object);
            }
            else{
                List<ParcelUuid> records = record.getServiceUuids();
                if(records != null){
                    Log.d(TAG, "records found: " + records.size());

                    if(records.contains(ParcelUuid.fromString(DoppleGattAttributes.DOPPLE_HRM_UUID))){
                        //we have a winner
                        object.DeviceName = fndDevice.getName();
                        object.MacAddress = fndDevice.getAddress();
                        object.data = "";
                        returnBLEDevice(object);
                    }
                }
            }

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            List<BLEDeviceClass> deviceNames = new ArrayList<>();
            for(ScanResult deviceResult: results){
                BluetoothDevice fndDevice = deviceResult.getDevice();
                BLEDeviceClass object = new BLEDeviceClass();
                ScanRecord record = deviceResult.getScanRecord();
                byte[] manData = record.getManufacturerSpecificData(0x01DA);
                if(manData != null){
                    object.DeviceName = fndDevice.getName();
                    object.MacAddress = fndDevice.getAddress();
                    object.data = manData.toString();

                    deviceNames.add(object);
                }
            }
            //send the list back to the main class
            returnBLEDeviceList(deviceNames);
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}
