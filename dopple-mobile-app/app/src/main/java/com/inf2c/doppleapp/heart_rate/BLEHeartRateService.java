package com.inf2c.doppleapp.heart_rate;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.ble.DoppleGattAttributes;
import com.inf2c.doppleapp.logging.DoppleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BLEHeartRateService extends Service {
    private final static String TAG = BLEHeartRateService.class.getSimpleName();

    //connection variables
    private String BLEDeviceName = "";
    private String BLEAddress = "";

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    //gatt intent actions
    public final static String ACTION_GATT_CONNECTED = "com.inf2c.doppleapp.heart_rate.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.inf2c.doppleapp.heart_rate.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.inf2c.doppleapp.heart_rate.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.inf2c.doppleapp.heart_rate.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.inf2c.doppleapp.heart_rate.le.EXTRA_DATA";
    public final static String EXTRA_DATA_TYPE = "com.inf2c.doppleapp.heart_rate.le.EXTRA_DATA_TYPE";

    //service events
    public final static String DOPPLE_ACTION_SET_HEARTBEAT = "com.inf2c.doppleapp.heart_rate.DOPPLE_ACTION_SET_HEARTBEAT";
    public final static String DOPPLE_ACTION_SET_BATTERY = "com.inf2c.doppleapp.heart_rate.DOPPLE_ACTION_SET_BATTERY";
    public final static String DOPPLE_ACTION_SET_MONITOR_NAME = "com.inf2c.doppleapp.heart_rate.DOPPLE_ACTION_SET_MONITOR_NAME";

    //service receiving events
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_NAME = "com.inf2c.doppleapp.heart_rate.DOPPLE_SERVICE_EVENT_REQUEST_NAME";
    public final static String DOPPLE_SERVICE_EVENT_DISCONNECT = "com.inf2c.doppleapp.heart_rate.DOPPLE_SERVICE_EVENT_DISCONNECT";
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_BATTERY = "com.inf2c.doppleapp.heart_rate.DOPPLE_SERVICE_EVENT_REQUEST_BATTERY";

    public BLEHeartRateService() {

    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        BLEDeviceName = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
        BLEAddress = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
        initialize();
        bindIntentReceiver();

        //do connect
        if(!BLEDeviceName.equals("Sample Heart Rate Monitor") && !BLEAddress.equals("FF:00:12:34")){
            if(!connect(BLEAddress)){
                this.stopSelf();
            }
        }
        return START_STICKY;
    }

    /**
     * Function that binds an intent filter to the service so that it can receive events.
     */
    private void bindIntentReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEHeartRateService.ACTION_GATT_CONNECTED);
        filter.addAction(BLEHeartRateService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BLEHeartRateService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BLEHeartRateService.ACTION_DATA_AVAILABLE);
        filter.addAction(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_NAME);
        filter.addAction(BLEHeartRateService.DOPPLE_SERVICE_EVENT_DISCONNECT);
        filter.addAction(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_BATTERY);
        registerReceiver(gattUpdateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(gattUpdateReceiver);
    }

    /**
     * Callback function for the gattUpdateReceiver
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                DoppleLog.i(TAG, "Monitor Connected");
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
               //monitor disconnected, remove resources and close service
                DoppleLog.i(TAG, "Monitor Disconnected");
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                retrieveDoppleGattCharacteristics(BLEHeartRateService.this.getSupportedGattServices());
                setNotificationOnGattCharacteristics(DoppleGattAttributes.DOPPLE_HEART_RATE_MEASUREMENT);
                DoppleLog.i(TAG, "Monitor Services Discovered");
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                DoppleLog.i(TAG, "Data Available");
                handleDataReceivedFromGatt(intent);
            } else if(DOPPLE_SERVICE_EVENT_REQUEST_NAME.equals(action)){
                DoppleLog.i(TAG, "Requesting name for fragment");
                sendBroadcast(DOPPLE_ACTION_SET_MONITOR_NAME, BLEDeviceName);
            } else if(DOPPLE_SERVICE_EVENT_DISCONNECT.equals(action)){
                disconnect();
                close();
            } else if(DOPPLE_SERVICE_EVENT_REQUEST_BATTERY.equals(action)){
                if(mGattCharacteristics != null) {
                    if(mGattCharacteristics.size() != 0){
                        readCharacteristic(characteristicSearch(DoppleGattAttributes.DOPPLE_HRM_BATTERY));
                    }
                }
            }
        }
    };

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            DoppleLog.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Function that retrieves the gatt characteristics from the gatt server.
     * @param gattServices
     */
    private void retrieveDoppleGattCharacteristics(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if(DoppleGattAttributes.contains(uuid))
                    charas.add(gattCharacteristic);
            }

            if(charas.size() != 0)
                mGattCharacteristics.add(charas);
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                DoppleLog.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            DoppleLog.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Function that setsup a connection with a bluetooth device.
     * @param address the bluetooth device's mac-address
     * @return returns if the connection worked.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            DoppleLog.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            DoppleLog.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        DoppleLog.d(TAG, "Trying to create a new connection.");
        return true;
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     *      * callback.
     *      *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            DoppleLog.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean succ = mBluetoothGatt.readCharacteristic(characteristic);
        DoppleLog.d(TAG, "readCharacteristic: " + succ);
    }

    /**
     * The gatt callback, this anonymous class instance handles anything that happens with the bluetooth device
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                DoppleLog.i(TAG, "Connected to GATT server.");
                DoppleLog.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                DoppleLog.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                DoppleLog.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            DoppleLog.d(TAG, "onCharacteristicWrite: " + ((status == BluetoothGatt.GATT_SUCCESS) ? "Success" : "false"));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            DoppleLog.d(TAG, "onCharacteristicRead()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            DoppleLog.d(TAG, "onDescriptorWrite: " + ((status == BluetoothGatt.GATT_SUCCESS) ? "Success" : "false"));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //Log.d(TAG, "onCharacteristicChanged()");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * Function that sends a normal broadcast through to the android operating system.
     * @param action
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Sends a pre-made broadcast as an intent
     * @param intentAction The action of the intent.
     * @param obj the String object to send along
     */
    private void sendBroadcast(String intentAction, String obj){
        Intent intent = new Intent(intentAction);
        intent.putExtra(EXTRA_DATA, obj);
        sendBroadcast(intent);
    }

    /**
     * Function that sends a processed broadcast from the gatt server into the current Service.
     * @param action the intents action
     * @param characteristic the characteristic from the server that changed.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        //Log.i(TAG, "Received Data broadcast message");
        if (characteristic.getUuid().equals(UUID.fromString(DoppleGattAttributes.DOPPLE_HEART_RATE_MEASUREMENT))) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                DoppleLog.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                DoppleLog.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            DoppleLog.d(TAG, String.format(Locale.UK,"Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA_TYPE, DoppleGattAttributes.DOPPLE_HEART_RATE_MEASUREMENT );
            intent.putExtra(EXTRA_DATA, heartRate);
        } else if(characteristic.getUuid().equals(UUID.fromString(DoppleGattAttributes.DOPPLE_HRM_BATTERY))){
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final int battery = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                DoppleLog.d(TAG, String.format(Locale.UK, "Received battery rate: %d", battery));
                intent.putExtra(EXTRA_DATA_TYPE, DoppleGattAttributes.DOPPLE_HRM_BATTERY);
                intent.putExtra(EXTRA_DATA, battery);
            }
        }
        sendBroadcast(intent);
    }

    /**
     * Function that handles received data from the gatt server hosted by the service
     * @param intent
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleDataReceivedFromGatt(Intent intent){
        String ActionType = intent.getStringExtra(BLEHeartRateService.EXTRA_DATA_TYPE);
        int Value = intent.getIntExtra(BLEHeartRateService.EXTRA_DATA, 0);
        if(ActionType.equals(DoppleGattAttributes.DOPPLE_HEART_RATE_MEASUREMENT)){
            sendBroadcast(BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT, String.valueOf(Value));
        }
        else if (ActionType.equals(DoppleGattAttributes.DOPPLE_HRM_BATTERY)){
            sendBroadcast(BLEHeartRateService.DOPPLE_ACTION_SET_BATTERY, Value);
        }
    }

    /**
     * Sends a pre-made broadcast as an intent
     * @param intentAction The action of the intent.
     * @param obj the int object to send along
     */
    private void sendBroadcast(String intentAction, int obj){
        Intent intent = new Intent(intentAction);
        intent.putExtra(EXTRA_DATA, obj);
        sendBroadcast(intent);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            DoppleLog.e(TAG, "BluetoothAdapter not initialized");
            return;
        }

        //write a descriptor to the ear buds in order to start receiving notifications
        if (DoppleGattAttributes.DOPPLE_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DoppleGattAttributes.DOPPLE_DESCRIPTOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            mBluetoothGatt.writeDescriptor(descriptor);
        }
        DoppleLog.i(TAG, "setCharacteristicNotification: " + mBluetoothGatt.setCharacteristicNotification(characteristic, enabled));
    }

    /**
     * Starts the notifications on a given gatt service Characteristics
     * @param charId UUID of the characteristic
     */
    private void setNotificationOnGattCharacteristics(String charId){
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic = characteristicSearch(charId);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                BLEHeartRateService.this.setCharacteristicNotification(characteristic, true);
            }
        }
    }

    /**
     * Function that searches through the services to find the provided characteristic
     * @param characteristicString
     * @return
     */
    private BluetoothGattCharacteristic characteristicSearch(String characteristicString){
        if (mGattCharacteristics != null){
            //loop through the services
            for (List<BluetoothGattCharacteristic> charList: mGattCharacteristics){
                //loop through the characteristics
                for(BluetoothGattCharacteristic characteristic: charList){
                    if(characteristic.getUuid().toString().equals(characteristicString)){
                        return characteristic;
                    }
                }
            }
        }
        //if the characteristic was not found return null
        return null;
    }
}
