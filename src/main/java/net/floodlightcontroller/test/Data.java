package net.floodlightcontroller.test;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("ENTROPY_IP_SRC")
    double ENTROPY_IP_SRC;
    @SerializedName("ENTROPY_PORT_SRC")
    double ENTROPY_PORT_SRC;
    @SerializedName("ENTROPY_PORT_DST")
    double ENTROPY_PORT_DST;
    @SerializedName("ENTROPY_PROTOCOL")
    double ENTROPY_PROTOCOL;
    @SerializedName("total_pkt")
    long total_pkt;

    public Data(double ENTROPY_IP_SRC, double ENTROPY_PORT_SRC, double ENTROPY_PORT_DST, double ENTROPY_PROTOCOL,long total_pkt) {
        this.ENTROPY_IP_SRC = ENTROPY_IP_SRC;
        this.ENTROPY_PORT_SRC = ENTROPY_PORT_SRC;
        this.ENTROPY_PORT_DST = ENTROPY_PORT_DST;
        this.ENTROPY_PROTOCOL = ENTROPY_PROTOCOL;
        this.total_pkt = total_pkt;
    }

    public long getTotal_pkt() {
        return total_pkt;
    }

    public void setTotal_pkt(long total_pkt) {
        this.total_pkt = total_pkt;
    }

    public double getENTROPY_IP_SRC() {
        return ENTROPY_IP_SRC;
    }

    public void setENTROPY_IP_SRC(double ENTROPY_IP_SRC) {
        this.ENTROPY_IP_SRC = ENTROPY_IP_SRC;
    }

    public double getENTROPY_PORT_SRC() {
        return ENTROPY_PORT_SRC;
    }

    public void setENTROPY_PORT_SRC(double ENTROPY_PORT_SRC) {
        this.ENTROPY_PORT_SRC = ENTROPY_PORT_SRC;
    }

    public double getENTROPY_PORT_DST() {
        return ENTROPY_PORT_DST;
    }

    public void setENTROPY_PORT_DST(double ENTROPY_PORT_DST) {
        this.ENTROPY_PORT_DST = ENTROPY_PORT_DST;
    }

    public double getENTROPY_PROTOCOL() {
        return ENTROPY_PROTOCOL;
    }

    public void setENTROPY_PROTOCOL(double ENTROPY_PROTOCOL) {
        this.ENTROPY_PROTOCOL = ENTROPY_PROTOCOL;
    }
}