package com.inf2c.doppleapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.inf2c.doppleapp.TestRun.Calculations;
import com.inf2c.doppleapp.TestRun.TestXMLParser;
import com.inf2c.doppleapp.TestRun.Trackpoint;
import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.export.DoppleFileHandler;
import com.inf2c.doppleapp.export.ExportFileType;
import com.inf2c.doppleapp.gps.GPSLocation;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private GraphView graphData;
    private List<Trackpoint> list;
    private boolean initialGraph;
    private Button submitGraphLimitsBtn;
    private EditText graphStartLimitEt;
    private EditText graphEndLimitEt;
    private EditText graphTargetET;
    private EditText graphIntervalET;
    private Spinner selectDataSpinner;


    @RequiresApi(api = Build.VERSION_CODES.O)
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


        selectDataSpinner = findViewById(R.id.selectDataSpinner);
        String[] items = new String[]{"Step frequency", "Contact time", "Flight time", "Duty factor"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, items);
        selectDataSpinner.setAdapter(adapter);

        fileHandler = new DoppleFileHandler(this, BLEDeviceName, BLEAddress);

        try {
            setXML();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        initialGraph = true;
        try {
            setFeedback(130, "Step frequency");
            createGraph(0,300, "Step frequency");
        } catch (ParseException e) {
            e.printStackTrace();
        }

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

    private void setXML() throws FileNotFoundException {
        //TODO Change this line to take the currently selected file
        File pathObject = new File(this.getExternalFilesDir(null), "RecordedSessions");
        File fileToShare = new File(pathObject, FileName + ".tcx");
        FileInputStream fis = new FileInputStream(fileToShare);
        TestXMLParser parser = new TestXMLParser();
        this.list = parser.parse(fis);
    }

    private void setFeedback(int targetAverage, String data)
    {
        float underMinimum = 0;
        float overMaximum = 0;
        int goalMinimum = targetAverage - (targetAverage / 20);
        int goalMaximum = targetAverage + (targetAverage / 20);
        String nlData = "";

        TextView feedbackValue = findViewById(R.id.feedback_value);

        double stat;

        for(int i = 0; i < list.size(); i++) {
            switch (data) {
                case "Contact time":
                    stat = list.get(i).getContactTime();
                    nlData = "contacttijd";
                    break;
                default:
                    stat = list.get(i).getStepFrequency();
                    nlData = "stapfrequentie";
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

        feedbackValue.setText("");
        feedbackValue.append(String.format("Onder minimum %s%% van de tijd", underMinimumPercent));
        feedbackValue.append(String.format("\nBoven maximum %s%% van de tijd", overMaximumPercent));
        feedbackValue.append(getFeedbackString(underMinimumPercent, overMaximumPercent, nlData));
    }

    private String getFeedbackString(float underMinimumPercent, float overMaximumPercent, String nlData)
    {
        if(underMinimumPercent > 15 && overMaximumPercent > 15)
        {
            return "\nProbeer een meer regelmatige stapfrequentie te krijgen";
        }
        else if(underMinimumPercent > overMaximumPercent)
        {
            if(underMinimumPercent > 10 && underMinimumPercent <= 20)
            {
                return String.format("\nProbeer je %s te verhogen", nlData);
            }
            else if(underMinimumPercent > 20)
            {
                return String.format("\nProbeer je %s regelmatig te verhogen", nlData);
            }
        }
        else if(underMinimumPercent < overMaximumPercent)
        {
            if(overMaximumPercent > 10 && overMaximumPercent <= 20)
            {
                return String.format("\nProbeer je %s te verlagen", nlData);
            }
            else if(overMaximumPercent > 20)
            {
                return String.format("\nProbeer je %s regelmatig te verlagen", nlData);
            }
        }
        else
        {
            return String.format("\nJe zit op een juiste %s", nlData);
        }
        return  "";
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createGraph(int startSecond, int endSecond, String data) throws ParseException {

        graphData = (GraphView) findViewById(R.id.graphData);
        graphTargetET = (EditText) findViewById(R.id.graphTargetET);
        graphIntervalET = (EditText) findViewById(R.id.graphIntervalET);

        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> targetSeries = new LineGraphSeries<DataPoint>();
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
        double intervalGraph = 2.0;

        if(!graphIntervalET.getText().toString().equals("")){
            intervalGraph = Double.parseDouble(String.valueOf(graphIntervalET.getText()));
            intervalGraph += 1;
        }

        ArrayList<Double> intervalList = new ArrayList<Double>();
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
                Double avg = intervalList.stream().mapToDouble(val -> val).average().orElse(0.0);
                series.appendData(new DataPoint(x, avg), true, list.size());
                intervalImplementCount = 0.0;
                intervalList.clear();
            }
            else if(intervalImplementCount < intervalGraph){
                intervalList.add(y);
            }
            if(!initialGraph){
                if(!graphTargetET.getText().toString().equals("")){
                    long graphTarget = Long.parseLong(String.valueOf(graphTargetET.getText()));
                    targetSeries.appendData(new DataPoint(x, graphTarget), true ,list.size());
                }
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

    /**
     * Function that connects all listener events to the UI items
     */
    private void assignListeners(){
        //back button
        ImageView btnBack = findViewById(R.id.backButton);
        btnBack.setOnClickListener(view -> finish());
        findViewById(R.id.btnCenter).setOnClickListener(v -> map.animateCamera(CameraUpdateFactory.newLatLngBounds(bound, 100), 1000, null));
        submitGraphLimitsBtn = (Button) findViewById(R.id.submitGraphLimitsBtn);


        submitGraphLimitsBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                initialGraph = false;

                graphStartLimitEt = (EditText) findViewById(R.id.graphStartLimitEt);
                graphEndLimitEt = (EditText) findViewById(R.id.graphEndLimitEt);
                graphIntervalET = (EditText) findViewById(R.id.graphIntervalET);
                graphTargetET = (EditText) findViewById(R.id.graphTargetET);

                if(graphStartLimitEt.getText().toString().matches("") || graphEndLimitEt.getText().toString().matches("") || graphIntervalET.getText().toString().matches("") || graphTargetET.getText().toString().matches("")){
                    Toast toast=Toast.makeText(getApplicationContext(),"Vul alle gegevens in!",Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                if(Float.parseFloat(graphStartLimitEt.getText().toString()) >= Float.parseFloat(graphEndLimitEt.getText().toString())){
                    Toast toast=Toast.makeText(getApplicationContext(),"Het begin van de grafiek kan niet starten na het einde! Verander het start getal, of het einde getal!",Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                float targetFloat = Float.parseFloat(String.valueOf(graphTargetET.getText()));

                    try {
                        setFeedback(Integer.parseInt(String.valueOf(graphTargetET.getText())), String.valueOf(selectDataSpinner.getSelectedItem().toString()));
                        createGraph(Integer.parseInt(String.valueOf(graphStartLimitEt.getText())), Integer.parseInt(String.valueOf(graphEndLimitEt.getText())), selectDataSpinner.getSelectedItem().toString());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
            }
        });
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
