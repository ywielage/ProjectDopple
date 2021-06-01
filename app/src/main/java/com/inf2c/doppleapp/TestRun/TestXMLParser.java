package com.inf2c.doppleapp.TestRun;

import com.inf2c.doppleapp.jsonConversion.models.Track.Track;

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

    System.out.println("==============================");
    List list = parser.parse(object);
    System.out.println("==============================" + list.size());
     */

    private List<Trackpoint> employees= new ArrayList<Trackpoint>();
    private Trackpoint employee;
    private String text;

    public List<Trackpoint> getEmployees() {
        return employees;
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
                            employee = new Trackpoint();
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagname.equalsIgnoreCase("Trackpoint")) {
                            // add employee object to list
                            employees.add(employee);
                        }else if (tagname.equalsIgnoreCase("Time")) {
                            employee.setId(Integer.parseInt(text));
                        }  else if (tagname.equalsIgnoreCase("LongitudeDegrees")) {
                            employee.setName(text);
                        } else if (tagname.equalsIgnoreCase("LatitudeDegrees")) {
                            employee.setSalary(Float.parseFloat(text));
                        }
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {e.printStackTrace();}
        catch (IOException e) {e.printStackTrace();}

        return employees;
    }
}
