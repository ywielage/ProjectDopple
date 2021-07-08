package com.inf2c.doppleapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.inf2c.doppleapp.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.Serializable;

public class PopupGraph extends Activity {

    private GraphView graphDataZoomed;
    private LineGraphSeries<DataPoint> series;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_window_graph);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        double width = dm.widthPixels;
        double height = dm.heightPixels;

        getWindow().setLayout((int) (width*0.8), (int) (height*0.8));

        graphDataZoomed = (GraphView) findViewById(R.id.graphDataZoomed);
        series = new LineGraphSeries<DataPoint>();
        series.appendData(new DataPoint(3, 5), true ,5);
        series.appendData(new DataPoint(4, 7), true ,5);
        graphDataZoomed.addSeries(series);
    }

}
