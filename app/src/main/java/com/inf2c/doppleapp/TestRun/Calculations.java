package com.inf2c.doppleapp.TestRun;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class Calculations {

    public Calculations() {
    }

    public static double deg2rad(double degrees)
    {
        double pi = Math.PI;
        return degrees * (pi/180);
    }

    public static double getDistanceFromLatLonInKm(double lat1,double lon1,double lat2,double lon2) {
        double R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2-lat1);  // deg2rad below
        double dLon = deg2rad(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c; // Distance in km
    }

    public static double getTotalDistance(List<Trackpoint> list)
    {
        double totalDistance = 0.0;
        for(int i = 0; i+1 < list.size(); i++)
        {
            totalDistance += getDistanceFromLatLonInKm(list.get(i).getLatitudeDegrees(),
                    list.get(i).getLongitudeDegrees(),
                    list.get(i+1).getLatitudeDegrees(),
                    list.get(i+1).getLongitudeDegrees());
        }
        return totalDistance;
    }

    public static String getSpeed(Time time, double km)
    {
        double totalTimeInSec = (time.getHours() * 3600) + (time.getMinutes() * 60) + time.getSeconds();
        double meter = km * 1000;
        double meterPerSecond = meter / totalTimeInSec;
        double speedInKmPerHour = meterPerSecond * 3.6;
        double roundedSpeed = (Math.round(speedInKmPerHour * 100.0)) / 100.0;
        return Double.toString(roundedSpeed);
    }

    public static String getStepFreqs(List<Trackpoint> list)
    {
        int minStepFreq = 0;
        int maxStepFreq = 0;
        int totalStepFreq = 0;
        int avgStepFreq = 0;
        for(int i = 0; i < list.size(); i++)
        {
            int currStepFrequency = list.get(i).getStepFrequency();
            if(currStepFrequency < minStepFreq)
            {
                minStepFreq = currStepFrequency;
            }
            else if(currStepFrequency > maxStepFreq)
            {
                maxStepFreq = currStepFrequency;
            }
            totalStepFreq += currStepFrequency;
            if(list.size() - i == 1)
            {
                avgStepFreq = totalStepFreq / list.size();
            }
        }
        return "Min step frequency: " + minStepFreq + "\nMax step frequency: " + maxStepFreq  + "\nAverage step frequency: " + avgStepFreq;
    }
}
