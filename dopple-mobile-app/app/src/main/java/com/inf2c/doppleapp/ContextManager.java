package com.inf2c.doppleapp;

import android.app.Application;
import android.content.Context;

public class ContextManager extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        ContextManager.context = getApplicationContext();
    }

    /**
     * Gets the appContext from the application, used in classes where the context is not available
     * @return Context
     */
    public static Context getAppContext() {
        return ContextManager.context;
    }
}
