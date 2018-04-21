package net.floodlightcontroller.OCSVM;

public class DataPoint_extreme extends DataPoint {
    public DataPoint_extreme() {

    }

    public DataPoint_extreme(double numberOfPackets, double averageSize) {
        super(numberOfPackets, averageSize);
    }

    @Override
    public void setNumberOfPackets(double numberOfPackets) {
        super.setNumberOfPackets(numberOfPackets);
    }

    @Override
    public void setAverageSize(double averageSize) {
        super.setAverageSize(averageSize);
    }

    @Override
    public void setScaled(boolean scaled) {
        super.setScaled(scaled);
    }

    @Override
    public double getNumberOfPackets() {
        return super.getNumberOfPackets();
    }

    @Override
    public double getAverageSize() {
        return super.getAverageSize();
    }

    @Override
    public void scale(DataPoint max, DataPoint min) {

    }
}
