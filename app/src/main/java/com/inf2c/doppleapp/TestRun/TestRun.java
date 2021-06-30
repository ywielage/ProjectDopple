package com.inf2c.doppleapp.TestRun;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.inf2c.doppleapp.R;


import com.inf2c.doppleapp.SessionsActivity;
import com.inf2c.doppleapp.ble.BLEConnectionService;
import com.inf2c.doppleapp.carousel.Carousel;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;
import com.inf2c.doppleapp.heart_rate.HeartBeat;
import com.inf2c.doppleapp.logging.DoppleLog;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TestRun extends AppCompatActivity {

    private final static String TAG = TestRun.class.getSimpleName();

    //connection variables
    private String BLEDeviceName = "";
    private String BLEAddress = "";

    //dopple service control
    private boolean isRecording = false;
    private boolean isConnected = false;

    private TextView durationTv;
    private RelativeLayout testSessionBtn;
    private TextView testSessionBtnLabel;
    private TextView testLapTime;
    private TextView testLapActualTimeLabel;

    private ImageView testSessionBtnImage;

    private TextView distanceValueTV;
    private TextView speedValueTV;
    private TextView heartbeatValue;
    private TextView contactTimeValue;
    private TextView flightTimeValue;
    private TextView dutyFactorValue;

    private TextView stepCountValue;
    private TextView stepMinFreqValue;
    private TextView stepMaxFreqValue;
    private TextView stepAvgFreqValue;
    private Fragment heartBeatFragment;

    private Spinner selectDataSpinner;

    private List<Trackpoint> list;

    private Button submitGraphLimitsBtn;
    private EditText graphStartLimitEt;
    private EditText graphEndLimitEt;
    private EditText graphTargetET;

    private GraphView graphData;
    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> targetSeries;
    private boolean initialGraph;
    private double intervalGraph;

    private String lastLapTime = "00:00:00";


    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        if(!isBLEServiceRunning(BLEConnectionService.class)){
            setContentView(R.layout.loading_screen);

            //set name and address
            Intent invokedIntent = getIntent();
            BLEDeviceName = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
            BLEAddress = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
            if(BLEDeviceName.equals("Sample Earbuds") || BLEAddress.equals("12:34:56:78")){
                //if sample device -> run this :)
                setContentView(R.layout.test_run);
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
            setContentView(R.layout.test_run);
            setupServiceEventReceiver();
            assignListeners();
            setupView();

            //because of the running service, request some parameters from the service.
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DEVICE_VALUES));
        }

        //request status
        sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_RECORDING));


        InputStream object = this.getResources().openRawResource(R.raw.dopple_session_20210511164705_1);
        TestXMLParser parser = new TestXMLParser();
        this.list = parser.parse(object);

        //Get field ids
        durationTv = (TextView) findViewById(R.id.durationTv);
        testSessionBtn = (RelativeLayout) findViewById(R.id.testStartSessionBtn);
        testLapTime = (TextView) findViewById(R.id.testLapTime);
        testSessionBtnImage  = findViewById(R.id.testSessionBtnLogo);
        testSessionBtnLabel = findViewById(R.id.lblTestSessionControl);

        distanceValueTV = (TextView) findViewById(R.id.distance_value);
        speedValueTV = (TextView) findViewById(R.id.speed_value);
        contactTimeValue = (TextView) findViewById(R.id.contactTimeValue);
        flightTimeValue = (TextView) findViewById(R.id.flightTime_value);
        dutyFactorValue = (TextView) findViewById(R.id.dutyFactor_value);

        stepCountValue = (TextView) findViewById(R.id.TestAmount_steps_value);
        stepMinFreqValue = (TextView) findViewById(R.id.Testminstepfreq_value);
        stepMaxFreqValue = (TextView) findViewById(R.id.TestMaxstepfreq_value);
        stepAvgFreqValue = (TextView) findViewById(R.id.TestAvgstepfreq_value);

        selectDataSpinner = findViewById(R.id.selectDataSpinner);

        String[] items = new String[]{"Step frequency", "Contact time", "Flight time", "Duty factor"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, items);
        selectDataSpinner.setAdapter(adapter);
        initialGraph = true;
        try {
            giveFeedback("Step frequency");
            createGraph(0,300, "Step frequency");
        } catch (ParseException e) {
            e.printStackTrace();
        }


