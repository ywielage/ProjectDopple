package com.inf2c.doppleapp.text_to_speech;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class DoppleSpeech {

    private TextToSpeech speech;

    public DoppleSpeech(Context context){
        speech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    speech.setLanguage(Locale.US);
                }
            }
        });
    }

    /**
     * Function that closes connections and releases resources used by the engine.
     */
    public void close(){
        speech.stop();
        speech.shutdown();
    }

    /**
     * Function that gives the good job feedback
     */
    public void sayGoodJob(){
        speech.speak("Good Job!", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Function that converts the given time into a text to speech
     * @param hours
     * @param minutes
     * @param seconds
     */
    public void announceTime(int hours, int minutes, int seconds){
        StringBuilder builder = new StringBuilder();
        builder.append("You have been running for ");
        if(hours != 0) {
            builder.append(hours).append(" hours, ");
        }
        if(minutes != 0) {
            builder.append(minutes).append(" minutes, and");
        }
        if(seconds == 1){
            builder.append(seconds).append(" second.");
        }
        else {
            builder.append(seconds).append(" seconds.");
        }

        speech.speak(builder.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Announces that the session has started.
     */
    public void saySessionStarted(){
        speech.speak("Recording started!", TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Announces that the session has stopped and gives the end time of the session
     * @param hours
     * @param minutes
     * @param seconds
     */
    public void saySessionStopped(int hours, int minutes, int seconds){
        StringBuilder builder = new StringBuilder();
        builder.append("Recording stopped, your end time was ");
        if(hours != 0) {
            builder.append(hours).append(" hours, ");
        }
        if(minutes != 0) {
            builder.append(minutes).append(" minutes, and");
        }
        if(seconds == 1){
            builder.append(seconds).append(" second.");
        }
        else {
            builder.append(seconds).append(" seconds.");
        }

        speech.speak(builder.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void sayStepCount(int steps) {
        StringBuilder builder = new StringBuilder();
        builder.append(" You have completed");
        if(steps == 1){
            builder.append(steps).append(" step.");
        }
        else {
            builder.append(steps).append(" steps.");
        }

        speech.speak(builder.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Announces the device name to the user.
     * @param deviceName
     */
    public void sayConnectedTo(String deviceName){
        speech.speak("Connected to " + deviceName, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
