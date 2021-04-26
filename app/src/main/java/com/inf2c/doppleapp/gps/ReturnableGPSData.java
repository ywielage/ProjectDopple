package com.inf2c.doppleapp.gps;

import com.inf2c.doppleapp.conversion.DoppleDataObject;

public interface ReturnableGPSData {
    /**
     * event that is triggerd when a gps location is received
     * @param location gps object containing timestamps and such
     */
    void onGPSLocationReceived(GPSLocation location, DoppleDataObject object);

    /**
     * event that is triggerd when an error occures.
     * @param error message containing the error.
     */
    void onError(String error, int queueSize);
}
