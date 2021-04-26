package com.inf2c.doppleapp.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.logging.DoppleLog;

import java.util.ArrayList;
import java.util.List;

public class DoppleGPSManager {
    private final static String TAG = DoppleGPSManager.class.getSimpleName();
    private long MillisecondTime, LastUpdate = 0L ;
    private int Seconds;
    private GPSLocation lastLocObject;
    private DoppleConversion converter;

    /**
     * List of listeners to call upon events
     */
    private List<ReturnableGPSData> listeners = new ArrayList<>();

    /**
     * reference to the main activity
     */
    private Context context;

    /**
     * androids location manager
     */
    private LocationManager locationManager;

    /**
     * Constructor that activates permissions
     * @param ac requires the main context
     */
    public DoppleGPSManager(Context ac){
        this.context = ac;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        converter = new DoppleConversion();
    }

    /**
     * adds the event listeners
     * @param toAdd event listener object
     */
    public void addListener(ReturnableGPSData toAdd) {
        listeners.add(toAdd);
    }

    List<DoppleDataObject> dataQueue = new ArrayList<>();

    /**
     * returns gps information to the subscribed classes
     */
    private void returnGPSData(DoppleDataObject obj) {
        //get the gps data
//        MillisecondTime = SystemClock.uptimeMillis() - LastUpdate;
//        Seconds = (int) (MillisecondTime / 1000);
//        if(Seconds > 10 ) {
//            //if the last update was more than 10 seconds ago, reset the current object and reinstate the queue
//            lastLocObject = null;
//            enableLocationUpdates();
//        }

        if(lastLocObject != null){
            //check queue for messages
            if(dataQueue.size() == 0){
                for(ReturnableGPSData li: listeners){
                    li.onGPSLocationReceived(lastLocObject, obj);
                }
            }
            else{
                //first fire off the queued objects to all listeners.
                for(DoppleDataObject queuedObject: dataQueue){
                    for(ReturnableGPSData li: listeners){
                        li.onGPSLocationReceived(lastLocObject, queuedObject);
                    }
                }
                dataQueue.clear();

                //then send the current object to all listeners.
                for(ReturnableGPSData li: listeners){
                    li.onGPSLocationReceived(lastLocObject, obj);
                }
            }
        }
        else{
            dataQueue.add(obj);
            if(dataQueue.size() == 250){
                reportError("250 data objects waiting in queue, gps not loaded");
            }
        }
    }

    /**
     * reports the error back to the subscribed clients
     * @param Error the error string
     */
    private void reportError(String Error){
        for(ReturnableGPSData li: listeners){
            li.onError(Error, dataQueue.size());
        }
    }

    public void enableLocationUpdates(){
        try {
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener, null);
            }
            else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mLocationListener, null);
            }
            else
            {
                reportError("No location data available");
            }
        } catch(SecurityException ex) {
            reportError(ex.getMessage());
        }
    }

    public void disableLocationUpdates(){
        locationManager.removeUpdates(mLocationListener);
    }


    /**
     * function that enables the internal location manager and then triggers an internal event.
     */
    public void getLocation(DoppleDataObject data){
        returnGPSData(data);
    }


    /**
     * event listener for the location listener
     */
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            if(location != null) {
                DoppleLog.d(TAG, "GPS Speed: " + location.getSpeed());
                float acc = location.getAccuracy();
                if (acc < 40) {
                    DoppleLog.d(TAG, "Writing gps data, accuracy is under 100 meter radius... accuracy: " + acc);
                    LastUpdate = SystemClock.uptimeMillis();
                    GPSLocation newObject = new GPSLocation(location.getLatitude(), location.getLongitude(), location.getSpeed(), String.valueOf(LastUpdate));
                    double distance = lastLocObject != null ? converter.calculateDistance(lastLocObject, newObject) : 10;
                    DoppleLog.d(TAG, "distance between last object: " + distance * 1000);
                    if (distance * 1000 > 5) {
                        lastLocObject = newObject;
                    } else {
                        if(lastLocObject != null) {
                            lastLocObject.speed = location.getSpeed();
                        }
                    }
                } else {
                    if(lastLocObject != null) {
                        lastLocObject.speed = location.getSpeed();
                    }
                    DoppleLog.d(TAG, "Didn't write gps data, accuracy is too low... accuracy: " + acc);
                }
            }
            else{
                DoppleLog.d(TAG, "Location object was null");
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    };
}
