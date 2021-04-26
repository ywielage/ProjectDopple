package com.inf2c.doppleapp.export;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.inf2c.doppleapp.R;
import com.inf2c.doppleapp.map.SessionListCallback;

import java.util.ArrayList;
import java.util.List;

public class ExportFileAdapter extends ArrayAdapter<ExportFileObject> {
    private List<SessionListCallback> listeners = new ArrayList<>();

    public ExportFileAdapter(@NonNull Context context, List<ExportFileObject> files, DoppleFileHandler handler){
        super(context, 0, files);
    }
    /**
     * adds the event listeners
     * @param toAdd event listener object
     */
    public void addListener(SessionListCallback toAdd) {
        listeners.add(toAdd);
    }

    private void invokeFileTapped(ExportFileObject obj) {
        for(SessionListCallback li: listeners){
            li.onFileTapped(obj);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ExportFileObject item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.export_item_view_1, parent, false);
        }

        TextView tvFileName = convertView.findViewById(R.id.tvFileName);
        TextView tvTimeStamp = convertView.findViewById(R.id.tvTimestamp);
        LinearLayout deviceViewLayout = convertView.findViewById(R.id.deviceViewLayout);

        tvFileName.setText(item.FileName);
        tvTimeStamp.setText(item.FileDate);
        deviceViewLayout.setTag(position);
        deviceViewLayout.setOnClickListener(view -> {
            int position1 = (Integer) view.getTag();
            ExportFileObject item1 = getItem(position1);
            invokeFileTapped(item1);
        });

        return convertView;
    }
}
