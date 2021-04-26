package com.inf2c.doppleapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inf2c.doppleapp.export.DoppleFileHandler;
import com.inf2c.doppleapp.export.ExportFileAdapter;
import com.inf2c.doppleapp.export.ExportFileObject;
import com.inf2c.doppleapp.gestures.SessionsSwipeGestureDetector;
import com.inf2c.doppleapp.map.SessionListCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SessionsActivity extends AppCompatActivity {
    private final static String TAG = SessionsActivity.class.getSimpleName();
    private DoppleFileHandler fileHandler;

    private String BLEDeviceName = "";
    private String BLEAddress = "";

    class SortByTimeStamp implements Comparator<ExportFileObject>
    {
        @Override
        public int compare(ExportFileObject a, ExportFileObject b)
        {
            return a.FileDate.compareTo(b.FileDate);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_screen);

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},0);

        //set name and address
        Intent invokedIntent = getIntent();
        BLEDeviceName = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_NAME");
        BLEAddress = invokedIntent.getStringExtra("EXTRA_DOPPLE_DEVICE_ADDRESS");

        fileHandler = new DoppleFileHandler(this, BLEDeviceName, BLEAddress);

        initSwipe(); //disable if you don't want to allow down swiping.
        assignListeners(); //assigns the buttons on the view
        setupView(); //fills in labels in the view
        loadFileData(); //populates the list with files on the device
        setUUIDTextField(); //sets the UUID
    }

    /**
     * Sets the text in the uuid text field to the uuid from the phone
     */
    private void setUUIDTextField() {
        TextView textField = (TextView)findViewById(R.id.uuidTextField);
        textField.setText("ID: " + getUUID());
    }

    /**
     * Gets the uuid from the storage
     * @return the uuid
     */
    private String getUUID() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String id = settings.getString("UUID", "0");
        return id;
    }

    private void setSampleData(){
        ExportFileObject ob = new ExportFileObject();
        ob.FileDate = "01/04/2020 12:00";
        ob.FileName = "FirstRun.csv";


        List<ExportFileObject> obList = new ArrayList<>();
        obList.add(ob);
        obList.add(ob);
        obList.add(ob);
        obList.add(ob);
        obList.add(ob);

        ExportFileAdapter adapter = new ExportFileAdapter(this, obList, fileHandler);
        ListView lv = findViewById(R.id.sessionList);
        lv.setAdapter(adapter);
    }

    private void loadFileData(){
        List<ExportFileObject> files = fileHandler.loadFileList();
        Collections.sort(files, new SortByTimeStamp());
        List<String> fileNames = new ArrayList<>();
        List<ExportFileObject> toRemove = new ArrayList<>();
        for(ExportFileObject obj: files){
            String fileName = obj.FileName.split("\\.")[0];
            String fileLocation = obj.FileLocation.substring(0, obj.FileLocation.length() - 4);
            if(!fileNames.contains(fileName)){
                fileNames.add(fileName);
                obj.FileName = fileName;
                obj.FileLocation = fileLocation;
            }
            else{
                toRemove.add(obj);
            }
        }
        files.removeAll(toRemove);

        ExportFileAdapter adapter = new ExportFileAdapter(this, files, fileHandler);
        adapter.addListener(sessionListCallback);
        ListView lv = findViewById(R.id.sessionList);
        lv.setAdapter(adapter);
    }

    private void setupView(){
        TextView deviceName = findViewById(R.id.deviceName);
        deviceName.setText(BLEDeviceName);
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
    }

    /**
     * Function that adds the swiping functionality to the activity_main.xml layout
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initSwipe(){
        RelativeLayout mLayout = findViewById(R.id.mLayout);
        SessionsSwipeGestureDetector SwipeGestureCallback = new SessionsSwipeGestureDetector(mLayout) {
            @Override
            public void onTopToBottomSwap() {
                //Toast.makeText(SessionsActivity.this, "Top to Bottom Swipe", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onBottomToTopSwap() {
                //Toast.makeText(SessionsActivity.this, "Bottom to Top Swipe", Toast.LENGTH_SHORT).show();
            }
        };

        final GestureDetector mGestureDetectorUpperLayout = new GestureDetector(this, SwipeGestureCallback);

        mLayout.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetectorUpperLayout.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
    }

    public SessionListCallback sessionListCallback = new SessionListCallback() {
        @Override
        public void onFileTapped(ExportFileObject obj) {
            Intent sessionScreen = new Intent(getApplicationContext(), SessionMapActivity.class);
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_NAME", BLEDeviceName);
            sessionScreen.putExtra("EXTRA_DOPPLE_DEVICE_ADDRESS", BLEAddress);
            sessionScreen.putExtra("EXTRA_DOPPLE_FILE_LOCATION", obj.FileLocation);
            sessionScreen.putExtra("EXTRA_DOPPLE_FILE_NAME", obj.FileName);
            startActivity(sessionScreen);
        }
    };
}

