package com.inf2c.doppleapp.ble;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothGatt;

import java.util.List;

public interface DoppleLowEnergyCallback {
    void onDeviceListReceived(List<BLEDeviceClass> items);
    void onJayBirdDeviceReceived(BLEDeviceClass item);
    void onStartDeviceConnection(BLEDeviceClass device);
}
