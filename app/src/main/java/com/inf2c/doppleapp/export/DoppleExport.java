package com.inf2c.doppleapp.export;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.inf2c.doppleapp.MainActivity;
import com.inf2c.doppleapp.conversion.DoppleJsonConversion;

import java.io.File;


public class DoppleExport
{
    private Context context;

    DoppleExport(Context ac)
    {
        this.context = ac;
    }

    void startExport(String file, ExportFileType type){
        File pathObject = new File(context.getExternalFilesDir(null), "RecordedSessions");
        File fileToShare = new File(pathObject, file);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        //get the path uri provider
        Uri path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            path = FileProvider.getUriForFile(context,"com.inf2c.doppleapp.fileprovider", fileToShare);
        } else {
             path = Uri.fromFile(fileToShare);
        }

        if(type.equals(ExportFileType.CSV))
            shareIntent.setType("text/csv");
        else
            shareIntent.setType("application/xml");

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
        shareIntent.putExtra(Intent.EXTRA_STREAM, path);
        context.startActivity(Intent.createChooser(shareIntent, "Opgenomen sessie delen via"));
    }
}
