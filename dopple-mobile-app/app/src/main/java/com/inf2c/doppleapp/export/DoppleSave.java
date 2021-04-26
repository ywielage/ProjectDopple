package com.inf2c.doppleapp.export;

import android.content.Context;
import android.widget.Toast;

import com.inf2c.doppleapp.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class DoppleSave
{
    private Context mainActivity;

    /**
     * Zorgt ervoor dat je parameters van de Main kunnen worden gebruikt.
     * @param ac context reference
     */
    public DoppleSave(Context ac)
    {
        this.mainActivity = ac;
    }

    File saveToFile2(String data, String fileName){
        File file = new File(mainActivity.getExternalFilesDir(null), "RecordedSessions");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File newFile = new File(file, fileName);
            FileWriter writer = new FileWriter(newFile);
            writer.append(data);
            writer.flush();
            writer.close();
            return file;
        }
        catch(Exception e){
          e.printStackTrace();
        }
        return null;
    }
}
