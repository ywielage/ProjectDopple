package com.inf2c.doppleapp.APIConnection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;

import com.inf2c.doppleapp.ContextManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class API_CONNECTION {
    private final static String baseUrl = getMetaData("api_url");

    /**
     * This function starts the connection with the API.
     * @return Returns the HttpURLConnection.
     * @throws IOException
     */
    private static HttpURLConnection startConnection() throws IOException {
        URL url = new URL(baseUrl + "/api/" + "add-session"); // + endpoint
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        return http;
    }

    public static String getMetaData(String name) {
        Context context = ContextManager.getAppContext();
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString(name);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function sends the json from the jsonObject to the API
     * @param json Expects the json that will be send to the API
     * @throws IOException
     */
    public static void sendJson(String json) throws IOException {
        HttpURLConnection http = startConnection();

        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();

        try(OutputStream os = http.getOutputStream()) {
            os.write(out);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(http.getResponseCode());
        http.disconnect();
    }
}