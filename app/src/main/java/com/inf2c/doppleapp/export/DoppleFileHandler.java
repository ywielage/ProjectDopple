package com.inf2c.doppleapp.export;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.APIConnection.API_CONNECTION;
import com.inf2c.doppleapp.SessionMapActivity;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.gps.GPSLocation;
import com.inf2c.doppleapp.jsonConversion.models.Json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DoppleFileHandler {
    private final static String TAG = DoppleFileHandler.class.getSimpleName();

    private DoppleSave doppleSaveFile;
    private DoppleLoad doppleLoadFile;
    private DoppleExport doppleExportFile;
    private ExportTCXStructure tcxExporter;
    private Context activity;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    public DoppleFileHandler(Context ac, String DeviceName, String Address){
        this.activity = ac;
        doppleSaveFile = new DoppleSave(this.activity);
        doppleLoadFile = new DoppleLoad(this.activity);
        doppleExportFile = new DoppleExport(this.activity);
        tcxExporter = new ExportTCXStructure(this.activity, DeviceName, Address);
    }

    public File saveToCSVFile(List<DoppleDataObject> dataList, Timestamp stamp) {
        //Timestamp stamp = new Timestamp(System.currentTimeMillis());
        String timestamp = sdf.format(stamp);
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("DeviceTimestamp,EarbudsTimestamp,X,Y,Z,CNT,Steps,Step_frequency,Contact_time,Long,Lat");
        for(DoppleDataObject item: dataList){
            dataBuilder.append("\n" + item.toString());
        }
        String fileName = "Dopple_Session_" + timestamp + ".csv";
        File file = doppleSaveFile.saveToFile2(dataBuilder.toString(), fileName);
        return file;
    }

    public void saveToTCXFile(List<DoppleDataObject> dataList, Timestamp stamp) {
        //Timestamp stamp = new Timestamp(System.currentTimeMillis());
        String timestamp = sdf.format(stamp);

        StringBuilder dataBuilder = new StringBuilder();

        dataBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<TrainingCenterDatabase\n" +
                "  xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\"\n" +
                "  xmlns:ns5=\"http://www.garmin.com/xmlschemas/ActivityGoals/v1\"\n" +
                "  xmlns:ns3=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\"\n" +
                "  xmlns:ns2=\"http://www.garmin.com/xmlschemas/UserProfile/v2\"\n" +
                "  xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"\n" +
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://www.garmin.com/xmlschemas/ProfileExtension/v1\">  \n" +
                "  <Activities>\n" +
                "    <Activity Sport=\"Running\">\n" +
                "      <Id>"+ timestamp +"</Id>\n" +
                "       <Lap StartTime=\"Unknown\">\n" + // TODO: Lap integration
                "        <TotalTimeSeconds></TotalTimeSeconds>\n" +
                "        <DistanceMeters></DistanceMeters>\n" +
                "        <MaximumSpeed></MaximumSpeed>\n" +
                "        <Calories></Calories>\n" +
                "        <AverageHeartRateBpm>\n" +
                "           <Value></Value>\n" +
                "        </AverageHeartRateBpm>\n" +
                "        <MaximumHeartRateBpm>\n" +
                "           <Value></Value>\n" +
                "        </MaximumHeartRateBpm>\n" +
                "        <Intensity></Intensity>\n" +
                "        <TriggerMethod></TriggerMethod>\n" +
                "        <Track>\n");
        for(DoppleDataObject item: dataList){
            dataBuilder.append("\t\t\t\t\t<Trackpoint>\n" +
                    "            <Time>"+ item.DeviceTimestamp +"</Time>\n" +
                    "            <Position>\n" +
                    "              <LatitudeDegrees>"+ item.Lat +"</LatitudeDegrees>\n" +
                    "              <LongitudeDegrees>"+ item.Long +"</LongitudeDegrees>\n" +
                    "            </Position>\n" +
                    "            <AltitudeMeters></AltitudeMeters>\n" +
                    "            <DistanceMeters></DistanceMeters>\n" +
                    "            <HeartRateBpm>\n" +
                    "              <Value></Value>\n" +
                    "            </HeartRateBpm>\n" +
                    "            <Extensions>\n" +
                    "              <ns3:TPX>\n" +
                    "                <ns3:CNT>"+ item.CNT +"</ns3:CNT>\n" +
                    "                <ns3:ContactTime>"+ item.Contact_time +"</ns3:ContactTime>\n" +
                    "                <ns3:EarbudsTimestamp>"+ item.EarbudsTimestamp +"</ns3:EarbudsTimestamp>\n" +
                    "                <ns3:StepFrequency>"+ item.Step_frequency +"</ns3:StepFrequency>\n" +
                    "                <ns3:Steps>"+ item.Steps +"</ns3:Steps>\n" +
                    "                <ns3:X>"+ item.X +"</ns3:X>\n" +
                    "                <ns3:Y>"+ item.Y +"</ns3:Y>\n" +
                    "                <ns3:Z>"+ item.Z +"</ns3:Z>\n" +
                    "              </ns3:TPX>\n" +
                    "            </Extensions>\n" +
                    "          </Trackpoint>\n");
        }

        dataBuilder.append("\t\t\t\t</Track>\n" +
                "           <Extensions>\n" +
                "              <ns3:LX>\n" +
                "                <ns3:AvgSpeed>Unknown</ns3:AvgSpeed>\n" +
                "              </ns3:LX>\n" +
                "           </Extensions>\n" +
                "       </Lap>\n" +
                "       <Creator xsi:type=\"Earbuds Owner\">\n" +
                "           <Name>Jaybird Vista</Name>\n" +
                "      </Creator>\n" +
                "   </Activity>\n" +
                "  </Activities>");

        String fileName = "Dopple_Session_" + timestamp + ".tcx";
        doppleSaveFile.saveToFile2(dataBuilder.toString(), fileName);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public File saveDataToFile(HashMap<String, List<DoppleDataObject>> dataList, ExportFileType type, Timestamp stamp) {
        if(type == ExportFileType.CSV) {
            List<DoppleDataObject> csvExportData = new ArrayList<>();

            List<String> laps = new ArrayList<>(dataList.keySet());
            laps.sort((o1, o2) -> {
                Time time1 = Time.valueOf(o1);
                Time time2 = Time.valueOf(o2);
                return time1.compareTo(time2);
            });

            for(String lapTime: laps){
                csvExportData.addAll(dataList.get(lapTime));
            }

            saveToCSVFile(csvExportData, stamp);
        } else if(type == ExportFileType.TCX) {
            File file = tcxExporter.createTCX(dataList, stamp);
            return file;

        } else {
            //TODO: throw error
        }
        return null;
    }



    public void saveToFileHEX(List<String> dataList) {
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        String timestamp = sdf.format(stamp);
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("TimeStamp,HexString,Longitude,Latitude");
        for(String item: dataList){
            dataBuilder.append("\n" + item);
        }
        String fileName = "Dopple_HexSession_" + timestamp + ".csv";
        doppleSaveFile.saveToFile2(dataBuilder.toString(), fileName);
    }

    public List<ExportFileObject> loadFileList(){
        return doppleLoadFile.getFileList();
    }

    public List<GPSLocation> loadGPSDataFromFile(String fileName){
        List<GPSLocation> locations = new ArrayList<>();
        List<String> csvData = doppleLoadFile.load(fileName);
        csvData.remove(0);
        for(String record: csvData){
            //10 en 11 zijn long en lat
            String[] data = record.split(",");
            if(data[6].isEmpty()) {
                locations.add(new GPSLocation(Float.parseFloat(data[10]), Float.parseFloat(data[9]), 0f, ""));
            }
        }

        return locations;
    }

    public List<DoppleDataObject> loadStepData(String fileName){
        List<DoppleDataObject> dataList = new ArrayList<>();
        List<String> csvData = doppleLoadFile.load(fileName);
        Log.e(TAG, "Record Count: " + csvData.size());
        csvData.remove(0);
        for(String record: csvData){

            String[] data = record.split(",");

            if(!data[6].isEmpty()){
                long timestamp = Long.parseLong(data[0]);
                double earbudstamp;
                if(data[1].isEmpty()){
                    earbudstamp = -1;
                }
                else{
                    earbudstamp = Double.parseDouble(data[1]);
                }
                int steps = Integer.parseInt(data[6]);

                dataList.add(new DoppleDataObject(timestamp, earbudstamp, steps, 0, 0, ""));
            }
        }

        return dataList;
    }

    public void export(String fileName, ExportFileType type){
        doppleExportFile.startExport(fileName, type);
    }

    private String createXYZSampleHeader(){
        StringBuilder builder = new StringBuilder();
        //get xyz * 20
        for(int i = 0; i < 20; i++){
            //get x
            builder.append("X").append(i).append(",");
            builder.append("X_CNT").append(i).append(",");
            //get y
            builder.append("Y").append(i).append(",");
            builder.append("Y_CNT").append(i).append(",");
            //get z
            builder.append("Z").append(i).append(",");
            builder.append("Z_CNT").append(i).append(",");
        }

        return builder.toString();
    }

    public boolean checkFileExsist(String fileName, String extention) {
        File pathObject = new File(activity.getExternalFilesDir(null), "RecordedSessions");
        File fileToShare = new File(pathObject, fileName + "." + extention);
        return fileToShare.exists();
    }
}
