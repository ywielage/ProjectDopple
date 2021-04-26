package com.inf2c.doppleapp.jsonConversion.models.Track;

import java.util.ArrayList;

public class Track {
    private ArrayList<TrackPoint> trackPoints;

    public Track() {
        this.trackPoints = new ArrayList<>();
    }

    public void addTrackPoint(TrackPoint trackPoint) {
        this.trackPoints.add(trackPoint);
    }

    public String getJson() {
        String json = "\"tracks\": [{ \"trackpoints\": [";
        for(int i = 0; i < this.trackPoints.size(); i++) {
            TrackPoint trackPoint = this.trackPoints.get(i);
            //Add trackPoint json to the track json
            json += trackPoint.getJson();

            //Add a comma after the trackpoint -> not when it's the last one in the list
            if(this.trackPoints.size() > 1 && i != (this.trackPoints.size() - 1)) {
                json += ",";
            }
        }
        json += "]}],";
        return json;
    }
}