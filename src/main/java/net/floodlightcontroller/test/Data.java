package net.floodlightcontroller.test;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("MaxRateProtol")
    protected double maxRateProtol;
    @SerializedName("PpF")
    protected double PPF;
    @SerializedName("IAT")
    protected double P_IAT;

    public Data() {
    }

    public Data(double maxRateProtol, double PPF, double p_IAT) {
        this.maxRateProtol = maxRateProtol;
        this.PPF = PPF;
        P_IAT = p_IAT;
    }

    public double getMaxRateProtol() {
        return maxRateProtol;
    }

    public void setMaxRateProtol(double maxRateProtol) {
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