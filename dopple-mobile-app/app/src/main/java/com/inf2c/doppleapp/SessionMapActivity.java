package com.inf2c.doppleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.export.DoppleFileHandler;
import com.inf2c.doppleapp.export.ExportFileType;
import com.inf2c.doppleapp.gps.GPSLocation;

import java.io.File;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SessionMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    private final static String TAG = SessionMapActivity.class.getSimpleName();

    private String BLEDeviceName = "";
    private String BLEAddress = "";
    private String FileLocation = "";
    private String FileName = "";
    private DoppleFileHandler fileHandler;
    private GoogleMap map;
    private LatLngBounds bound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_map);

        //set name, address and location
        Intent invokedIntent = getIntent();
        BLEDeviceName = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
        BLEAddress = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");
        FileLocation = invokedIntent.getStringExtra("EXTRA_DOPPLE_FILE_LOCATION");
        FileName = invokedIntent.getStringExtra("EXTRA_DOPPLE_FILE_NAME");

        fileHandler = new DoppleFileHandler(this, BLEDeviceName, BLEAddress);

        setupView();
        assignListeners();
        Date fileDate = parseTitleToTime(FileName.split("_")[2]);
        setTitles(fileDate);
        renderGoogleMap();
    }

    private Date parseTitleToTime(String title){
        Date date1 = null;
        try {
            date1 = new SimpleDateFormat("yyyyMMddHHmmss").parse(title);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date1;
    }

    private void setTitles(Date title) {
        ((TextView)findViewById(R.id.filesTitle))
                .setText(new SimpleDateFormat("d MMMM yyyy")
                        .format(title,  new StringBuffer("Sessie van "), new FieldPosition(0)));

        String time = new SimpleDateFormat("H:mm").format(title);
        int hour = Integer.parseInt(time.split(":")[0]);
        String timeOfDay;
        if(hour >= 6 && hour < 12){
            timeOfDay = "'s ochtends";
        }
        else if( hour >= 12 && hour < 17){
            timeOfDay = "'s middags";
        }
        else if (hour >= 17 && hour <= 23){
            timeOfDay = "'s avonds";
        }
        else{
            timeOfDay = "'s nachts";
        }

        ((TextView)findViewById(R.id.filesSubTitle))
                .setText(String.format("Om %s %s", time, timeOfDay));
    }

    private void renderGoogleMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setupView(){
        TextView deviceName = findViewById(R.id.deviceName);
        deviceName.setText(BLEDeviceName);

        Button shareCSV = findViewById(R.id.btnShareCSV);
        ImageView btnIconCSV = findViewById(R.id.btnIconCSV);
        Button shareTCX = findViewById(R.id.btnShareTCX);
        ImageView btnIconTCX = findViewById(R.id.btnIconTCX);

        if(!fileHandler.checkFileExsist(FileName, "csv")) {
            shareCSV.setTextColor(Color.parseColor("#BDBDBD"));
            btnIconCSV.setColorFilter(Color.parseColor("#BDBDBD"));
        } else {
            shareCSV.setOnClickListener(view -> {
                fileHandler.export(FileName + ".csv", ExportFileType.TCX);
            });
        }

        if(!fileHandler.checkFileExsist(FileName, "tcx")) {
            shareTCX.setTextColor(Color.parseColor("#BDBDBD"));
            btnIconTCX.setColorFilter(Color.parseColor("#BDBDBD"));
        } else {
            shareTCX.setOnClickListener(view -> {
                fileHandler.export(FileName + ".tcx", ExportFileType.TCX);

            });
        }
    }

    private void setTotalDistance(double distance) {
        runOnUiThread(() -> {
            TextView distance_value = findViewById(R.id.distance_value);
            distance_value.setText(String.valueOf(distance));
        });
    }

    private void setSteps(int stepCount) {
        runOnUiThread(() -> {
            TextView amount_steps_value = findViewById(R.id.amount_steps_value);
            amount_steps_value.setText(String.valueOf(stepCount));
        });
    }

    /**
     * Function that connects all listener events to the UI items
     */
    private void assignListeners(){
        //back button
        ImageView btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(view -> finish());
        findViewById(R.id.btnCenter).setOnClickListener(v -> map.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, 100), 1000, null));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapLoadedCallback(this);

        List<DoppleDataObject> dat = fileHandler.loadStepData(FileName + ".csv");
        int stepCount = 0;
        double prevEarbudsTimestamp = 0;
        double prevSteps = 0;
        for(DoppleDataObject obj: dat){
            if(prevEarbudsTimestamp != obj.EarbudsTimestamp && prevSteps != obj.EarbudsTimestamp) {
                prevEarbudsTimestamp = obj.EarbudsTimestamp;
                prevSteps = obj.Steps;
                stepCount += obj.Steps;
            }
        }
        setSteps(stepCount);
        Log.d(TAG, "Steps: " + stepCount);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapLoaded() {
        if(map != null){
            drawMapLines();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawMapLines(){
        List<GPSLocation> locations = fileHandler.loadGPSDataFromFile(FileName + ".csv");
        if(locations.size() > 0){
            List<GPSLocation> dLocations = locations.stream().distinct().collect(Collectors.toList());

            List<LatLng> longLatList = new ArrayList<>();
            for(GPSLocation loc: dLocations){
                Log.d(TAG, loc.longitude + " " + loc.latitude);
                longLatList.add(new LatLng(loc.latitude, loc.longitude));
            }
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_sjihdazi_light));
            map.addPolyline(new PolylineOptions().addAll(longLatList).width(10).color(Color.rgb(24,240,139)).jointType(JointType.ROUND));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for(LatLng ding: longLatList){
                builder.include(ding);
            }
            bound = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, 100), 1000, null);

            DoppleConversion conv = new DoppleConversion();
            this.setTotalDistance(conv.round(conv.getTotalDistanceGPSLocations(dLocations)));
        }
    }
}
