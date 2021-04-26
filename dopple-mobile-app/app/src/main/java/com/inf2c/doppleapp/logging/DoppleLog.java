package com.inf2c.doppleapp.logging;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoppleLog {

    private static List<String> logList = new ArrayList<>();
    private static boolean writeToLogCat = true;
    private static boolean writeToRecorder = false;


    /**
     * Logs a DEBUG message
     * @param Tag
     * @param text
     */
    public static void d(String Tag, String text){
        if(writeToLogCat)
            Log.d(Tag, text);
        if(writeToRecorder)
            writeToLogList("D/" + Tag + ": " + text);
    }

    /**
     * Logs an INFO message
     * @param Tag
     * @param text
     */
    public static void i(String Tag, String text){
        if(writeToLogCat)
            Log.i(Tag, text);
        if(writeToRecorder)
            writeToLogList("I/" + Tag + ": " + text);
    }

    /**
     * Logs an ERROR message
     * @param Tag
     * @param text
     */
    public static void e(String Tag, String text){
        if(writeToLogCat)
            Log.e(Tag, text);
        if(writeToRecorder)
            writeToLogList("E/" + Tag + ": " + text);
    }

    /**
     * Logs a VERBOSE message
     * @param Tag
     * @param text
     */
    public static void v(String Tag, String text){
        if(writeToLogCat)
            Log.v(Tag, text);
        if(writeToRecorder)
            writeToLogList("V/" + Tag + ": " + text);
    }

    /**
     * Logs a WARN message
     * @param Tag
     * @param text
     */
    public static void w(String Tag, String text){
        if(writeToLogCat)
            Log.w(Tag, text);
        if(writeToRecorder)
            writeToLogList("W/" + Tag + ": " + text);
    }

    private static void writeToLogList(String message){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.FFF", Locale.US);
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        String timestamp = sdf.format(stamp);
        logList.add(timestamp + " " + message);
    }

    public static void setWriteToLogCat(boolean write){
        writeToLogCat = write;
    }

    public static void setWriteToRecorder(boolean write){
        writeToRecorder = write;
    }

    public static void exportLog(Context context){
        StringBuilder log = new StringBuilder();
        for(String data: logList){
            log.append(data).append("\r\n");
        }
        logList.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        String timestamp = sdf.format(stamp);
        String fileName = "Dopple_LOG_" + timestamp + ".log";
        saveToFile(context, log.toString(), fileName);
    }

    private static void saveToFile(Context c, String data, String fileName){
        File file = new File(c.getExternalFilesDir(null), "Logs");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File newFile = new File(file, fileName);
            FileWriter writer = new FileWriter(newFile);
            writer.append(data);
            writer.flush();
            writer.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
