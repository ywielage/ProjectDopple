package com.inf2c.doppleapp.ble;

import android.app.ActivityManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.inf2c.doppleapp.R;
import com.inf2c.doppleapp.heart_rate.BLEHeartRateService;

import java.util.ArrayList;
import java.util.List;
public class BluetoothDeviceAdapter extends ArrayAdapter<BLEDeviceClass> {
    private final static String TAG = BluetoothDeviceAdapter.class.getSimpleName();

    private List<DoppleLowEnergyCallback> listeners = new ArrayList<>();
    private DoppleLowEnergyScanner _scanner; //only required in order to stop the scanning process

    public BluetoothDeviceAdapter(@NonNull Context context, List<BLEDeviceClass> devices, DoppleLowEnergyScanner scanner) {
        super(context, 0, devices);
        this._scanner = scanner;
    }

    private boolean checkIfServiceIsRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * adds the event listeners
     * @param toAdd event listener object
     */
    public void addListener(DoppleLowEnergyCallback toAdd) {
        listeners.add(toAdd);
    }

    private void invokeConnectToDevice(BLEDeviceClass device) {
        for(DoppleLowEnergyCallback li: listeners){
            li.onStartDeviceConnection(device);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BLEDeviceClass device = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_view_1, parent, false);
        }

        LinearLayout bigLayout = convertView.findViewById(R.id.deviceViewLayout);
        TextView name = convertView.findViewById(R.id.dName);
        TextView address = convertView.findViewById(R.id.dAddress);
        ImageView btnConnect = convertView.findViewById(R.id.btnConnect); //not used as a button anymore

        name.setText(device.DeviceName);
        address.setText(device.MacAddress);

        //set the logo for heart rate monitor and ear buds
        if(device.data.isEmpty()){
            //TODO: make the logic that allows for the heartratemonitor to only connect once.
            boolean isServiceRunning = checkIfServiceIsRunning(BLEHeartRateService.class);
            if(!isServiceRunning){
                btnConnect.setBackgroundResource(R.drawable.ic_heartbeat);
                bigLayout.setTag(position);
                bigLayout.setOnClickListener(view -> {
                    //_scanner.stopLeScanDevice();
                    int position1 = (Integer) view.getTag();
                    BLEDeviceClass device1 = getItem(position1);
                    invokeConnectToDevice(device1); //invoke the listener and start the new activity
                });
            }
            else{
                btnConnect.setBackgroundResource(R.drawable.ic_heartbeat_grey);
            }
        }
        else{
            btnConnect.setBackgroundResource(R.drawable.ic_jaybirdlogo);

            bigLayout.setTag(position);
            bigLayout.setOnClickListener(view -> {
                //_scanner.stopLeScanDevice();
                int position1 = (Integer) view.getTag();
                BLEDeviceClass device1 = getItem(position1);
                invokeConnectToDevice(device1); //invoke the listener and start the new activity
            });
        }

        return convertView;
    }
}
