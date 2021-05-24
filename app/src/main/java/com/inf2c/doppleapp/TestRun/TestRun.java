package com.inf2c.doppleapp.TestRun;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inf2c.doppleapp.R;

public class TestRun extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_run);

        TextView actualTimeTv = (TextView) findViewById(R.id.TvActualTime);


        actualTimeTv.setText("Dit is een test");
    }
}
