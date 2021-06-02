package com.inf2c.doppleapp.TestRun;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.inf2c.doppleapp.DataActivity;
import com.inf2c.doppleapp.R;


import com.inf2c.doppleapp.SessionsActivity;
import com.inf2c.doppleapp.ble.BLEConnectionService;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;
import com.inf2c.doppleapp.heart_rate.HeartBeat;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TestRun extends AppCompatActivity {

    private TextView durationTv;
    private RelativeLayout testSessionBtn;
    private TextView testLapTime;
    private boolean isRecording = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        durationTv = (TextView) findViewById(R.id.durationTv);
        testSessionBtn = (RelativeLayout) findViewById(R.id.testStartSessionBtn);
        testLapTime = (TextView) findViewById(R.id.testLapTime);


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

