package com.inf2c.doppleapp.conversion;

import java.util.HashMap;

public class DoppleDataObject {
    public long DeviceTimestamp;
    public double EarbudsTimestamp;
    public int X;
    public int Y;
    public int Z;
    public int CNT;
    public int Steps;
    public int Step_frequency;
    public int Contact_time;
    public String HeartBeat;
    public double Long;
    public double Lat;

    public DoppleDataObject(long DeviceTimestamp, double EarbudsTimestamp, int x, int y, int z, int CNT){
        this.DeviceTimestamp = DeviceTimestamp;
        this.EarbudsTimestamp = EarbudsTimestamp;
        this.X = x;
        this.Y = y;
        this.Z = z;
        this.CNT = CNT;
    }

    public DoppleDataObject(long DeviceTimestamp, double EarbudsTimestamp, int Steps, int Step_frequency, int Contact_time, String HeartBeat){
        this.DeviceTimestamp = DeviceTimestamp;
        this.EarbudsTimestamp = EarbudsTimestamp;
        this.Steps = Steps;
        this.Step_frequency = Step_frequency;
        this.Contact_time = Contact_time;
        this.HeartBeat = HeartBeat;
    }


    @Override
    public String toString() {
        if(X == 0 && Y == 0 && Z == 0){
            return (DeviceTimestamp == 0 ? "" : DeviceTimestamp) + ","
                    + (EarbudsTimestamp == 0 ? "" : EarbudsTimestamp)+ ",,,,,"
                    + (Steps == 0 ? "" : Steps) + ","
                    + (Step_frequency == 0 ? "" : Step_frequency)+ ","
                    + (Contact_time ==0 ? "" : Contact_time)+ ","
                    + Long + ","
                    + Lat;
        }
        else{
            return (DeviceTimestamp == 0 ? "" : DeviceTimestamp) + ","
                    + (EarbudsTimestamp == 0 ? "" : EarbudsTimestamp)+ ","
                    + (X ==0 ? "" : X) + ","
                    + (Y == 0 ? "" : Y) + ","
                    + (Z == 0 ? "" : Z) + ","
                    + CNT + ","
                    + (Steps == 0 ? "" : Steps) + ","
                    + (Step_frequency == 0 ? "" : Step_frequency)+ ","
                    + (Contact_time ==0 ? "" : Contact_time)+ ","
                    + Long + ","
                    + Lat;
        }
    }
}
