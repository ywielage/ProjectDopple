package com.inf2c.doppleapp.conversion;

import java.util.HashMap;

public class DoppleRawDataObject {
    public double Timestamp;
    public long DeviceTimestamp;
    public String HexData;
    public HashMap<String, Integer> doppleData;
    public DoppleRawDataObject(){
        doppleData = new HashMap<>();
    }
}
