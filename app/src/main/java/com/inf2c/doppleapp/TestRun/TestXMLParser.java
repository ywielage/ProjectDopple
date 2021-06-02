package com.inf2c.doppleapp.TestRun;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class TestXMLParser {


    /*
    Test run code

    InputStream object = this.getResources().openRawResource(R.raw.dopple_session_20210511164705_1);
    TestXMLParser parser = new TestXMLParser();

    List<Trackpoint> list = parser.parse(object);
    Calculations calculations = new Calculations();

    System.out.println("==============================");
    System.out.println(list.get(0).getEarbudsTimeStamp());
    System.out.println(calculations.getStepFreqs(list));
    System.out.println("Distance in km: " + calculations.getTotalDistance(list));
    System.out.println("Items in list: " + list.size());
    System.out.println("==============================");
     */

    private List<Trackpoint> trackpoints = new ArrayList<Trackpoint>();
    private Trackpoint trackpoint;
    private String text;

    public List<Trackpoint> getTrackpoints() {
        return trackpoints;
    }

    public List<Trackpoint> parse(InputStream is) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser  parser = factory.newPullParser();

            parser.setInput(is, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagname = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (tagname.equalsIgnoreCase("Trackpoint")) {
                            // create a new instance of employee
                            trackpoint = new Trackpoint();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if(trackpoint != null && !text.equals("")) {
                            switch(tagname) {
                                case "Trackpoint":
                                    trackpoints.add(trackpoint);
                                    break;
                                case "Time":
                                    trackpoint.setTime(Long.parseLong(text));
                                    break;
                                case "LongitudeDegrees":
                                    trackpoint.setLongitudeDegrees(Float.parseFloat(text));
                                    break;
                                case "LatitudeDegrees":
                                    trackpoint.setLatitudeDegrees(Float.parseFloat(text));
                                    break;
//                                case "AltitudeMeters":
//                                    trackpoint.setAltitudeMeters(Float.parseFloat(text));
//                                    break;
//                                case "DistanceMeters":
//                                    trackpoint.setDistanceMeters(Float.parseFloat(text));
//                                    break;
                                case "Value":
                                    trackpoint.setHeartRateBpm(Integer.parseInt(text));
                                    break;
                                case "ContactTime":
                                    trackpoint.setContactTime(Integer.parseInt(text));
                                    break;
                                case "EarbudsTimestamp":
                                    trackpoint.setEarbudsTimeStamp(Float.parseFloat(text));
                                    break;
                                case "StepFrequency":
                                    trackpoint.setStepFrequency(Integer.parseInt(text));
                                    break;
                                case "Steps":
                                    trackpoint.setSteps(Integer.parseInt(text));
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {e.printStackTrace();}
        catch (IOException e) {e.printStackTrace();}

        return trackpoints;
    }
}
