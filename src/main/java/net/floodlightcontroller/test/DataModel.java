package net.floodlightcontroller.test;

import com.google.gson.annotations.SerializedName;

public class DataModel {
    @SerializedName("RATE_ICMP")
    double RATE_ICMP;
    @SerializedName("PPF")
    double PPF;
    @SerializedName("P_IAT")
    double P_IAT;
    @SerializedName("PKT_SIZE_AVG")
    double PKT_SIZE_AVG;
    @SerializedName("TOTAL_PKT")
    long TOTAL_PKT;
    @SerializedName("RATE_DNSRESPONE")
    double RATE_DNSRESPONE;
    @SerializedName("TOTAL_DNSRESPONE")
    long TOTAL_DNSRESPONE;

    public DataModel(double RATE_ICMP,double PPF, double p_IAT, double PKT_SIZE_AVG, long TOTAL_PKT, double RATE_DNSRESPONE, long TOTAL_DNSRESPONE) {
        this.RATE_ICMP = RATE_ICMP;
        P_IAT = p_IAT;
        this.PKT_SIZE_AVG = PKT_SIZE_AVG;
        this.TOTAL_PKT = TOTAL_PKT;
        this.RATE_DNSRESPONE = RATE_DNSRESPONE;
        this.TOTAL_DNSRESPONE = TOTAL_DNSRESPONE;
        this.PPF = PPF;
    }

    public double getPPF() {
        return PPF;
    }

    public void setPPF(double PPF) {
        this.PPF = PPF;
    }

    public double getRATE_DNSRESPONE() {
        return RATE_DNSRESPONE;
    }

    public void setRATE_DNSRESPONE(double RATE_DNSRESPONE) {
        this.RATE_DNSRESPONE = RATE_DNSRESPONE;
    }

    public long getTOTAL_DNSRESPONE() {
        return TOTAL_DNSRESPONE;
    }

    public void setTOTAL_DNSRESPONE(long TOTAL_DNSRESPONE) {
        this.TOTAL_DNSRESPONE = TOTAL_DNSRESPONE;
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

    public double getPKT_SIZE_AVG() {
        return PKT_SIZE_AVG;
    }

    public void setPKT_SIZE_AVG(double PKT_SIZE_AVG) {
        this.PKT_SIZE_AVG = PKT_SIZE_AVG;
    }

    public long getTOTAL_PKT() {
        return TOTAL_PKT;
    }

    public void setTOTAL_PKT(long TOTAL_PKT) {
        this.TOTAL_PKT = TOTAL_PKT;
    }
}
