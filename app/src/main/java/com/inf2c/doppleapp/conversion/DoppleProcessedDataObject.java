package com.inf2c.doppleapp.conversion;

import java.util.HashMap;

public class DoppleProcessedDataObject {

    public long DeviceTimestamp;
    public double EarbudsTimestamp;
    public int Steps;

    public HashMap<String, Integer> StepData;

    public DoppleProcessedDataObject(){
        StepData = new HashMap<>();
    }

}
