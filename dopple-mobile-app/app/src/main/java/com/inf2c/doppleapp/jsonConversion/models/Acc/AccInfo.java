package com.inf2c.doppleapp.jsonConversion.models.Acc;

import java.util.ArrayList;

public class AccInfo {
    private String timeStamp;
    private int steps;
    private int averageContactTime;
    private int averageStepFrequentie;
    private ArrayList<ProccesedAccData> proccesedAccData;

    public AccInfo(String timeStamp, int steps, ArrayList<Integer> contactTimes, ArrayList<Integer> stepFrequencies, ArrayList<ProccesedAccData> proccesedAccDataList) {
        this.timeStamp = timeStamp;
        this.steps = steps;
        this.setAverageContactTime(contactTimes);
        this.setAverageStepFrequency(stepFrequencies);
        this.proccesedAccData = proccesedAccDataList;
    }

    public String getJson() {
        String json =
                "\"accInfo\": [{" +
                "\"timeStamp\": \""+this.timeStamp+"\"," +
                "\"steps\": "+this.steps+"," +
                "\"averageContactTime\": "+this.averageContactTime+"," +
                "\"averageStepFrequency\": "+this.averageStepFrequentie+"," +
                "\"processedAccData\" : [";

        for(int i = 0; i < proccesedAccData.size(); i++) {
            ProccesedAccData pAccData = this.proccesedAccData.get(i);
            json += pAccData.getJson();
            if(this.proccesedAccData.size() > 1 && i != (this.proccesedAccData.size() - 1)) {
                json += ",";
            }
        }
        json += "]}]";

        return json;
    }

    private void setAverageContactTime(ArrayList<Integer> list) {
        double total = 0;
        for(Integer contactTime : list) {
            total += contactTime;
        }
        this.averageContactTime = (int) (total / list.size());
    }

    private void setAverageStepFrequency(ArrayList<Integer> list) {
        double total = 0;
        for(Integer stepFrequency : list) {
            total += stepFrequency;
        }
        this.averageStepFrequentie = (int) (total / list.size());
    }

    public void addAccData(ProccesedAccData proccesedAccData) {
        this.proccesedAccData.add(proccesedAccData);
    }

    public ArrayList<ProccesedAccData> getAccDataInfo() {
        return this.proccesedAccData;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public double getAverageContactTime() {
        return averageContactTime;
    }

    public double getAverageStepFrequentie() {
        return averageStepFrequentie;
    }

}
