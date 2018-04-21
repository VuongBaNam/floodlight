package net.floodlightcontroller.OCSVM;
import com.google.gson.annotations.SerializedName;

public class DataPoint {
    @SerializedName("TOTAL_PKT")
    protected double numberOfPackets;
    @SerializedName("PKT_SIZE_AVG")
    protected double averageSize;
    protected boolean isScaled;

    public DataPoint() {
        isScaled = false;
    }

    public DataPoint(double numberOfPackets, double averageSize) {
        this.numberOfPackets = numberOfPackets;
        this.averageSize = averageSize;
        this.isScaled = false;
    }

    public void setNumberOfPackets(double numberOfPackets) {
        this.numberOfPackets = numberOfPackets;
    }

    public void setAverageSize(double averageSize) {
        this.averageSize = averageSize;
    }

    public void setScaled(boolean scaled) {
        isScaled = scaled;
    }

    public double getNumberOfPackets() {
        return numberOfPackets;
    }

    public double getAverageSize() {
        return averageSize;
    }

    private boolean isScaled() {
        return isScaled;
    }

    public void scale(DataPoint max, DataPoint min){
        if(!isScaled()) {
            double numberOfPackets_gap = max.numberOfPackets - min.numberOfPackets;
            double averageSize_gap = max.averageSize - min.averageSize;

            this.numberOfPackets = (this.numberOfPackets - min.numberOfPackets)/numberOfPackets_gap;
            this.averageSize = (this.averageSize - min.averageSize)/averageSize_gap;

            this.isScaled = true;
        }
    }
}
