package com.inf2c.doppleapp.TestRun;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inf2c.doppleapp.R;


import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.FileReader;

public class TestRun extends AppCompatActivity {

    TextView actualTimeTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        actualTimeTv = (TextView) findViewById(R.id.TvActualTime);

        actualTimeTv.setText("Dit is een test");

        actualTimeTv.setText(getSessionDataCSV());
    }

    public StringBuffer getSessionDataCSV() {
        StringBuffer data = null;
        try {
            CSVReader reader = new CSVReader(new FileReader(String.valueOf(R.raw.dopplesession)));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                data.append(nextLine[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(data != null) {
            return data;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append("Cannot read data");
            return sb;
        }
    }
}
