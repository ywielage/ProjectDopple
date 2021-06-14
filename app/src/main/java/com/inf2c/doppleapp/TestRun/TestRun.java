package com.inf2c.doppleapp.TestRun;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.inf2c.doppleapp.R;


import com.inf2c.doppleapp.ble.BLEConnectionService;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestRun extends AppCompatActivity {

    private TextView durationTv;
    private RelativeLayout testSessionBtn;
    private TextView testLapTime;
    private boolean isRecording = false;

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

    private GraphView graphData;
    private LineGraphSeries<DataPoint> series;


    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        InputStream object = this.getResources().openRawResource(R.raw.dopple_session_20210511164705_1);
        TestXMLParser parser = new TestXMLParser();
        List<Trackpoint> list = parser.parse(object);

        //Get field ids
        durationTv = (TextView) findViewById(R.id.durationTv);
        testSessionBtn = (RelativeLayout) findViewById(R.id.testStartSessionBtn);
        testLapTime = (TextView) findViewById(R.id.testLapTime);

        distanceValueTV = (TextView) findViewById(R.id.distance_value);
        speedValueTV = (TextView) findViewById(R.id.speed_value);
        heartbeatValue = (TextView) findViewById(R.id.heartBeatValue);
        contactTimeValue = (TextView) findViewById(R.id.contactTimeValue);
        flightTimeValue = (TextView) findViewById(R.id.flightTime_value);
        dutyFactorValue = (TextView) findViewById(R.id.dutyFactor_value);

        stepCountValue = (TextView) findViewById(R.id.TestAmount_steps_value);
        stepMinFreqValue = (TextView) findViewById(R.id.Testminstepfreq_value);
        stepMaxFreqValue = (TextView) findViewById(R.id.TestMaxstepfreq_value);
        stepAvgFreqValue = (TextView) findViewById(R.id.TestAvgstepfreq_value);

        graphData = (GraphView) findViewById(R.id.graphData);
        series = new LineGraphSeries<DataPoint>();
        GridLabelRenderer gridLabelRenderer = graphData.getGridLabelRenderer();
        gridLabelRenderer.setHorizontalAxisTitle("Minutes");
        gridLabelRenderer.setVerticalAxisTitle(new Date(list.get(0).getTime()).toString().split(" ")[3]);

        long x;
        int y;
        for(int i = 0; i<list.size();i++) {
            String[] dateSplit = new Date(list.get(i).getTime()).toString().split(" ");
            String[] timeSplit = dateSplit[3].split(":");
            String timehours = timeSplit[0], timeMinutes = timeSplit[1], timeSeconds = timeSplit[2];
            System.out.println(timeMinutes);
            x = Long.parseLong(timehours);
            y = list.get(i).getStepFrequency();
            series.appendData(new DataPoint(x, y), true, list.size());
        }
        graphData.addSeries(series);

        list.get(9).getTime();
        //Set field values
        Time timeRan = Calculations.getTimeRan(list);
        double totalDistance = Math.round(Calculations.getTotalDistance(list) * 100.0) / 100.0;
        StepFreqs stepsFreq = Calculations.getStepFreqs(list);

        durationTv.setText(timeRan.toString());
        distanceValueTV.setText(Double.toString(totalDistance));
        speedValueTV.setText(Calculations.getSpeed(timeRan, totalDistance));
        heartbeatValue.setText(Integer.toString(Calculations.getAverageHeartRateBpm(list)));
        contactTimeValue.setText(Integer.toString(Calculations.getAverageContactTime(list)));
        flightTimeValue.setText(Long.toString(Calculations.getAverageFlightTime(list)));
        dutyFactorValue.setText(Double.toString(Calculations.getAverageDutyFactor(list)));

        stepCountValue.setText(Integer.toString(Calculations.getTotalStepCount(list)));
        stepMinFreqValue.setText(Integer.toString(stepsFreq.getMinStepFreq()));
        stepMaxFreqValue.setText(Integer.toString(stepsFreq.getMaxStepFreq()));
        stepAvgFreqValue.setText(Integer.toString(stepsFreq.getAvgStepFreq()));

        testSessionBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                startRecordingSession();
//                durationTv.setText(getSessionDataCSV(3, 0));

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

    /**
     * Function that toggles the recording of Earbud data
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startRecordingSession(){
        if(!isRecording){
            setupView();
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_START_RECORDING));
        }
        else{
            sendBroadcast(new Intent(BLEConnectionService.DOPPLE_SERVICE_EVENT_STOP_RECORDING));
        }
    }

    private void setupView(){
        durationTv.setText("00:00:00");
    }


}

