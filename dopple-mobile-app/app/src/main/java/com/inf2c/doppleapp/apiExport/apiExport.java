package com.inf2c.doppleapp.apiExport;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.APIConnection.API_CONNECTION;
import com.inf2c.doppleapp.ContextManager;
import com.inf2c.doppleapp.jsonConversion.models.Json.JsonObject;

import java.io.File;
import java.io.IOException;


public class apiExport {
    public static boolean isConnected;

    /**
     * Exports the json the API
     * @param tcxFile the tcxFile from the session
     * @param csvFile the csvFile form the session
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void exportData(File tcxFile, File csvFile) {
        try {
            JsonObject jsonObject = new JsonObject(tcxFile, csvFile, ContextManager.getAppContext());
            System.out.println(jsonObject.getJson());
            new asyncApiCall().execute(jsonObject.getJson());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Waits for a wifi connection, when wifi connection is established -> send the json to the
     * API_CONNECTION file where it is send to the API
     */
    private static class asyncApiCall extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                isConnected = isOnline();
                while(!isConnected) {
                    Thread.sleep(1000);
                    isConnected = isOnline();
                }
                API_CONNECTION.sendJson(strings[0]);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Checks if the phone is connected to wifi
     * @return boolean
     */
    private static boolean isOnline() {
        ConnectivityManager connManager = (ConnectivityManager) ContextManager.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            return true;
        }
        return false;
    }
}
