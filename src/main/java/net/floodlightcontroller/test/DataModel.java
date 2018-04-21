package net.floodlightcontroller.test;

import com.google.gson.annotations.SerializedName;

public class DataModel {
    @SerializedName("TOTAL_PKT")
    protected double numberOfPackets;
    @SerializedName("PKT_SIZE_AVG")
    protected double averageSize;
    @SerializedName("RATE_ICMP")
    protected double RATE_ICMP;
    @SerializedName("P_IAT")
    protected double P_IAT;

    public DataModel(){}

    public DataModel(double numberOfPackets, double averageSize, double RATE_ICMP, double p_IAT) {
        this.numberOfPackets = numberOfPackets;
        this.averageSize = averageSize;
        this.RATE_ICMP = RATE_ICMP;
        P_IAT = p_IAT;
    }

    public double getNumberOfPackets() {
        return numberOfPackets;
    }

    public void setNumberOfPackets(double numberOfPackets) {
        this.numberOfPackets = numberOfPackets;
    }

    public double getAverageSize() {
        return averageSize;
    }

    public void setAverageSize(double averageSize) {
        this.averageSize = averageSize;
    }

    public double getRATE_ICMP() {
        return RATE_ICMP;
    }

    public void setRATE_ICMP(double RATE_ICMP) {
        this.RATE_ICMP = RATE_ICMP;
    }

    public double getP_IAT() {
        return P_IAT;
    }

    public void setP_IAT(double p_IAT) {
        P_IAT = p_IAT;
    }
}
