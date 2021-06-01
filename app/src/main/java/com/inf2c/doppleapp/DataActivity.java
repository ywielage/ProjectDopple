package com.inf2c.doppleapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inf2c.doppleapp.ble.BLEConnectionService;
import com.inf2c.doppleapp.carousel.Carousel;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;
import com.inf2c.doppleapp.heart_rate.HeartBeat;
import com.inf2c.doppleapp.logging.DoppleLog;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataActivity extends AppCompatActivity {
    private final static String TAG = DataActivity.class.getSimpleName();

    //connection variables
    private String BLEDeviceName = "";
    private String BLEAddress = "";

    //dopple service control
    private boolean isRecording = false;
    private boolean isConnected = false;
    //views
    //private TextView tvStopwatchCounter;
    private ImageView sessionBtnImage;
    private TextView sessionBtnLabel;
    private TextView lapActualTimeLabel;
    private TextView lapTimeLabel;
    private RelativeLayout sessionBtn;
    private Fragment heartBeatFragment;

    private String lastLapTime = "00:00:00";

    public DataActivity(){

    }

    private boolean isBLEServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        if(!isBLEServiceRunning(BLEConnectionService.class)){
            setContentView(R.layout.loading_screen);

            //set name and address
            Intent invokedIntent = getIntent();
            BLEDeviceName = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
            BLEAddress = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
            if(BLEDeviceName.equals("Sample Earbuds") || BLEAddress.equals("12:34:56:78")){
                //if sample device -> run this :)
                setContentView(R.layout.data_screen);
                setupCarousel();
                assignListeners();
                setupView();

                Intent gattServiceIntent = new Intent(this, BLEConnectionService.class);
                gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
                gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
                startService(gattServiceIntent);
                setupServiceEventReceiver();
            }
            else{
                //enable the gatt service
                Intent gattServiceIntent = new Intent(this, BLEConnectionService.class);
                gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
                gattServiceIntent.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
                startService(gattServiceIntent);
                setupServiceEventReceiver();
            }
        }
        else {
            setContentView(R.layout.data_screen);
            setupServiceEventReceiver();
            assignListeners();
            setupView();

            //because of the running service, request some parameters from the service.
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES));
        }

        //request status
        sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_RECORDING));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT));
        sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_DISCONNECT));
        unregisterReceiver(serviceUpdateReceiver);
    }
    
    @Override
    public void onResume(){
        super.onResume();
        if(isConnected){
            setupCarousel();
        }
    }

    private void initHeartbeatElements(){
        //check if the service is running:
        if(isBLEServiceRunning(BLEHeartRateService.class)){
            createHeartbeatFragment();
        }
    }

    private void setHeartbeatFragmentName(String name){
        if(heartBeatFragment != null) {
            ((HeartBeat)heartBeatFragment).setText(name);
        }
    }

    private void setBattery(int batteryLevel){
        if(heartBeatFragment != null) {
            ((HeartBeat)heartBeatFragment).setBattery(batteryLevel);
        }
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

        sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_NAME));
        sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_REQUEST_BATTERY));
    }

    private void setupServiceEventReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_SPEED);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_DISTANCE);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_X);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_Y);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_Z);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_STEPS);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_CONTACT_TIME);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_MIN);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_MAX);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_AVERAGE);
        filter.addAction(BLEConnectionService.DOPPLE_ACTION_SET_TIMER);
        filter.addAction(BLEConnectionService.DOPPLE_EVENT_EARBUDS_CONNECTED);
        filter.addAction(BLEConnectionService.DOPPLE_EVENT_EARBUDS_DISCONNECTED);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS);
        filter.addAction(BLEConnectionService.DOPPLE_SERVICE_EVENT_SET_DEVICE_VALUES);
        filter.addAction(BLEConnectionService.DOPPLE_EVENT_SET_LAP_TIME);
        filter.addAction(BLEConnectionService.DOPPLE_EVENT_SET_LAPS);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_BATTERY);
        filter.addAction(BLEHeartRateService.DOPPLE_ACTION_SET_MONITOR_NAME);
        registerReceiver(serviceUpdateReceiver, filter);
    }

    /**
     * Function that connects all listener events to the UI items
     */
    private void assignListeners(){
        //back button
        ImageView btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

        sessionBtn = findViewById(R.id.sessionBtn);
        sessionBtnLabel = findViewById(R.id.lblSessionControl);
        lapActualTimeLabel = findViewById(R.id.lapActualTime);
        lapTimeLabel = findViewById(R.id.lapTime);
        sessionBtnImage = findViewById(R.id.sessionBtnLogo);
        sessionBtn.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view){
                startRecordingSession();
            }
        });

        RelativeLayout lapBtn = findViewById(R.id.rondeBtn);
        lapBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_NEW_LAP));
                //addLapItem();

            }
        });
    }

    /**
     * Function that fills in all the labels in the view
     */
    private void setupView(){
        initHeartbeatElements();
        TextView deviceName = findViewById(R.id.deviceName);
        deviceName.setText(BLEDeviceName);
        setX(0);
        setY(0);
        setZ(0);
        setSpeed(0);
        setDistance(0);
        setSteps(0);

        findViewById(R.id.viewSessionButton).setOnClickListener(view -> {
            Intent sessionScreen = new Intent(getApplicationContext(), SessionsActivity.class);
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
            startActivity(sessionScreen);
        });

        setHeartRate("0");
        setContactTime(0);
        setMinStepFrequency(0);
        setMaxStepFrequency(0);
        setStepFrequency(0);
        setAverageStepFrequency(0);

        setLapTime("00:00:00");
        setActualLapTime("00:00:00.000");
    }

    /**
     * Function that updates the text of the lap button in the UI.
     */
    private void setLapText(final String lapText){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView lapButtonView;
                lapButtonView = findViewById(R.id.lblSessionControl1);
                lapButtonView.setText(lapText);

            }
        });
    }

    /**
     * Function that updates the current lap screen
     * @param laps List with the lap times.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setLapScreen(List<String> laps){
        laps.sort((o1, o2) -> {
            Time time1 = Time.valueOf(o1);
            Time time2 = Time.valueOf(o2);
           return time1.compareTo(time2);
        });

        int currentLap = 0;
        ViewGroup lapList = findViewById(R.id.lapList);
        lapList.removeAllViews();
        String lastEndTime = "00:00:00";
        for(String lap: laps){
            DoppleLog.d(TAG, "lap: " + lap);
            currentLap++;



            String endTime = lap;
            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));


            DateFormat format = new SimpleDateFormat("HH:mm:ss");
            Date date1 = null;
            Date date2 = null;
            try{
                date1 = format.parse(lastEndTime);
                date2 = format.parse(endTime);
            }
            catch(Exception e){
                DoppleLog.e(TAG, e.getMessage());
            }
            long difference = date2.getTime() - date1.getTime();
            String duration = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(difference),
                    TimeUnit.MILLISECONDS.toMinutes(difference) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(difference) % TimeUnit.MINUTES.toSeconds(1));

            //Lapnumber
            TextView lapNumber = new TextView(this);
            lapNumber.setTextSize(20);
            lapNumber.setEllipsize(TextUtils.TruncateAt.END);
            lapNumber.setText("" + currentLap);
            LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            numParams.setMargins(0, 0, 50, 0);
            numParams.width = 100;
            lapNumber.setLayoutParams(numParams);

            //Start Time
            TextView startTime = new TextView(this);
            startTime.setTextSize(20);
            startTime.setText(duration);
            LinearLayout.LayoutParams startTimeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            startTimeParams.setMargins(0, 0, 20, 0);
            startTimeParams.width = 300;
            startTime.setLayoutParams(startTimeParams);
            startTime.setGravity(Gravity.CENTER);

            //End Time
            TextView endingTime = new TextView(this);
            endingTime.setTextSize(20);
            endingTime.setText(endTime);
            LinearLayout.LayoutParams endTimeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            endTimeParams.width = 300;
            endingTime.setLayoutParams(endTimeParams);
            endingTime.setGravity(Gravity.CENTER);

            //add to wrapper
            wrapper.addView(lapNumber);
            wrapper.addView(startTime);
            wrapper.addView(endingTime);

            lapList.addView(wrapper, 0);

            lastEndTime = endTime;
        }
    }

    /**
     * Function to set the XYZ values on the UI
     */
    private void setX(final double x){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView xview;
                xview = findViewById(R.id.x_value);
                xview.setText(String.valueOf(x));
            }
        });
    }

    /**
     * Function to set the XYZ values on the UI
     */
    private void setY( final double y){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView yview;
                yview = findViewById(R.id.y_value);
                yview.setText(String.valueOf(y));
            }
        });
    }

    /**
     * Function to set the XYZ values on the UI
     */
    private void setZ(final double z){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView zview;
                zview = findViewById(R.id.z_value);
                zview.setText(String.valueOf(z));
            }
        });
    }

    /**
     * Function to setup the carousel
     */
    private void setupCarousel(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Carousel)findViewById(R.id.mainCarousel)).resizeChildren();
                ((Carousel)findViewById(R.id.mainCarousel)).selectCenter();
            }
        });
    }

    /**
     * Function to set the Speed on the UI
     * @param speed speed in KM/H
     */
    private void setSpeed(final double speed){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView speedView = findViewById(R.id.speed_value);
                speedView.setText(String.valueOf(speed));
            }
        });
    }

    /**
     * Function to set the Distance on the UI
     * @param distance distance in KM
     */
    private void setDistance(final double distance){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView distanceView = findViewById(R.id.distance_value);
                distanceView.setText(String.valueOf(distance));
            }
        });
    }

    /**
     * Function to set the Steps on the UI
     * @param steps steps as int
     */
    private void setSteps(final int steps){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.amount_steps_value);
                String stepsString = String.valueOf(steps);
                stepView.setText(padLeftZeros(stepsString, 5));
            }
        });
    }

    /**
     * Function to set the Contact Time on the UI
     * @param contactTime steps as int
     */
    private void setContactTime(final int contactTime){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.contact_value);
                String contactTimeString = String.valueOf(contactTime);
                stepView.setText(padLeftZeros(contactTimeString, 3));
            }
        });
    }

    /**
     * Function to set the Contact Time on the UI
     * @param minStepFrequency steps as int
     */
    private void setMinStepFrequency(final int minStepFrequency){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.minstepfreq_value);
                String minStepFreqString = String.valueOf(minStepFrequency);
                stepView.setText(padLeftZeros(minStepFreqString, 3));
            }
        });
    }

    /**
     * Function to set the Max Step Frequency on the UI
     * @param maxStepFrequency steps as int
     */
    private void setMaxStepFrequency(final int maxStepFrequency){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.maxstepfreq_value);
                String maxStepFreqString = String.valueOf(maxStepFrequency);
                stepView.setText(padLeftZeros(maxStepFreqString, 3));
            }
        });
    }

    /**
     * Function to set the current time of the current lap
     * @param lapTime current time of the lap
     */
    private void setLapTime(final String lapTime){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView lapView = findViewById(R.id.lapTime);
                lapView.setText(lapTime);
            }
        });
    }

    /**
     * Function to set the current time of the current lap
     * @param lapTime Overall time of the lap
     */
    private void setActualLapTime(final String lapTime){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView lapView = findViewById(R.id.lapActualTime);
                lapView.setText(lapTime);
            }
        });
    }

    /**
     * Function to set the Max Step Frequency on the UI
     * @param avgStepFrequency steps as int
     */
    private void setAverageStepFrequency(final int avgStepFrequency){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.avgstepfreq_value);
                String avgStepFreqString = String.valueOf(avgStepFrequency);
                stepView.setText(padLeftZeros(avgStepFreqString, 3));
            }
        });
    }

    /**
     * Function to set the Step Frequency on the UI
     * @param stepFrequency steps as int
     */
    private void setStepFrequency(final int stepFrequency){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.stepfreq_value);
                String stepFreqString = String.valueOf(stepFrequency);
                stepView.setText(padLeftZeros(stepFreqString, 3));
            }
        });
    }

    /**
     * Function to set the Heart Rate on the UI
     * @param heartRate steps as int
     */
    private void setHeartRate(final String heartRate){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView stepView = findViewById(R.id.heartbeat_value);
                stepView.setText(padLeftZeros(heartRate, 3));
            }
        });
        if(heartBeatFragment != null) {
            ((HeartBeat)heartBeatFragment).setBPM(padLeftZeros(heartRate, 3));
        }
    }

    /**
     * Function to set the Timer text in the UI
     * @param time
     */
    private void setTimerText(final String time){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sessionBtnLabel.setText(time);
                lapActualTimeLabel.setText(time);
                lapTimeLabel.setText("" + lastLapTime);
            }
        });
    }

    /**
     * Function that toggles the recording of Earbud data
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void startRecordingSession(){
        if(!isRecording){
            setupView();
            sessionBtnImage.setImageResource(R.drawable.stop_icon_2);
            sessionBtn.setBackgroundResource(R.drawable.red_round_btn);
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_START_RECORDING));
        }
        else{
            sessionBtnImage.setImageResource(R.drawable.play_icon);
            sessionBtn.setBackgroundResource(R.drawable.blue_round_btn);
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_STOP_RECORDING));
        }
    }

    /**
     * Callback function for the gattUpdateReceiver
     */
    private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver(){
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action){
                case BLEConnectionService.DOPPLE_SERVICE_EVENT_RECORDING_STATUS:
                    //set internal recording status
                    isRecording = intent.getBooleanExtra(BLEConnectionService.EXTRA_DATA, false);
                    if(isRecording){
                        setupView();
                        sessionBtnImage.setImageResource(R.drawable.stop_icon_2);
                        sessionBtnLabel.setText("00:00:00.000");
                        sessionBtn.setBackgroundResource(R.drawable.red_round_btn);
                    }
                    else{
                        sessionBtnImage.setImageResource(R.drawable.play_icon);
                        sessionBtnLabel.setText("Start sessie");
                        sessionBtn.setBackgroundResource(R.drawable.blue_round_btn);
                    }
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_SPEED:
                    setSpeed(intent.getDoubleExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_DISTANCE:
                    setDistance(intent.getDoubleExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_X:
                    setX(intent.getDoubleExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_Y:
                    setY(intent.getDoubleExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_Z:
                    setZ(intent.getDoubleExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_STEPS:
                    setSteps(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_CONTACT_TIME:
                    setContactTime(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ:
                    setStepFrequency(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_MIN:
                    setMinStepFrequency(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_MAX:
                    setMaxStepFrequency(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_STEP_FREQ_AVERAGE:
                    setAverageStepFrequency(intent.getIntExtra(BLEConnectionService.EXTRA_DATA, 0));
                    break;
                case BLEConnectionService.DOPPLE_ACTION_SET_TIMER:
                    setTimerText(intent.getStringExtra(BLEConnectionService.EXTRA_DATA));
                    break;
                case BLEConnectionService.DOPPLE_EVENT_EARBUDS_CONNECTED:
                    setContentView(R.layout.data_screen);
                    assignListeners();
                    setupView();
                    DataActivity.this.isConnected = true;
                    setupCarousel();
                    break;
                case BLEConnectionService.DOPPLE_EVENT_EARBUDS_DISCONNECTED:
                    Toast.makeText(DataActivity.this, "Disconnected from " + BLEDeviceName, Toast.LENGTH_SHORT).show();
                    finish();
                    DataActivity.this.isConnected = false;
                    break;
                case BLEConnectionService.DOPPLE_SERVICE_EVENT_SET_DEVICE_VALUES:
                    BLEDeviceName = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
                    BLEAddress = intent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
                    TextView deviceName = findViewById(R.id.deviceName);
                    deviceName.setText(BLEDeviceName);
                    break;
                case BLEConnectionService.DOPPLE_EVENT_SET_LAP_TIME:
                    String lapTime = intent.getStringExtra(BLEConnectionService.EXTRA_DATA);
                    setLapText(lapTime);
                    lastLapTime = lapTime;
                    break;
                case BLEConnectionService.DOPPLE_EVENT_SET_LAPS:
                    //sets the lap list
                    List<String> laps = intent.getStringArrayListExtra(BLEConnectionService.EXTRA_DATA);
                    setLapScreen(laps);
                    break;
                case BLEHeartRateService.DOPPLE_ACTION_SET_HEARTBEAT:
                   if(isConnected){
                       String heartRate = intent.getStringExtra(BLEHeartRateService.EXTRA_DATA);
                       setHeartRate(heartRate);
                   }
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

    /**
     * Function that pads zero's from the left
     * @param inputString the string that needs padding
     * @param length the length of the padding
     * @return returns a string padded on the left with zero's
     */
    private String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }
}
