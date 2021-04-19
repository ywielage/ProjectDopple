package com.inf2c.doppleapp.ble;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.inf2c.doppleapp.APIConnection.API_CONNECTION;
import com.inf2c.doppleapp.DataActivity;
import com.inf2c.doppleapp.R;
import com.inf2c.doppleapp.SessionsActivity;
import com.inf2c.doppleapp.apiExport.apiExport;
import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.conversion.DoppleProcessedDataObject;
import com.inf2c.doppleapp.conversion.DoppleRawDataObject;
import com.inf2c.doppleapp.export.DoppleFileHandler;
import com.inf2c.doppleapp.export.ExportFileType;
import com.inf2c.doppleapp.gps.DoppleGPSManager;
import com.inf2c.doppleapp.gps.GPSLocation;
import com.inf2c.doppleapp.gps.ReturnableGPSData;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;
import com.inf2c.doppleapp.jsonConversion.models.Json.JsonObject;
import com.inf2c.doppleapp.logging.DoppleLog;
import com.inf2c.doppleapp.text_to_speech.DoppleSpeech;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BLEConnectionService extends Service {

    private final static String TAG = BLEConnectionService.class.getSimpleName();

    //connection variables
    private String BLEDeviceName = "";
    private String BLEAddress = "";

    //Dopple lists
    private List<DoppleDataObject> currentLapData;
    private HashMap<String, List<DoppleDataObject>> recordedLapData;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;
    private ArrayList<Integer> timesForVoice;
    private ArrayList<Integer> timesHadForVoice;

    //dopple vars
    private int stepCount = 0;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int Seconds, Minutes, MilliSeconds, Hours;
    private String storedTime;
    private boolean calcDistance = false;
    private String heartBeat = "0";

    private boolean running = false;

    private DoppleFileHandler fileHandler;
    public DoppleGPSManager gpsManager;
    private DoppleSpeech speech;
    private Handler stopwatch;
    private NotificationCompat.Builder mNotificationBuilder;
    private PowerManager.WakeLock wakeLock;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    //gatt intent actions
    public final static String ACTION_GATT_CONNECTED = "com.inf2c.doppleapp.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.inf2c.doppleapp.ble.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.inf2c.doppleapp.ble.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.inf2c.doppleapp.ble.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.inf2c.doppleapp.ble.le.EXTRA_DATA";
    public final static String EXTRA_DATA_RAW = "com.inf2c.doppleapp.ble.le.EXTRA_DATA_RAW";
    public final static String EXTRA_DATA_TYPE = "com.inf2c.doppleapp.ble.le.EXTRA_DATA_TYPE";

    //service receive event intent actions
    public final static String DOPPLE_SERVICE_EVENT_START_RECORDING = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_START_RECORDING";
    public final static String DOPPLE_SERVICE_EVENT_STOP_RECORDING = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_STOP_RECORDING";
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_RECORDING = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_REQUEST_RECORDING";
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT";
    public final static String DOPPLE_SERVICE_EVENT_RECORDING_STATUS = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_RECORDING_STATUS";
    public final static String DOPPLE_SERVICE_EVENT_SET_DEVICE_VALUES = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_SET_DEVICE_VALUES";
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES";
    public final static String DOPPLE_SERVICE_EVENT_NEW_LAP = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_NEW_LAP";
    public final static String DOPPLE_SERVICE_EVENT_REQUEST_LAPS = "com.inf2c.doppleapp.ble.DOPPLE_SERVICE_EVENT_REQUEST_LAPS";

    //service event intent actions
    public final static String DOPPLE_ACTION_SET_SPEED = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_SPEED";
    public final static String DOPPLE_ACTION_SET_DISTANCE = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_DISTANCE";
    public final static String DOPPLE_ACTION_SET_X = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_X";
    public final static String DOPPLE_ACTION_SET_Y = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_Y";
    public final static String DOPPLE_ACTION_SET_Z = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_Z";
    public final static String DOPPLE_ACTION_SET_STEPS = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_STEPS";
    public final static String DOPPLE_ACTION_SET_CONTACT_TIME = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_CONTACT_TIME";
    public final static String DOPPLE_ACTION_SET_STEP_FREQ = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_STEP_FREQ";
    public final static String DOPPLE_ACTION_SET_STEP_FREQ_MIN = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_STEP_FREQ_MIN";
    public final static String DOPPLE_ACTION_SET_STEP_FREQ_MAX = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_STEP_FREQ_MAX";
    public final static String DOPPLE_ACTION_SET_STEP_FREQ_AVERAGE = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_STEP_FREQ_AVERAGE";
    public final static String DOPPLE_ACTION_SET_TIMER = "com.inf2c.doppleapp.ble.DOPPLE_ACTION_SET_TIMER";
    public final static String DOPPLE_EVENT_EARBUDS_CONNECTED = "com.inf2c.doppleapp.ble.DOPPLE_EVENT_EARBUDS_CONNECTED";
    public final static String DOPPLE_EVENT_EARBUDS_DISCONNECTED = "com.inf2c.doppleapp.ble.DOPPLE_EVENT_EARBUDS_DISCONNECTED";
    public final static String DOPPLE_EVENT_SET_LAP_TIME = "com.inf2c.doppleapp.ble.DOPPLE_EVENT_SET_LAP_TIME";
    public final static String DOPPLE_EVENT_SET_LAPS = "com.inf2c.doppleapp.ble.DOPPLE_EVENT_SET_LAPS";
    private static Context context;

    public BLEConnectionService() {
        currentLapData = new ArrayList<>();
        recordedLapData = new HashMap<>();
        timesForVoice = new ArrayList<>(Arrays.asList(1,10,20,30,40,50,60));
        timesHadForVoice = new ArrayList<>();
        DoppleLog.setWriteToRecorder(true);
    }

    /**
     * binds a context from another activity to this service for use with other classes
     * @param activity activity for the service.
     */
    public void setServiceContext(Context activity, String DeviceName, String DeviceAddress){
        //init gps manager
        gpsManager = new DoppleGPSManager(activity);
        gpsManager.addListener(gpsListener);

        //init file handler
        fileHandler = new DoppleFileHandler(activity, DeviceName, DeviceAddress);

        //init stopwatch handler
        stopwatch = new Handler();

        //start the speech engine
        speech = new DoppleSpeech(this);

        this.context = activity;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("WakelockTimeout")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        BLEDeviceName = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
        BLEAddress = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
        initialize();
        bindIntentReceiver();

        if(!BLEDeviceName.equals("Sample Earbuds") && !BLEAddress.equals("12:34:56:78")){
            connect(BLEAddress);
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();

        setServiceContext(this, BLEDeviceName, BLEAddress);
        return START_REDELIVER_INTENT;
    }

    /**
     * Start the recording session
     */
    public void startReceiver(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundNotification(context);
        }
        else {
            startForeground(1, new Notification());
        }

        //reset the service status to 0
        currentLapData.clear();
        recordedLapData.clear();
        timesHadForVoice.clear();
        storedTime = "";
        MillisecondTime  = 0L;
        StartTime = 0L;
        TimeBuff  = 0L;
        UpdateTime = 0L ;
        Seconds = 0;
        Minutes = 0;
        MilliSeconds = 0;
        Hours = 0;
        stepCount = 0;

        startACCDataReceiver();
        setRecording(true);
    }

    /**
     * Stop the recording session
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void stopReceiver(Context context){
        stopACCDataReceiver();
        setNotificationTitleText("Opname gestopt");
        setNotificationContentText("Eind tijd: " + storedTime);
        buildNotification();

        saveData();

        //gpsManager.disableLocationUpdates();
        setRecording(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveData(){
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        recordedLapData.put(storedTime, new ArrayList<>(currentLapData));
        currentLapData.clear();
        File csvFile = fileHandler.saveDataToFile(recordedLapData, ExportFileType.CSV, stamp);
        File tcxFile = fileHandler.saveDataToFile(recordedLapData, ExportFileType.TCX, stamp);
        apiExport.exportData(tcxFile, csvFile);
    }

    /**
     * Function that binds an intent filter to the service so that it can receive events.
     */
    private void bindIntentReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEConnectionService.ACTION_GATT_CONNECTED);
        filter.addAction(BLEConnectionService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BLEConnectionService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BLEConnectionService.ACTION_DATA_AVAILABLE);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_START_RECORDING);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_STOP_RECORDING);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_RECORDING);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_NEW_LAP);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_LAPS);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT);
        registerReceiver(gattUpdateReceiver, filter);
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(gattUpdateReceiver);
        gpsManager.disableLocationUpdates();
        speech.close();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        DoppleLog.exportLog(this);
        DoppleLog.setWriteToRecorder(false);
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
                gatt.requestMtu(515);
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
     * Function that sends a processed broadcast from the gatt server into the current Service.
     * @param action the intents action
     * @param characteristic the characteristic from the server that changed.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        //Log.i(TAG, "Received Data broadcast message");
        if (characteristic.getUuid().equals(UUID.fromString(DoppleGattAttributes.PROCESSED_ACC_DATA))) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data){
                    stringBuilder.append(String.format("%02X", byteChar));
                }

                intent.putExtra(EXTRA_DATA_TYPE, DoppleGattAttributes.PROCESSED_ACC_DATA);
                intent.putExtra(EXTRA_DATA_RAW, data);
                intent.putExtra(EXTRA_DATA, stringBuilder.toString());
                DoppleLog.i(TAG, "Received Step Data");
            }
        } else if(characteristic.getUuid().equals(UUID.fromString(DoppleGattAttributes.RAW_ACC_DATA))){
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data){
                    stringBuilder.append(String.format("%02X", byteChar));
                }
                intent.putExtra(EXTRA_DATA_TYPE, DoppleGattAttributes.RAW_ACC_DATA);
                intent.putExtra(EXTRA_DATA_RAW, data);
                intent.putExtra(EXTRA_DATA, stringBuilder.toString());
            }
        }
        else{
            //unknown broadcast: ignore...
        }
        sendBroadcast(intent);
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
     * Gets the recording status of the Service
     * @return returns a boolean
     */
    public Boolean isRecording(){
        return running;
    }

    /**
     * Sets the recording status on the service, only acts as a boolean.
     * @param recordingStatus true or false.
     */
    public void setRecording(boolean recordingStatus){
        running = recordingStatus;
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
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Writes a value to a characteristics on the gatt server
     * @param characteristic the characteristic to write
     */
    public void writeCharacteristics(BluetoothGattCharacteristic characteristic){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            DoppleLog.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
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
        if (DoppleGattAttributes.PROCESSED_ACC_DATA.equals(characteristic.getUuid().toString())
                || DoppleGattAttributes.RAW_ACC_DATA.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(DoppleGattAttributes.DOPPLE_DESCRIPTOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            mBluetoothGatt.writeDescriptor(descriptor);
        }
        DoppleLog.i(TAG, "setCharacteristicNotification: " + mBluetoothGatt.setCharacteristicNotification(characteristic, enabled));
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
     * Starts the acceleration meter data receiver
     */
    private void startACCDataReceiver(){
        writeGattCharacteristics(DoppleGattAttributes.START_STEP_DETECTION,new byte[] {0x01}); //writes the 0x01 byte to the second characteristics
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //enabled the notify option on the 4th characteristics (raw)
                setNotificationOnGattCharacteristics(DoppleGattAttributes.RAW_ACC_DATA);
                startTimer();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //enabled the notify option on the 1st characteristics (processed)
                        setNotificationOnGattCharacteristics(DoppleGattAttributes.PROCESSED_ACC_DATA);
                    }
                }, 1000);
            }
        }, 1000);
    }

    /**
     * Stops the acceleration meter data receiver
     */
    private void stopACCDataReceiver(){
        writeGattCharacteristics(DoppleGattAttributes.START_STEP_DETECTION, new byte[] {0x00}); //writes the 0x00 byte to the second characteristics
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopNotificationOnGattCharacteristics(DoppleGattAttributes.RAW_ACC_DATA); //enabled the notify option on the 4th characteristics
                stopTimer();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopNotificationOnGattCharacteristics(DoppleGattAttributes.PROCESSED_ACC_DATA); //enabled the notify option on the 4th characteristics
                        stopForeground(true);
                        String[] time = storedTime.split(":");
                        speech.saySessionStopped(Integer.parseInt(time[0]), Integer.parseInt(time[1]), Integer.parseInt(time[2]));
                    }
                }, 1000);
            }
        }, 1000);
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
     * Writes a byte array value to a given Characteristics
     * @param charId UUID of the characteristic
     * @param value value to write in byte array
     */
    private void writeGattCharacteristics(String charId, byte[] value){
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic = characteristicSearch(charId);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                characteristic.setValue(value);
                BLEConnectionService.this.writeCharacteristics(characteristic);
            }
        }
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
                BLEConnectionService.this.setCharacteristicNotification(characteristic, true);
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

    /**
     * Stops the notifications on a given gatt service Characteristics
     * @param charId UUID of the characteristic
     */
    private void stopNotificationOnGattCharacteristics(String charId){
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic = characteristicSearch(charId);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                BLEConnectionService.this.setCharacteristicNotification(characteristic, false);
            }
        }
    }

    /**
     * Function that handles received data from the gatt server hosted by the service
     * @param intent
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleDataReceivedFromGatt(Intent intent){
        String ActionType = intent.getStringExtra(BLEConnectionService.EXTRA_DATA_TYPE);
        String Data = intent.getStringExtra(BLEConnectionService.EXTRA_DATA); //this is the incoming data converted to a hex string
        byte[] RawData = intent.getByteArrayExtra(BLEConnectionService.EXTRA_DATA_RAW); //this is the raw data in byte[] format
        if(ActionType.equals(DoppleGattAttributes.RAW_ACC_DATA)){
            if(RawData.length > 8){
                DoppleConversion converter = new DoppleConversion();
                DoppleRawDataObject dataObjectRaw = converter.convert(Data);
                dataObjectRaw.HexData = Data;
                dataObjectRaw.DeviceTimestamp = System.currentTimeMillis();

                int sampleCount = 20;
                double time = 0.4;
                double mTimeFactor = time / sampleCount;
                for(int i = 0; i < sampleCount; i++){
                    DoppleDataObject currentObject = converter.getXYZCount(dataObjectRaw, i);

                    /* uitgecomment vanwege redundancy*/
                    //convert xyz to velocity and distance
                    //double acceleration = converter.acceleration(currentObject.X, currentObject.Y, currentObject.Z);
                    //double deltaVelocity = converter.velocity(acceleration, mTimeFactor);
                    //Log.d(TAG, "recorded change in velocity: " + velocity + "m/s + " + deltaVelocity);

                    //velocity += deltaVelocity;
                    //double deltaDistance = converter.distance(velocity, mTimeFactor);
                    //Log.d(TAG, "received Delta distance: " + deltaDistance + "m");
                    // distance += deltaDistance >= 0.25 ? deltaDistance : 0;

                    //update UI
                    sendBroadcast(DOPPLE_ACTION_SET_X, converter.round(currentObject.X));
                    sendBroadcast(DOPPLE_ACTION_SET_Y, converter.round(currentObject.Y));
                    sendBroadcast(DOPPLE_ACTION_SET_Z, converter.round(currentObject.Z));

                    gpsManager.getLocation(currentObject); //get a location with this object
                }
            }
        }
        else if(ActionType.equals(DoppleGattAttributes.PROCESSED_ACC_DATA)){
            if(RawData.length > 4){
                DoppleConversion converter = new DoppleConversion();
                DoppleProcessedDataObject converted =  converter.convertStepInfo(Data);
                converted.DeviceTimestamp = System.currentTimeMillis();
                stepCount = stepCount + converted.Steps;
                sendBroadcast(DOPPLE_ACTION_SET_STEPS, stepCount);
                for(int i = 0; i < converted.Steps; i++){
                    int stepFreq = converted.StepData.get("StepFreq" + i);
                    int contactTime = converted.StepData.get("ContactTime" + i);

                    if(stepFreq != -1 && stepFreq != 0){
                        if(stepFreq < StepFreq_Min || StepFreq_Min == 0){
                            StepFreq_Min = stepFreq;
                        }
                        else if(stepFreq > StepFreq_Max) {
                            StepFreq_Max = stepFreq;
                        }
                        StepFreq_Average = ((StepFreq_Average_Count * StepFreq_Average) + stepFreq) / ++StepFreq_Average_Count;
                        sendBroadcast(DOPPLE_ACTION_SET_STEP_FREQ, stepFreq);
                        sendBroadcast(DOPPLE_ACTION_SET_STEP_FREQ_MIN, StepFreq_Min);
                        sendBroadcast(DOPPLE_ACTION_SET_STEP_FREQ_MAX, StepFreq_Max);
                        sendBroadcast(DOPPLE_ACTION_SET_STEP_FREQ_AVERAGE, StepFreq_Average);
                    }

                    sendBroadcast(DOPPLE_ACTION_SET_CONTACT_TIME, contactTime);

                    DoppleDataObject currentObject = new DoppleDataObject(converted.DeviceTimestamp, converted.EarbudsTimestamp, converted.Steps, stepFreq, contactTime, this.heartBeat);
                    gpsManager.getLocation(currentObject);
                }

            }
        }
    }

    private int StepFreq_Min, StepFreq_Max, StepFreq_Average, StepFreq_Average_Count = 0;

    /**
     * Callback function for the gattUpdateReceiver
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                sendBroadcast(DOPPLE_EVENT_EARBUDS_CONNECTED, BLEDeviceName);
                speech.sayConnectedTo(BLEDeviceName);
                gpsManager.enableLocationUpdates();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                sendBroadcast(DOPPLE_EVENT_EARBUDS_DISCONNECTED, BLEDeviceName);
                if(isRecording()){
                    saveData();
                }
                gpsManager.disableLocationUpdates();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                retrieveDoppleGattCharacteristics(BLEConnectionService.this.getSupportedGattServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                handleDataReceivedFromGatt(intent);
            } else if(DOPPLE_SERVICE_EVENT_START_RECORDING.equals(action)){
                startReceiver(BLEConnectionService.this);
                speech.saySessionStarted();
                sendBroadcast(BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS, isRecording());
            } else if(DOPPLE_SERVICE_EVENT_STOP_RECORDING.equals(action)){
                stopReceiver(context);
                sendBroadcast(BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS, isRecording());
            } else if(DOPPLE_SERVICE_EVENT_REQUEST_RECORDING.equals(action)){
                //check recording and throw status intent
                sendBroadcast(BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS, isRecording());
            } else if(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT.equals(action)){
                if(!isRecording()){
                    disconnect();
                    close();
                    stopSelf();
                }
            } else if(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES.equals(action)){
                Intent serviceIntent = new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_SET_DEVICE_VALUES);
                serviceIntent.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
                serviceIntent.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
                sendBroadcast(serviceIntent);
            } else if(BLEConnectionService.DOPPLE_SERVICE_EVENT_NEW_LAP.equals(action)){
                if(running){
                    speech.sayGoodJob();
                    addLap();
                    setLaps();
                }
            } else if(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_LAPS.equals(action)){
               if(running){
                   setLaps();
               }
            } else if(BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT.equals(action)){
                heartBeat =  intent.getStringExtra(BLEHeartRateService.EXTRA_DATA);
            }
        }
    };

    /**
     * Adds an internal lap item
     */
    private void addLap(){
        recordedLapData.put(storedTime, new ArrayList<>(currentLapData));
        currentLapData.clear();

        //send the data to the frontend.
        Intent broadcastUpdateLapTime = new Intent(BLEConnectionService.DOPPLE_EVENT_SET_LAP_TIME);
        broadcastUpdateLapTime.putExtra(EXTRA_DATA, storedTime);
        sendBroadcast(broadcastUpdateLapTime);
    }

    /**
     * Sends the current recorded laps to the frontend.
     */
    private void setLaps(){
        Intent broadcastUpdateLaps= new Intent(BLEConnectionService.DOPPLE_EVENT_SET_LAPS);
        ArrayList<String> list1 = new ArrayList<>(recordedLapData.keySet());
        broadcastUpdateLaps.putStringArrayListExtra(EXTRA_DATA, list1);
        sendBroadcast(broadcastUpdateLaps);
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
     * Sends a pre-made broadcast as an intent
     * @param intentAction The action of the intent.
     * @param obj the boolean object to send along
     */
    private void sendBroadcast(String intentAction, boolean obj){
        Intent intent = new Intent(intentAction);
        intent.putExtra(EXTRA_DATA, obj);
        sendBroadcast(intent);
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
     * Sends a pre-made broadcast as an intent
     * @param intentAction The action of the intent.
     * @param obj the double object to send along
     */
    private void sendBroadcast(String intentAction, double obj){
        Intent intent = new Intent(intentAction);
        intent.putExtra(EXTRA_DATA, obj);
        sendBroadcast(intent);
    }

    /**
     * GPS listener callback
     */
    private ReturnableGPSData gpsListener = new ReturnableGPSData() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onGPSLocationReceived(GPSLocation location, DoppleDataObject object) {
            object.Long = location.longitude;
            object.Lat = location.latitude;
            currentLapData.add(object);

            DoppleConversion conversion = new DoppleConversion();
            if(Integer.parseInt(storedTime.split(":")[2]) % 10 == 0){
                if(calcDistance){
                    sendBroadcast(DOPPLE_ACTION_SET_DISTANCE, conversion.round(getDistance()));
                    calcDistance = false;
                    DoppleLog.d(TAG, "Calculating distance for UI");
                }
            }
            else{
                calcDistance = true;
            }

            sendBroadcast(DOPPLE_ACTION_SET_SPEED, conversion.round(location.speed * 3.6));
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onError(String error, int queueSize) {
            if(queueSize == 100)
            Toast.makeText(getApplicationContext(), "GPS error: " + error, Toast.LENGTH_SHORT).show();
            else if(queueSize > 500){
                //stop the recording, this is bad for the app
                stopReceiver(getApplicationContext());
                sendBroadcast(BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS, isRecording());
            }
        }
    };

    /**
     * Gets distance between two gps coordinates
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private double getDistance() {

        List<DoppleDataObject> csvExportData = new ArrayList<>();

        List<String> laps = new ArrayList<>(this.recordedLapData.keySet());
        laps.sort((o1, o2) -> {
            Time time1 = Time.valueOf(o1);
            Time time2 = Time.valueOf(o2);
            return time1.compareTo(time2);
        });

        for(String lapTime: laps){
            csvExportData.addAll(recordedLapData.get(lapTime));
        }

        return new DoppleConversion().getTotalDistance(csvExportData);
    }

    /**
     * Starts the Session recorder timer.
     */
    private void startTimer(){
        //for the time counter
        StartTime = SystemClock.uptimeMillis();
        stopwatch.postDelayed(RecordingCounter, 0);
    }

    /**
     * Stops the Session recorder timer.
     */
    private void stopTimer(){
        MillisecondTime = 0L ;
        StartTime = 0L ;
        TimeBuff = 0L ;
        UpdateTime = 0L ;
        Seconds = 0 ;
        Minutes = 0 ;
        MilliSeconds = 0 ;
        stopwatch.removeCallbacks(RecordingCounter);
    }

    /**
     * Session recorder, sends periodic events to UI and updates a possible notification.
     */
    public Runnable RecordingCounter = new Runnable() {
        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Hours = Minutes / 60;
            Minutes = Minutes % 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);

            StringBuilder time = new StringBuilder();
            time.append(String.format("%02d", Hours));
            time.append(":");
            time.append(String.format("%02d", Minutes));
            time.append(":");
            time.append(String.format("%02d", Seconds));
            storedTime = time.toString();
            time.append(".");
            time.append(String.format("%03d", MilliSeconds));

            sendBroadcast(DOPPLE_ACTION_SET_TIMER, time.toString());
            setNotificationContentText(storedTime);
            buildNotification();
            voiceCommand(Minutes);
            stopwatch.postDelayed(this, 50);
        }
    };

    public void voiceCommand(int minutes){
        Minutes = minutes;
        if(timesForVoice.contains(Minutes) && !timesHadForVoice.contains(Minutes)){
            speech.sayStepCount(stepCount);
            timesHadForVoice.add(Minutes);
        }
    }

    /**
     * Sets a permanent notification for this service
     * @param context the service context
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundNotification(Context context){
        String NOTIFICATION_CHANNEL_ID = "com.inf2c.doppleapp";
        String channelName = "Dopple BLE Connections";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setSound(null, null);
        chan.setLightColor(Color.parseColor("#1ACE78"));
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Intent notificationIntent = new Intent(DOPPLE_SERVICE_EVENT_STOP_RECORDING);
        PendingIntent stopRecordingPendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

        Intent notificationIntentLap = new Intent(DOPPLE_SERVICE_EVENT_NEW_LAP);
        PendingIntent createNewLapPendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntentLap, 0);

        Intent sessionIntent = new Intent(this, SessionsActivity.class);
        sessionIntent.setAction("DOPPLE_ACTION_OPEN_SESSIONS"); //todo: turn into global var
        sessionIntent.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
        sessionIntent.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
        PendingIntent pendingIntentSessions = PendingIntent.getActivity(this, 0, sessionIntent, 0);

        Intent dataIntent = new Intent(this, DataActivity.class);
        PendingIntent pendingData = PendingIntent.getActivity(context, 0, dataIntent, 0);

        mNotificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        mNotificationBuilder.setOngoing(true)
                .setColor(Color.parseColor("#1ACE78"))
                .setColorized(true)
                .setSmallIcon(R.drawable.ic_doppleappnotiicon)
                .setSubText("Verbonden met " + BLEDeviceName)
                .setContentTitle("Opname loopt")
                .setContentText("00:00:00")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingData)
                .addAction(0, "Stop Opname", stopRecordingPendingIntent)
                .addAction(0, "Sessions", pendingIntentSessions)
                .addAction(0, "Lap", createNewLapPendingIntent);

        buildNotification();
    }

    /**
     * Sets the notification content text
     * @param text content text
     */
    public void setNotificationContentText(String text){
        if(mNotificationBuilder != null){
            mNotificationBuilder.setContentText(text);
        }
    }

    /**
     * Sets the notification content text
     * @param text title text
     */
    public void setNotificationTitleText(String text){
        if(mNotificationBuilder != null){
            mNotificationBuilder.setContentTitle(text);
        }
    }

    /**
     * Builds and sets a notification for this service
     */
    public void buildNotification(){
        if(mNotificationBuilder != null){
            Notification n = mNotificationBuilder.build();
            startForeground(2, n);
        }
    }
}
