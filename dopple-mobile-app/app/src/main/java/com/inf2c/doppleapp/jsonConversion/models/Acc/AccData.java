package com.inf2c.doppleapp.jsonConversion.models.Acc;

public class AccData {
    private String timeStamp;
    private int xAxis;
    private int yAxis;
    private int zAxis;
    private String xAxisMarker;

    public AccData(String timeStamp, int xAxis, int yAxis, int zAxis) {
        this.timeStamp = timeStamp;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
    }

    public String getJson() {
        String json = "{" +
                "\"timeStamp\": \""+this.timeStamp+"\"," +
                "\"xAxis\": "+this.xAxis+"," +
                "\"yAxis\": "+this.zAxis+","+
                "\"zAxis\": "+this.zAxis+","+
                "\"xAxisMarker\": \"xMarker\"" +
                "}";
        return json;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getxAxis() {
        return xAxis;
    }

    public void setxAxis(int xAxis) {
        this.xAxis = xAxis;
    }

    public String getxAxisMarker() {
        return xAxisMarker;
    }

    public void setxAxisMarker(String xAxisMarker) {
        this.xAxisMarker = xAxisMarker;
    }
}
