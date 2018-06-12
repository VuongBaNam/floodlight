package net.floodlightcontroller.test;

/**
 * Created by odldev on 4/24/17.
 */
public class Parameter {
    double ENTROPY_IP_SRC;
    double ENTROPY_PORT_SRC;
    double ENTROPY_PORT_DST;
    double ENTROPY_PROTOCOL;

    public Parameter(double ENTROPY_IP_SRC, double ENTROPY_PORT_SRC, double ENTROPY_PORT_DST, double ENTROPY_PROTOCOL) {
        this.ENTROPY_IP_SRC = ENTROPY_IP_SRC;
        this.ENTROPY_PORT_SRC = ENTROPY_PORT_SRC;
        this.ENTROPY_PORT_DST = ENTROPY_PORT_DST;
        this.ENTROPY_PROTOCOL = ENTROPY_PROTOCOL;
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
