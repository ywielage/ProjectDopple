package com.inf2c.doppleapp.jsonConversion.models.Acc;

import java.util.ArrayList;

public class ProccesedAccData {
    private int stepFrequency;
    private int contactTime;
    private ArrayList<AccData> accDataArraylist;

    public ProccesedAccData(int stepFrequency, int contactTime) {
        this.stepFrequency = stepFrequency;
        this.contactTime = contactTime;
        this.accDataArraylist = new ArrayList<>();
    }

    public String getJson() {
        String json = "{" +
                "\"stepFrequency\": "+this.stepFrequency+"," +
                "\"contactTime\": "+this.contactTime+"," +
                "\"accData\": [";

        for(int i = 0; i < this.accDataArraylist.size(); i++) {
            AccData accData = this.accDataArraylist.get(i);
            json += accData.getJson();
            if(this.accDataArraylist.size() > 1 && i != (this.accDataArraylist.size() - 1)) {
                json += ",";
            }
        }

        json += "]}";
        return json;
    }

    public int getStepFrequency() {
        return stepFrequency;
    }

    public void setStepFrequency(int stepFrequency) {
        this.stepFrequency = stepFrequency;
    }

    public int getContactTime() {
        return contactTime;
    }

    public void setContactTime(int contactTime) {
        this.contactTime = contactTime;
    }

    public ArrayList<AccData> getAccDataArraylist() {
        return accDataArraylist;
    }

    public void addAccData(AccData accData) {
        this.accDataArraylist.add(accData);
    }
}
