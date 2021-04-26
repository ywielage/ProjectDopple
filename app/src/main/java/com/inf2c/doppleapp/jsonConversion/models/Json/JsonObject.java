package com.inf2c.doppleapp.jsonConversion.models.Json;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import com.inf2c.doppleapp.ContextManager;
import com.inf2c.doppleapp.jsonConversion.models.Acc.AccData;
import com.inf2c.doppleapp.jsonConversion.models.Acc.AccInfo;
import com.inf2c.doppleapp.jsonConversion.models.Acc.ProccesedAccData;
import com.inf2c.doppleapp.jsonConversion.models.Track.Track;
import com.inf2c.doppleapp.jsonConversion.models.Track.TrackPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.UUID;

public class JsonObject {
    private Context context;
    //Default data
    private String userId;
    private String timeStamp;
    private String activity;
    private String lapStartTime;
    private String lapEndTime;
    private double totalTime;
    private double distance;
    private double maxSpeed;
    private int calories;
    private String intensity;
    private String triggerMethod;
    private int averageHeartRate;
    private int maxHeartRate;

    //Models.Track.Track -> has trackpoints
    private Track track;

    //Models.Acc.AccData -> has ProcessedAccData
    private AccInfo accInfo;

    /**
     * Creates a jsonObject that will convert an xml file into a json format that can be send to the API.
     * @param tcxFile Expects an xml file in the File format.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public JsonObject(File tcxFile, File csvFile, Context context) {
        this.setFileInfo(tcxFile);
        this.setAccInfo(csvFile);
        this.context = context;
    }

    private String getUUID() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ContextManager.getAppContext());
        String id = settings.getString("UUID", "0");
        return id;
    }


    /**
     * This function converts the xml file into a json format that can be send to the API.
     * @param file This is the xml file that is being converted.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setFileInfo(File file) {
        try {

            //Create document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            //Build document and normalize it
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            //Get root tag (Not necessary)
            Element root = document.getDocumentElement();

            //Get and set all default data
            NodeList defaultDataList = document.getElementsByTagName("Activity");
            this.setDefaultData(defaultDataList);

            NodeList heartRateList = document.getElementsByTagName("AverageHeartRateBpm");
            this.averageHeartRate = Integer.parseInt(heartRateList.item(0).getTextContent().trim());

            NodeList maxHeartRateList = document.getElementsByTagName("MaximumHeartRateBpm");
            this.maxHeartRate = Integer.parseInt(maxHeartRateList.item(0).getTextContent().trim());

            //Get all trackpoint and create a new track
            NodeList trackpointsList = document.getElementsByTagName("Trackpoint");
            this.track = this.createTrack(trackpointsList);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setAccInfo(File file) {
        ArrayList<String[]> list = this.setAccInfoToList(file);
        ArrayList<ProccesedAccData> proccesedAccDataList = new ArrayList<>();
        ArrayList<Integer> stepFrequencyList = new ArrayList<>();
        ArrayList<Integer> contactTimeList = new ArrayList<>();

        int steps = 0;
        for(String[] strings : list) {
            try {
                long deviceTimestamp = Long.parseLong(strings[0]);
                String timeStamp = millToLocalDateTime(deviceTimestamp);
                timeStamp = convertToNewFormat(timeStamp);

                int xAxis = getIntFromString(strings[2]);
                int yAxis = getIntFromString(strings[3]);
                int zAxis = getIntFromString(strings[3]);

                steps += getIntFromString(strings[6]);

                int stepFrequency = getIntFromString(strings[7]);
                stepFrequencyList.add(stepFrequency);

                int contactTime = getIntFromString(strings[8]);
                contactTimeList.add(contactTime);

                String longitude = strings[9];
                String latitude = strings[10];

                ProccesedAccData proccesedAccData = new ProccesedAccData(stepFrequency, contactTime);
                AccData accData = new AccData(timeStamp, xAxis, yAxis, zAxis);
                proccesedAccData.addAccData(accData);
                proccesedAccDataList.add(proccesedAccData);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        this.accInfo = new AccInfo(timeStamp, steps, contactTimeList, stepFrequencyList, proccesedAccDataList);
    }

    private int getIntFromString(String string) {
        try {
            return Integer.parseInt(string);
        } catch(Exception e) {
            return 0;
        }
    }

    private ArrayList<String[]> setAccInfoToList(File file) {
        ArrayList<String> stringList = new ArrayList<>();
        try {
            Scanner reader = new Scanner(file);
            while(reader.hasNextLine()) {
                String data = reader.nextLine();
                stringList.add(data);
            }
            reader.close();
        } catch(Exception e) {

        }

        ArrayList<String[]> list = new ArrayList<>();
        boolean firstLine = true;
        for(String s : stringList) {
            if(firstLine) {
                firstLine = false;
            } else {
                String[] elements = s.split(",");
                list.add(elements);
            }
        }
        return list;
    }

    private String createUUID() {
        //Generate UUID
        UUID id = UUID.randomUUID();
        return id.toString();
    }

    /**
     * This function sets all the default data in the jsonObject.
     * @param list Expects a nodeList with all of the default data elements from the xml file.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setDefaultData(NodeList list) {
        this.userId = getUUID();
        for (int index = 0; index < list.getLength(); index++) {
            Node node = list.item(index);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                this.timeStamp = LocalDateTime.now().toString();

                //Set all the default data to the class fields
                this.activity = element.getAttribute("Sport");
                this.totalTime = parseDouble(getElementContent(element, "TotalTimeSeconds"));
                this.distance = parseDouble(getElementContent(element, "DistanceMeters"));
                this.maxSpeed = parseDouble(getElementContent(element, "MaximumSpeed"));
                this.calories = parseInt(getElementContent(element, "Calories"));
                this.intensity = getElementContent(element, "Intensity");
                this.triggerMethod = getElementContent(element, "TriggerMethod");
            }
        }
    }

    /**
     * This function returns the text from an element from the xml file.
     * @param element The element that the text is being retrieved from.
     * @param tagName The tag name from the element that is being retrieved.
     * @return Returns the text from the element.
     */
    private String getElementContent(Element element, String tagName) {
        NodeList list = element.getElementsByTagName(tagName).item(0).getChildNodes();
        if(list.getLength() > 0) {
            return element.getElementsByTagName(tagName).item(0).getTextContent();
        }
        return null;
    }

