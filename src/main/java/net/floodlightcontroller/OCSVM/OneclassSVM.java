package net.floodlightcontroller.OCSVM;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import java.io.IOException;

public class OneclassSVM {

    //Trường hợp thống kê 6s
    private final DataPoint_extreme max = new DataPoint_extreme(702, 880.6234);
    private final DataPoint_extreme min = new DataPoint_extreme(18, 73.0749);

//    //Trường hợp thống kê 3s
//    private final DataPoint_extreme max = new DataPoint_extreme(671, 1026);
//    private final DataPoint_extreme min = new DataPoint_extreme(1, 46);
    private svm_model svmModel;

    public OneclassSVM() {
        setSvmModel();
    }

    public DataPoint_extreme getMax() {
        return max;
    }

    public DataPoint_extreme getMin() {
        return min;
    }

    public svm_model getSvmModel() {
        return svmModel;
    }

    private void setSvmModel(){
        try {
            svmModel = svm.svm_load_model("E:\\hoc tap\\floodlight\\mySVMmodel_6s_DTL.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double predict(DataPoint point){
        if(point.getNumberOfPackets() == 0 || point.getAverageSize() == 0) return 1;
        point.scale(max, min);
        svm_node[] nodes = new svm_node[2];

        svm_node node1 = new svm_node();
        node1.index = 0;
        node1.value = point.getNumberOfPackets();

        svm_node node2 = new svm_node();
        node2.index = 1;
        node2.value = point.getAverageSize();

        nodes[0] = node1;
        nodes[1] = node2;

        double result = svm.svm_predict(svmModel, nodes);
//        if(result > 0) System.out.println("Predict: Normal");
//        else System.out.println("Predict: Abnormal");
        return result;
    }

    public double[] predict(DataPoint[] points) {
        double[] result = new double[points.length];
        int count_normal = 0;
        for(int i = 0; i < points.length; i++) {
//            System.out.print("Point " + String.valueOf(i));
            result[i] = predict(points[i]);
            if(result[i] == 1) count_normal ++;
        }
//        System.out.println("Normal: " + count_normal + "/" + points.length);
//        System.out.println("Abnormal: " + (points.length - count_normal) + "/" + points.length);
        return result;
    }
}
