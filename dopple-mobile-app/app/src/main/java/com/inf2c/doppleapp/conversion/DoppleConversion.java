package com.inf2c.doppleapp.conversion;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.gps.GPSLocation;

import java.util.List;

public class DoppleConversion {

    private static final int HEX_RADIX = 16;
    private static final int TWO_PWR_10 = 1024;
    private static final int TWO_PWR_11 = 2048;
    private static final int TWO_PWR_12 = 4096;
    private static final int TWO_PWR_13 = 8192;
    private static final int TWO_PWR_14 = 16384;
    private static final int TWO_PWR_15 = 32768;
    private static final double EARTH_ACCELERATION = 9.80665;

    public DoppleConversion(){

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public DoppleRawDataObject convert(String DataString){
        DoppleRawDataObject object = new DoppleRawDataObject();

        object.Timestamp = (double)Integer.parseUnsignedInt(reverseHexByBytes(DataString.substring(0, 16)),HEX_RADIX) / TWO_PWR_15;

        int start = 16;
        int stop = 20;
        for(int i = 0; i < 20; i++){
            String hex = DataString.substring(start,stop);
            object.doppleData.put("X" + i, shiftAndVerify(hex));
            object.doppleData.put("X_CNT" + i, bitwiseCheck(hex));
            start += 4;
            stop += 4;
            hex = DataString.substring(start,stop);
            object.doppleData.put("Y" + i, shiftAndVerify(hex));
            object.doppleData.put("Y_CNT" + i, bitwiseCheck(hex));
            start += 4;
            stop += 4;
            hex = DataString.substring(start,stop);
            object.doppleData.put("Z" + i, shiftAndVerify(hex));
            object.doppleData.put("Z_CNT" + i, bitwiseCheck(hex));
            start += 4;
            stop += 4;
        }

        return object;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public DoppleProcessedDataObject convertStepInfo(String DataString){
        DoppleProcessedDataObject object = new DoppleProcessedDataObject();
        object.EarbudsTimestamp = Integer.parseUnsignedInt(reverseHexByBytes(DataString.substring(0, 4)), HEX_RADIX);
        object.Steps = Integer.parseUnsignedInt(reverseHexByBytes(DataString.substring(4, 8)), HEX_RADIX);

        int start = 8;
        int stop = 12;
        int maxRequiredLength = (object.Steps * 4) + stop;
        if(maxRequiredLength  <= DataString.length()){
            for(int i = 0; i < object.Steps; i++){
                int step_frequency = Integer.parseUnsignedInt(reverseHexByBytes(DataString.substring(start, stop)), HEX_RADIX);
                start += 4;
                stop += 4;
                int contact_time = Integer.parseUnsignedInt(reverseHexByBytes(DataString.substring(start, stop)), HEX_RADIX);
                start += 4;
                stop += 4;
                object.StepData.put("StepFreq" + i, step_frequency);
                object.StepData.put("ContactTime" + i, contact_time);
            }
        }
        else{
            object.StepData.put("StepFreq0", -1);
            object.StepData.put("ContactTime0", -1);
        }

        return object;
    }

    public String convertRawDataObjectToString(DoppleRawDataObject object){
        StringBuilder builder = new StringBuilder();
        //get xyz * 20
        for(int i = 0; i < 20; i++){
            //get x
            builder.append(object.doppleData.get("X" + i)).append(",");
            builder.append(object.doppleData.get("X_CNT" + i)).append(",");
            //get y
            builder.append(object.doppleData.get("Y" + i)).append(",");
            builder.append(object.doppleData.get("Y_CNT" + i)).append(",");
            //get z
            builder.append(object.doppleData.get("Z" + i)).append(",");
            builder.append(object.doppleData.get("Z_CNT" + i)).append(",");
        }

        return builder.toString();
    }

    public DoppleDataObject getXYZCount(DoppleRawDataObject object, int iteration){
        //get x
        int x = object.doppleData.get("X" + iteration);
        int cntx = object.doppleData.get("X_CNT" + iteration);
        //get y
        int y = object.doppleData.get("Y" +iteration);
        int cnty = object.doppleData.get("Y_CNT" + iteration);
        //get z
        int z = object.doppleData.get("Z" + iteration);
        int cntz = object.doppleData.get("Z_CNT" + iteration);

        int cnt = cntx != 0 ? -1 : 3 * cnty + cntz - 4;
        double EarbudTimestamp = round(object.Timestamp);

        return new DoppleDataObject(object.DeviceTimestamp, EarbudTimestamp, x ,y, z, cnt);
    }

    public double getAverageXValue(DoppleRawDataObject object){
        int count = 0;
        int total = 0;

        for(String key: object.doppleData.keySet()){
            if(key.startsWith("X") && key.length() < 4){
                total+= object.doppleData.get(key);
                count++;
            }

        }

        return (double)total / (double)count;
    }

    public double getAverageYValue(DoppleRawDataObject object){
        int count = 0;
        int total = 0;

        for(String key: object.doppleData.keySet()){
            if(key.startsWith("Y") && key.length() < 4){
                total+= object.doppleData.get(key);
                count++;
            }

        }

        return (double)total / (double)count;
    }

    public double getAverageZValue(DoppleRawDataObject object){
        int count = 0;
        int total = 0;

        for(String key: object.doppleData.keySet()){
            if(key.startsWith("Z") && key.length() < 4){
                total+= object.doppleData.get(key);
                count++;
            }

        }

        return (double)total / (double)count;
    }



    private String reverseHexByBytes(String Hex){
        int length = Hex.length();
        if(length % 2 == 0){
            StringBuilder finalString = new StringBuilder();
            for(int i = 0; i < length / 2; i++){
                finalString.insert(0, Hex.substring(i * 2, (i * 2) + 2));
            }
            return finalString.toString();
        }
        else{
            //incorrect hex byte string, return same string
            return Hex;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int shiftAndVerify(String Hex){
        int parsedHex = Integer.parseUnsignedInt(reverseHexByBytes(Hex), HEX_RADIX);
        int shiftedHex = parsedHex >> 2;
        return ((shiftedHex + (TWO_PWR_13)) % (TWO_PWR_14)) - (TWO_PWR_13);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int bitwiseCheck(String Hex){
        return Integer.parseUnsignedInt(reverseHexByBytes(Hex), HEX_RADIX) & 3;
    }

    public double acceleration(double X, double Y, double Z)
    {
        return ((4 * EARTH_ACCELERATION) * Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2) + Math.pow(Z, 2)) / TWO_PWR_13)  - EARTH_ACCELERATION;
    }

    public double distance(double speed, double time)
    {
        return speed * time;
    }

    public double velocity(double acceleration, double time)
    {
        return acceleration * time;
    }

    public double round(double number){
        return (double)(Math.round(number * 100d) / 100d);
    }

    public double getTotalDistanceGPSLocations(List<GPSLocation> data) {
        GPSLocation waypoint1 = null;
        GPSLocation waypoint2 = null;
        double result = 0.0;
        for (GPSLocation waypoint: data) {
            if(waypoint1 == null) {
                waypoint1 = waypoint;
            } else if(waypoint2 == null) {
                waypoint2 = waypoint;
                result = this.calculateDistance(waypoint1, waypoint2);
            } else {
                waypoint1 = waypoint2;
                waypoint2 = waypoint;
                result = result + this.calculateDistance(waypoint1, waypoint2);
            }
        }

        return result;
    }

    public double getTotalDistance(List<DoppleDataObject> data) {
        DoppleDataObject waypoint1 = null;
        DoppleDataObject waypoint2 = null;
        double result = 0.0;
        for (DoppleDataObject waypoint: data) {
            if(waypoint1 == null) {
                waypoint1 = waypoint;
            } else if(waypoint2 == null) {
                waypoint2 = waypoint;
                result = this.calculateDistance(waypoint1, waypoint2);
            } else {
                waypoint1 = waypoint2;
                waypoint2 = waypoint;
                result = result + this.calculateDistance(waypoint1, waypoint2);
            }
        }

        return result;
    }

    public double calculateDistance(DoppleDataObject waypoint1, DoppleDataObject waypoint2) {
        double dLat = Math.toRadians(waypoint2.Lat - waypoint1.Lat);
        double dLon = Math.toRadians(waypoint2.Long - waypoint1.Long);
        double lat1 = Math.toRadians(waypoint1.Lat);
        double lat2 = Math.toRadians(waypoint2.Lat);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6371 * c;
    }

    public double calculateDistance(GPSLocation waypoint1, GPSLocation waypoint2) {
        double dLat = Math.toRadians(waypoint2.latitude - waypoint1.latitude);
        double dLon = Math.toRadians(waypoint2.longitude - waypoint1.longitude);
        double lat1 = Math.toRadians(waypoint1.latitude);
        double lat2 = Math.toRadians(waypoint2.latitude);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6371 * c;
    }
}
