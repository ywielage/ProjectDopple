package com.inf2c.doppleapp.gps;

public class GPSLocation {
    public double longitude;
    public double latitude;
    public double speed;
    public String timestamp;

    public GPSLocation(){
        this(0, 0, 0, "");
    }
    public GPSLocation(double lat, double longt, float speed, String timestamp){
        this.longitude = longt;
        this.latitude = lat;
        this.speed = speed;
        this.timestamp = timestamp;
    }
}
