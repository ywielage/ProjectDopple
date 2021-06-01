package com.inf2c.doppleapp.TestRun;

public class Trackpoint {
    private long time;
    private float longitudeDegrees;
    private float latitudeDegrees;
    private float altitudeMeters;
    private float distanceMeters;
    private int heartRateBpm;
    private int contactTime;
    private float earbudsTimeStamp;
    private int stepFrequency;
    private int steps;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getLongitudeDegrees() {
        return longitudeDegrees;
    }

    public void setLongitudeDegrees(float longitudeDegrees) {
        this.longitudeDegrees = longitudeDegrees;
    }

    public float getLatitudeDegrees() {
        return latitudeDegrees;
    }

    public void setLatitudeDegrees(float latitudeDegrees) {
        this.latitudeDegrees = latitudeDegrees;
    }

    public float getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(float altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public float getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(float distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public int getHeartRateBpm() {
        return heartRateBpm;
    }

    public void setHeartRateBpm(int heartRateBpm) {
        this.heartRateBpm = heartRateBpm;
    }

    public int getContactTime() {
        return contactTime;
    }

    public void setContactTime(int contactTime) {
        this.contactTime = contactTime;
    }

    public float getEarbudsTimeStamp() {
        return earbudsTimeStamp;
    }

    public void setEarbudsTimeStamp(float earbudsTimeStamp) {
        this.earbudsTimeStamp = earbudsTimeStamp;
    }

    public int getStepFrequency() {
        return stepFrequency;
    }

    public void setStepFrequency(int stepFrequency) {
        this.stepFrequency = stepFrequency;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}