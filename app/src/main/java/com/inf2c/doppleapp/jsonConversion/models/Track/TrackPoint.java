package com.inf2c.doppleapp.jsonConversion.models.Track;

public class TrackPoint {
    private String timeStamp;
    private String longitude;
    private String latitude;
    private double speed;

    public TrackPoint(String timeStamp, String longitude, String lattitude, double speed) {
        this.timeStamp = timeStamp;
        this.longitude = longitude;
        this.latitude = lattitude;
        this.speed = speed;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getJson() {
        return "{" +
            "\"timeStamp\": \""+this.timeStamp+"\"," +
            "\"longitude\": \""+this.longitude+"\"," +
            "\"latitude\": \""+this.latitude+"\"," +
            "\"speed\": "+this.speed+"" +
                "}";
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String lattitude) {
        this.latitude = lattitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}