    /**
     * This function converts a String into a double.
     * @param element The element text that has to be converted to a double.
     * @return Returns the converted double.
     */
    private double parseDouble(String element) {
        if (element != null && element.length() > 0) {
            try {
                return Double.parseDouble(element);
            } catch(Exception e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * This function converts a String into an int.
     * @param element The element text that has to be converted to a double.
     * @return Returns the converted int.
     */
    private int parseInt(String element) {
        if (element != null && element.length() > 0) {
            try {
                return Integer.parseInt(element);
            } catch(Exception e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * This function creates / set the track with all of the trackPoints.
     * @param list Expects a nodeList with all of the trackPoints.
     * @return Returns a track filled with trackPoints.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Track createTrack(NodeList list) throws ParseException {
        Track track = new Track();

        for (int index = 0; index < list.getLength(); index++) {
            Node node = list.item(index);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                String timeStamp = getElementContent(element, "Time");
                long timeStampLong = Long.parseLong(timeStamp);
                timeStamp = millToLocalDateTime(timeStampLong);
                timeStamp = convertToNewFormat(timeStamp);

                String longitude = getElementContent(element, "LongitudeDegrees");
                String latitude = getElementContent(element, "LatitudeDegrees");
                double speed = 0;

                TrackPoint trackPoint = new TrackPoint(timeStamp, longitude, latitude, speed);
                track.addTrackPoint(trackPoint);
            }
        }
        return track;
    }

    /**
     * This function converts the milliseconds into a date format.
     * @param m The milliseconds that will be converted to a date format.
     * @return Returns the temporary date format.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String millToLocalDateTime(long m) {
        return Instant.ofEpochMilli(m).toString();
    }

    /**
     * This function converts the temporary date format into the correct final date format.
     * @param dateStr Expects the temporary date format.
     * @return Returns the corrent date format that can be send to the API.
     * @throws ParseException
     */
    private static String convertToNewFormat(String dateStr) throws ParseException {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        SimpleDateFormat destFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sourceFormat.setTimeZone(utc);
        Date convertedDate = sourceFormat.parse(dateStr);
        return destFormat.format(convertedDate);
    }

    /**
     * This function creates the actual json format from the xml file.
     * @return Returns the json format
     */
    public String getJson() {
        String json =
                "{\"userId\":\""+this.userId+"\"," +
                "\"timeStamp\": \""+this.timeStamp+"\"," +
                "\"activity\": \""+this.activity+"\"," +
                "\"lapStartTime\": \""+this.lapStartTime+"\"," +
                "\"lapEndTime\": \""+this.lapEndTime+"\"," +
                "\"totalTime\": "+this.totalTime+"," +
                "\"distance\": "+this.distance+"," +
                "\"maxSpeed\": "+this.maxSpeed+"," +
                "\"calories\": "+this.calories+"," +
                "\"intensity\": \""+this.intensity+"\"," +
                "\"triggerMethod\": \""+this.triggerMethod+"\"," +
                "\"averageHeartRate\": "+this.averageHeartRate+"," +
                "\"maxHeartRate\": "+this.maxHeartRate+"," +
                this.track.getJson() +
                this.accInfo.getJson() +
                "}";
        return json;
    }

}