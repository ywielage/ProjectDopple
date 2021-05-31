package com.inf2c.doppleapp.TestRun;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inf2c.doppleapp.R;


import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;

public class TestRun extends AppCompatActivity {

    TextView actualTimeTv;
    RelativeLayout testSessionBtn;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        actualTimeTv = (TextView) findViewById(R.id.TvActualTime);

        actualTimeTv.setText("Dit is een test");

        actualTimeTv.setText("Test");

        testSessionBtn = (RelativeLayout) findViewById(R.id.testSessionBtn);

        testSessionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actualTimeTv.setText(getSessionDataCSV());
            }
        });
    }

    public String getSessionDataCSV() {
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.dopplesession)));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null)
                return nextLine[1];
            } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

