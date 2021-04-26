package com.inf2c.doppleapp.export;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.ble.BLEConnectionService;
import com.inf2c.doppleapp.conversion.DoppleConversion;
import com.inf2c.doppleapp.conversion.DoppleDataObject;
import com.inf2c.doppleapp.heart_rate.HeartBeat;
import com.inf2c.doppleapp.jsonConversion.models.Json.JsonObject;
import com.inf2c.doppleapp.logging.DoppleLog;

import java.io.File;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

class ExportTCXStructure {
    private final static String TAG = ExportTCXStructure.class.getSimpleName();
    private Context context;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);

    //connection variables
    private String BLEDeviceName = "";
    private String BLEAddress = "";

    private String prevLapTime = "00:00:00";

    ExportTCXStructure(Context context, String deviceName, String deviceAddress) {
       this.context = context;
       BLEDeviceName = deviceName;
       BLEAddress = deviceAddress;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    File createTCX(HashMap<String, List<DoppleDataObject>> dataList, Timestamp stamp){
        try{
            String timestamp = sdf.format(stamp);
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // root element
            Element root = createRootWithNamespaces(document);
            document.appendChild(root);

            Element activities = document.createElement("Activities");
            Element activity = document.createElement("Activity");
            activity.setAttribute("Sport", "Running");
            Element id = document.createElement("Id");
            id.setTextContent(timestamp);


            List<String> laps = new ArrayList<>(dataList.keySet());
            laps.sort((o1, o2) -> {
                Time time1 = Time.valueOf(o1);
                Time time2 = Time.valueOf(o2);
                return time1.compareTo(time2);
            });

            for(String lapTime: laps){
                Element lap = document.createElement("Lap");
                lap.setAttribute("Time", lapTime);
                List<DoppleDataObject> lapData = dataList.get(lapTime);

                lap.appendChild(createTotalLapTimeInSeconds(document, lapTime));
                lap.appendChild(createDistanceMeters(document, lapData));
                lap.appendChild(document.createElement("MaximumSpeed"));
                lap.appendChild(document.createElement("Calories"));
                lap.appendChild(createAverageHeartRateElement(document, lapData));
                lap.appendChild(createMaximumHeartRateElement(document, lapData));
                lap.appendChild(document.createElement("Intensity"));
                lap.appendChild(document.createElement("TriggerMethod"));

                Element track = document.createElement("Track");
                for(DoppleDataObject dataObject: lapData){
                    if(dataObject.Steps != 0) {
                        Element trackPoint = document.createElement("Trackpoint");
                        Element time = document.createElement("Time");
                        time.setTextContent(String.valueOf(dataObject.DeviceTimestamp));

                        Element altitudeMeters = document.createElement("AltitudeMeters");
                        Element distanceMeters = document.createElement("DistanceMeters");
                        Element heartRateBpm = document.createElement("HeartRateBpm");
                        Element hrValue = document.createElement("Value");
                        hrValue.setTextContent(padLeftZeros(String.valueOf(dataObject.HeartBeat), 3));
                        heartRateBpm.appendChild(hrValue);

                        trackPoint.appendChild(time);
                        trackPoint.appendChild(createPositionElement(document, dataObject)); //creates the position object
                        trackPoint.appendChild(altitudeMeters);
                        trackPoint.appendChild(distanceMeters);
                        trackPoint.appendChild(heartRateBpm);
                        trackPoint.appendChild(createExtensionsElement(document, dataObject)); //crates the Extensions object

                        track.appendChild(trackPoint);
                    }
                }
                lap.appendChild(track);
                activity.appendChild(lap);
            }

            activity.appendChild(createCreatorElement(document));
            activity.appendChild(id);
            activities.appendChild(activity);
            root.appendChild(activities);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource domSource = new DOMSource(document);
            String fileName = "Dopple_Session_" + timestamp + ".tcx";
            File file = new File(context.getExternalFilesDir(null), "RecordedSessions");
            if(!file.exists()){
                file.mkdir();
            }
            File actualFile = new File(file, fileName);
            StreamResult streamResult = new StreamResult(actualFile);

            transformer.transform(domSource, streamResult);
            return actualFile;
        } catch (ParserConfigurationException | TransformerException pce) {
            pce.printStackTrace();
            DoppleLog.e(TAG, pce.getMessage());
        }
        return null;
    }

    private Element createExtensionsElement(Document doc, DoppleDataObject object){
        Element extensions = doc.createElement("Extensions");
        Element tpx = doc.createElement("ns3:TPX");
        Element contactTime = doc.createElement("ns3:ContactTime");
        Element earbudsTimestamp = doc.createElement("ns3:EarbudsTimestamp");
        Element stepFrequency = doc.createElement("ns3:StepFrequency");
        Element steps = doc.createElement("ns3:Steps");

        contactTime.setTextContent(String.valueOf(object.Contact_time));
        earbudsTimestamp.setTextContent(String.valueOf(object.EarbudsTimestamp));
        stepFrequency.setTextContent(String.valueOf(object.Step_frequency));
        steps.setTextContent(String.valueOf(object.Steps));

        tpx.appendChild(contactTime);
        tpx.appendChild(earbudsTimestamp);
        tpx.appendChild(stepFrequency);
        tpx.appendChild(steps);
        extensions.appendChild(tpx);
        return extensions;
    }

    private Element createRootWithNamespaces(Document doc){
        Element root = doc.createElement("TrainingCenterDatabase");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("xsi:schemaLocation", "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 http://www.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd");
        root.setAttribute("xmlns:ns5", "http://www.garmin.com/xmlschemas/ActivityGoals/v1");
        root.setAttribute("xmlns:ns4", "http://www.garmin.com/xmlschemas/ProfileExtension/v1");
        root.setAttribute("xmlns:ns3", "http://www.garmin.com/xmlschemas/ActivityExtension/v2");
        root.setAttribute("xmlns:ns2", "http://www.garmin.com/xmlschemas/UserProfile/v2");
        root.setAttribute("xmlns", "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2");
        return root;
    }

    private Element createPositionElement(Document doc, DoppleDataObject object){
        Element position = doc.createElement("Position");
        Element longitude = doc.createElement("LongitudeDegrees");
        longitude.setTextContent(String.valueOf(object.Long));
        Element latitude = doc.createElement("LatitudeDegrees");
        latitude.setTextContent(String.valueOf(object.Lat));
        position.appendChild(longitude);
        position.appendChild(latitude);

        return position;
    }

    private Element createCreatorElement(Document doc){
        Element creator = doc.createElement("Creator");
        creator.setAttribute("xsi:type", "Earbuds Owner");
        Element name = doc.createElement("Name");
        name.setTextContent(BLEDeviceName);
        creator.appendChild(name);
        return creator;
    }

    private Element createAverageHeartRateElement(Document doc, List<DoppleDataObject> lapData){
        Element averageHeartRateBpm = doc.createElement("AverageHeartRateBpm");
        Element value = doc.createElement("value");

        int count = 0;
        int total = 0;

        for(DoppleDataObject dataObject: lapData){
            if(dataObject.Steps != 0) {
                total += Integer.parseInt(dataObject.HeartBeat);
                count++;
            }
        };

        if(count > 0) {
            value.setTextContent(String.valueOf(total/count));
        } else {
            value.setTextContent(String.valueOf(0));
        }

        averageHeartRateBpm.appendChild(value);
        return averageHeartRateBpm;
    }

    private Element createMaximumHeartRateElement(Document doc, List<DoppleDataObject> lapData ){
        Element maximum = doc.createElement("MaximumHeartRateBpm");
        Element value = doc.createElement("value");
        int MaxHeartbeat = 0;

        for(DoppleDataObject dataObject: lapData){
            if(dataObject.Steps != 0) {
                if (Integer.parseInt(dataObject.HeartBeat) > MaxHeartbeat) {
                    MaxHeartbeat = Integer.parseInt(dataObject.HeartBeat);
                }
            }
        }

        value.setTextContent(String.valueOf(MaxHeartbeat));
        maximum.appendChild(value);

        return maximum;
    }


    private Element createTotalLapTimeInSeconds(Document doc, String lapTime){
        Element totalTimeInSeconds = doc.createElement("TotalTimeSeconds");

        Time prevLap = Time.valueOf(prevLapTime);
        Time currentLap = Time.valueOf(lapTime);

        long timeDifference = currentLap.getTime() - prevLap.getTime();
        double differenceInSeconds = new DoppleConversion().round((double)timeDifference / 1000);

        prevLapTime = lapTime;

        totalTimeInSeconds.setTextContent(String.valueOf(differenceInSeconds));
        return totalTimeInSeconds;
    }

    private Element createDistanceMeters(Document doc, List<DoppleDataObject> lapData)
    {
        Element distanceMeters = doc.createElement("DistanceMeters");
        double distance = new DoppleConversion().getTotalDistance(lapData) * 1000;

        distanceMeters.setTextContent(String.valueOf(distance));

        return distanceMeters;
    }

    /**
     * Function that pads zero's from the left
     * @param inputString the string that needs padding
     * @param length the length of the padding
     * @return returns a string padded on the left with zero's
     */
    private String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }
}
