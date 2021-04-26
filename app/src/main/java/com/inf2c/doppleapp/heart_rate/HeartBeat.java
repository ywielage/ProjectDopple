package com.inf2c.doppleapp.heart_rate;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inf2c.doppleapp.R;

public class HeartBeat extends Fragment {
    private static HeartBeat instance;

    public HeartBeat() {
        // Required empty public constructor
    }

    private boolean checkIfServiceIsRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static HeartBeat getInstance(){
        if(instance != null){
            return instance;
        }
        else {
            return new HeartBeat();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LinearLayout imageBtn = getActivity().findViewById(R.id.imageLayout);
        imageBtn.getParent().requestDisallowInterceptTouchEvent(false);
        imageBtn.setOnClickListener(v -> {
            DraggableLayout test = getActivity().findViewById(R.id.dLayout);
            if(test.getCurrentX() == test.getMinX()){
                test.moveToX(test.getMaxX());
            }
            else if(test.getCurrentX() == test.getMaxX()){
                test.moveToX(test.getMinX());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setText(String text){
        if(text.length() >= 20){
            text = text.substring(0, 20) + "...";
        }

        if(getActivity() != null){
            TextView view = getActivity().findViewById(R.id.heartbeatDisplayText);
            view.setText(text);
        }
    }

    public void setBattery(int text){
        if(getActivity() != null){
            ImageView batteryIcon = getActivity().findViewById(R.id.batteryIcon);
            batteryIcon.setImageResource(getBatteryIcon(text));
            TextView view = getActivity().findViewById(R.id.batteryText);
            view.setText(text + "%");
        }
    }

    public void setBPM(String bpm){
        if(getActivity() != null){
            TextView bpmView = getActivity().findViewById(R.id.heartRateLabel);
            if(bpmView != null){
                bpmView.setText(String.format("%s BPM", bpm));
            }
        }
    }

    private int getBatteryIcon(int batteryLevel) {
        if(batteryLevel == -1) {
            return R.drawable.dopple_ic_battery_unknown_24dp;
        } else if(batteryLevel > 90) {
            return R.drawable.dopple_ic_battery_full_24dp;
        } else if(batteryLevel > 80) {
            return R.drawable.dopple_ic_battery_90_24dp;
        } else if(batteryLevel > 60) {
            return R.drawable.dopple_ic_battery_80_24dp;
        } else if(batteryLevel > 50) {
            return R.drawable.dopple_ic_battery_60_24dp;
        } else if(batteryLevel > 30) {
            return R.drawable.dopple_ic_battery_50_24dp;
        } else if(batteryLevel > 20) {
            return R.drawable.dopple_ic_battery_30_24dp;
        } else {
            return R.drawable.dopple_ic_battery_20_24dp;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_heart_beat, container, false);
    }
}
