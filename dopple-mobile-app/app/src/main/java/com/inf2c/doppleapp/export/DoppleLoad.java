package com.inf2c.doppleapp.export;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoppleLoad
{
    private Context mainActivity;

    /**
     * Zorgt ervoor dat je parameters van de Main kunnen worden gebruikt.
     * @param ac context reference
     */
    public DoppleLoad(Context ac)
    {
        this.mainActivity = ac;
    }

    /**
     * Function gets all the files in the folder.
     * @return
     */
    public List<ExportFileObject> getFileList(){
        List<ExportFileObject> exportList = new ArrayList<>();
        File file = new File(mainActivity.getExternalFilesDir(null), "RecordedSessions"); //storage/emulated/0/Android/data/com.inf2c.doppleapp/files/RecordedSessions
        File[] files = file.listFiles();
        if(files != null){
            for(File item: files){
                ExportFileObject e = new ExportFileObject();

                Date date = new Date(item.lastModified());
                DateFormat f = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.UK); //MMddyyHHmmss

                e.FileName =item.getName();
                e.FileDate = f.format(date);
                e.FileLocation = item.getPath();
                exportList.add(e);
            }
        }

        return exportList;
    }

    /**
     * Deze functie haalt de data die in de file data.csv op.
     * Hierna wordt deze data weergegven in de textfield.
     */
    public List<String> load(String filename)
    {
        FileInputStream fis = null;
        List<String> data = new ArrayList<>();
        try
        {
            File pathObject = new File(mainActivity.getExternalFilesDir(null), "RecordedSessions");
            File fileToShare = new File(pathObject, filename);
            fis = new FileInputStream(fileToShare);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String text;

            //Zolang er data in de CSV file zit wordt deze per regel weergegeven
            while ((text = br.readLine()) != null)
            {
                data.add(text);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

}
