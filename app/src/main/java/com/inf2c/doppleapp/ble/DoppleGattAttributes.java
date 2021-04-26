package com.inf2c.doppleapp.ble;

import java.util.HashMap;

public class DoppleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<>();

    public static String RAW_ACC_DATA = "3b9a134b-b222-4619-829d-2daeeef4276a";
    public static String PROCESSED_ACC_DATA = "c7ab5bf9-0cd3-4826-9cf1-c6981d62466e";
    public static String START_STEP_DETECTION = "aa2777cb-52a6-443e-abb5-6426c4c8f9f5";
    public static String DOPPLE_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    public static String DOPPLE_HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String DOPPLE_HRM_UUID =  "0000180D-0000-1000-8000-00805F9B34FB";
    public static String DOPPLE_HRM_BATTERY = "00002a19-0000-1000-8000-00805f9b34fb";
    static {
        attributes.put(START_STEP_DETECTION, "Start step detection");
        attributes.put("ec84d442-0813-49bd-9c9d-2de3218929bd", "Steps");
        attributes.put(DOPPLE_HEART_RATE_MEASUREMENT, "Heart Rate");
        attributes.put(DOPPLE_HRM_BATTERY, "Battery");
        attributes.put(PROCESSED_ACC_DATA, "Processed Acc Data");
        attributes.put(RAW_ACC_DATA, "Raw Acc Data");
    }

    public static boolean contains(String uuid){
        return attributes.containsKey(uuid);
    }

    public static String lookup(String uuid, String defaultName){
        String name = attributes.get(uuid.toUpperCase());
        return name == null ? defaultName : name;
    }
}
