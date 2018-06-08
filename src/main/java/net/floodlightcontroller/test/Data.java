package net.floodlightcontroller.test;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("MaxRateProtol")
    protected String maxRateProtol;
    @SerializedName("PpF")
    protected double PPF;
    @SerializedName("IAT")
    protected double P_IAT;

    public Data() {
    }

    public Data(String maxRateProtol, double PPF, double p_IAT) {
        this.maxRateProtol = maxRateProtol;
        this.PPF = PPF;
        P_IAT = p_IAT;
    }

    public String getMaxRateProtol() {
        return maxRateProtol;
    }

    public void setMaxRateProtol(String maxRateProtol) {
        this.maxRateProtol = maxRateProtol;
    }

    public double getPPF() {
        return PPF;
    }

    public void setPPF(double PPF) {
        this.PPF = PPF;
    }

    public double getP_IAT() {
        return P_IAT;
    }

    public void setP_IAT(double p_IAT) {
        P_IAT = p_IAT;
    }
}