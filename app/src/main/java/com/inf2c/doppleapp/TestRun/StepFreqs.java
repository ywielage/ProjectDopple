package com.inf2c.doppleapp.TestRun;

public class StepFreqs {
    private int minStepFreq;
    private int maxStepFreq;
    private int avgStepFreq;

    public StepFreqs(int minStepFreq, int maxStepFreq, int avgStepFreq) {
        this.minStepFreq = minStepFreq;
        this.maxStepFreq = maxStepFreq;
        this.avgStepFreq = avgStepFreq;
    }

    public int getMinStepFreq() {
        return minStepFreq;
    }

    public void setMinStepFreq(int minStepFreq) {
        this.minStepFreq = minStepFreq;
    }

    public int getMaxStepFreq() {
        return maxStepFreq;
    }

    public void setMaxStepFreq(int maxStepFreq) {
        this.maxStepFreq = maxStepFreq;
    }

    public int getAvgStepFreq() {
        return avgStepFreq;
    }

    public void setAvgStepFreq(int avgStepFreq) {
        this.avgStepFreq = avgStepFreq;
    }
}