//        graphData.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent popupWindow = new Intent(getApplicationContext(), PopupGraph.class);
////                popupWindow.putExtra("data", (Serializable) list);
//                graphStartLimitEt = (EditText) findViewById(R.id.graphStartLimitEt);
//                popupWindow.putExtra("min", graphStartLimitEt.toString());
//                graphEndLimitEt = (EditText) findViewById(R.id.graphEndLimitEt);
//                popupWindow.putExtra("max", graphEndLimitEt.toString());
//                startActivity(popupWindow);
//            }
//        });


        submitGraphLimitsBtn = (Button) findViewById(R.id.submitGraphLimitsBtn);


        submitGraphLimitsBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                initialGraph = false;
                graphStartLimitEt = (EditText) findViewById(R.id.graphStartLimitEt);
                graphEndLimitEt = (EditText) findViewById(R.id.graphEndLimitEt);
                graphTargetET = (EditText) findViewById(R.id.graphInvervalET);
                float targetFloat = Float.parseFloat(String.valueOf(graphTargetET.getText()));


                if(graphStartLimitEt.getText().length() > 0 || graphEndLimitEt.getText().length() > 0 ) {
                    try {
                        createGraph(Integer.parseInt(String.valueOf(graphStartLimitEt.getText())), Integer.parseInt(String.valueOf(graphEndLimitEt.getText())), selectDataSpinner.getSelectedItem().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        //Set field values
        Time timeRan = Calculations.getTimeRan(list);
        double totalDistance = Math.round(Calculations.getTotalDistance(list) * 100.0) / 100.0;
        StepFreqs stepsFreq = Calculations.getStepFreqs(list);

//        durationTv.setText(timeRan.toString());
//        distanceValueTV.setText(Double.toString(totalDistance));
//        speedValueTV.setText(Calculations.getSpeed(timeRan, totalDistance));
//        heartbeatValue.setText(Integer.toString(Calculations.getAverageHeartRateBpm(list)));
//        contactTimeValue.setText(Integer.toString(Calculations.getAverageContactTime(list)));
//        flightTimeValue.setText(Long.toString(Calculations.getAverageFlightTime(list)));
//        dutyFactorValue.setText(Double.toString(Calculations.getAverageDutyFactor(list)));
//
//        stepCountValue.setText(Integer.toString(Calculations.getTotalStepCount(list)));
//        stepMinFreqValue.setText(Integer.toString(stepsFreq.getMinStepFreq()));
//        stepMaxFreqValue.setText(Integer.toString(stepsFreq.getMaxStepFreq()));
//        stepAvgFreqValue.setText(Integer.toString(stepsFreq.getAvgStepFreq()));

        testSessionBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override

            public void onClick(View view) {
                startRecordingSession();
//                durationTv.setText(getSessionDataCSV(3, 0));

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void giveFeedback(String data)
    {
        int goalMinimum = 130;
        int goalMaximum = 160;

        float underMinimum = 0;
        float overMaximum = 0;

        double stat;

        for(int i = 0; i < this.list.size(); i++) {
            switch (data) {
                case "Contact time":
                    stat = this.list.get(i).getContactTime();
                    break;
//                case "Flight time":
//                    stat = Calculations.getFlightTime(Math.toIntExact(x), this.list.get(i).getContactTime(), this.list.get(i).getSteps()); // TODO
//                    break;
//                case "Duty factor":
//                    int flighttime = Calculations.getFlightTime(Math.toIntExact(x),this.list.get(i).getContactTime(), this.list.get(i).getSteps());
//                    stat = Calculations.getDutyFactor((this.list.get(i).getContactTime()), flighttime);
//                    break;
                default:
                    stat = this.list.get(i).getStepFrequency();
                    break;
            }
            if(stat < goalMinimum)
            {
                underMinimum++;
            }
            else if(stat > goalMaximum)
            {
                overMaximum++;
            }
        }

        float underMinimumPercent = Math.round(underMinimum / list.size() * 10000f) / 100f;
        float overMaximumPercent = Math.round(overMaximum / list.size() * 10000f) / 100f;

        System.out.println("You're under your minimum " + underMinimumPercent + "% of the time");
        System.out.println("You're over your maximum " + overMaximumPercent + "% of the time");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createGraph(int startSecond, int endSecond, String data) throws ParseException {

        graphData = (GraphView) findViewById(R.id.graphData);
        series = new LineGraphSeries<DataPoint>();
        targetSeries = new LineGraphSeries<DataPoint>();
        GridLabelRenderer gridLabelRenderer = graphData.getGridLabelRenderer();
        gridLabelRenderer.setHorizontalAxisTitle("Time");
        gridLabelRenderer.setVerticalAxisTitle(data);

        if(graphData.getSeries().size() > 0) {
            graphData.removeAllSeries();
        }


        long x;
        double y = 0.0;
        Date startTime = null;
        double intervalImplementCount = 0.0;
        intervalGraph = 10.0;
        for(int i = 0; i<this.list.size();i++) {
            String[] dateSplit = new Date(this.list.get(i).getTime()).toString().split(" ");
            String[] timeSplit = dateSplit[3].split(":");

            String time = timeSplit[0] + "-" + timeSplit[1] + "-" + timeSplit[2];
            Date currTime = new SimpleDateFormat("hh-mm-ss").parse(time);


            if(i == 0) {
                startTime = new SimpleDateFormat("hh-mm-ss").parse(time);
                x = 0;
            }
            else {
                long diff = currTime.getTime() - startTime.getTime();
                x = diff / 1000; // TODO %60
            }

            switch (data) {
                case "Step frequency":
                    y = this.list.get(i).getStepFrequency();
                    break;
                case "Contact time":
                    y = this.list.get(i).getContactTime();
                    break;
                case "Flight time":
                    y = Calculations.getFlightTime(Math.toIntExact(x),this.list.get(i).getContactTime(), this.list.get(i).getSteps()); // TODO
                    break;
                case "Duty factor":
                    int flighttime = Calculations.getFlightTime(Math.toIntExact(x),this.list.get(i).getContactTime(), this.list.get(i).getSteps());
                    y = Calculations.getDutyFactor((this.list.get(i).getContactTime()), flighttime);
                    break;
            }
            intervalImplementCount++;
            if(intervalImplementCount == intervalGraph){
                series.appendData(new DataPoint(x, y), true, list.size());
                if(!initialGraph){
                    long graphTarget = Long.parseLong(String.valueOf(graphTargetET.getText()));
                    targetSeries.appendData(new DataPoint(x, graphTarget), true ,list.size());
                }
                intervalImplementCount = 0;
            }
        }


        graphData.getViewport().setMinX(startSecond);
        graphData.getViewport().setMaxX(endSecond);
        graphData.getViewport().setXAxisBoundsManual(true);
        if(!initialGraph){
            targetSeries.setColor(Color.GREEN);
            graphData.addSeries(targetSeries);
        }
        graphData.addSeries(series);
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
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_REQUEST_DISCONNECT));
        sendBroadcast(new Intent(BLEHeartRateService.DOPPLE_SERVICE_EVENT_DISCONNECT));
        unregisterReceiver(serviceUpdateReceiver);
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

    private void assignListeners(){
        //back button
        ImageView btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });

        testSessionBtn = findViewById(R.id.testStartSessionBtn);
        testSessionBtnLabel = findViewById(R.id.lblTestSessionControl);
        testLapActualTimeLabel = findViewById(R.id.testLapActualTime);
        testLapTime = findViewById(R.id.testLapTime);
        testSessionBtnImage = findViewById(R.id.testSessionBtnLogo);

        testSessionBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override

            public void onClick(View view) {
                startRecordingSession();
                //durationTv.setText(getSessionDataCSV(3, 0));

            }
        });


    }

    private void setupView(){
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

        setContactTime(0);
        setMinStepFrequency(0);
        setMaxStepFrequency(0);
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
                xview = findViewById(R.id.TestX_value);
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
                yview = findViewById(R.id.TestY_value);
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
                zview = findViewById(R.id.TestZ_value);
                zview.setText(String.valueOf(z));
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
                TextView stepView = findViewById(R.id.TestAmount_steps_value);
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
                TextView stepView = findViewById(R.id.contactTimeValue);
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
                TextView stepView = findViewById(R.id.Testminstepfreq_value);
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
                TextView stepView = findViewById(R.id.TestMaxstepfreq_value);
                String maxStepFreqString = String.valueOf(maxStepFrequency);
                stepView.setText(padLeftZeros(maxStepFreqString, 3));
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
                TextView stepView = findViewById(R.id.TestAvgstepfreq_value);
                String avgStepFreqString = String.valueOf(avgStepFrequency);
                stepView.setText(padLeftZeros(avgStepFreqString, 3));
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
                TextView lapView = findViewById(R.id.testLapTime);
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
                TextView lapView = findViewById(R.id.testLapActualTime);
                lapView.setText(lapTime);
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

    public String getSessionDataCSV(int line, int element) { // Element = DeviceTimestamp,EarbudsTimestamp,X,Y,Z,CNT,Steps,Step_frequency,Contact_time,Long,Lat (0 - 10)
        ArrayList data = new ArrayList();
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.dopplesession)));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null)
                data.add(nextLine);
            } catch (IOException e) {
            e.printStackTrace();
        }
        String [] result = (String[]) data.get(line);
        return result[element];
    }

    private void setTimerText(final String time){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testSessionBtnLabel.setText(time);
                //lapActualTimeLabel.setText(time);
                //lapTimeLabel.setText("" + lastLapTime);
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
            testSessionBtnImage.setImageResource(R.drawable.stop_icon_2);
            testSessionBtn.setBackgroundResource(R.drawable.red_round_btn);
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_START_RECORDING));
        }
        else{
            testSessionBtnImage.setImageResource(R.drawable.play_icon);
            testSessionBtn.setBackgroundResource(R.drawable.blue_round_btn);
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_STOP_RECORDING));
        }
    }

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
                        testSessionBtnImage.setImageResource(R.drawable.stop_icon_2);
                        testSessionBtnLabel.setText("00:00:00.000");
                        testSessionBtn.setBackgroundResource(R.drawable.red_round_btn);
                    }
                    else{
                        testSessionBtnImage.setImageResource(R.drawable.play_icon);
                        testSessionBtnLabel.setText("Start sessie");
                        testSessionBtn.setBackgroundResource(R.drawable.blue_round_btn);
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
                    setContentView(R.layout.test_run);
                    assignListeners();
                    setupView();
                    TestRun.this.isConnected = true;
                    setupCarousel();
                    break;
                case BLEConnectionService.DOPPLE_EVENT_EARBUDS_DISCONNECTED:
                    Toast.makeText(TestRun.this, "Disconnected from " + BLEDeviceName, Toast.LENGTH_SHORT).show();
                    finish();
                    TestRun.this.isConnected = false;
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

    /*
    private void setupView(){

        durationTv.setText("00:00:00");
    }
     */



}

