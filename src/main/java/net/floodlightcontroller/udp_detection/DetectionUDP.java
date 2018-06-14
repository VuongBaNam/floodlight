package net.floodlightcontroller.udp_detection;

import Jama.Matrix;
import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.test.Parameter;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DetectionUDP implements IFloodlightModule {
    private IOFSwitchService switchService;
    private static long start = 0;
    private static Map<OFFlowStatsEntry,Integer> listFlow1;
    private static Map<OFFlowStatsEntry,Integer> listFlow2;
    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IOFSwitchService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException, IOException {
        switchService = context.getServiceImpl(IOFSwitchService.class);
        start = System.currentTimeMillis();
        listFlow1 = new HashMap<>();
        listFlow2 = new HashMap<>();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        while (true){
            long end = System.currentTimeMillis();
            if(end - start < 5000){
                Map<DatapathId, List<OFStatsReply>> replies = getAllFlowStatistics(switchService.getAllSwitchDpids());
                for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
                    for (OFStatsReply r : e.getValue()) {
                        OFFlowStatsReply fsr = (OFFlowStatsReply) r;
                        List<OFFlowStatsEntry> list = fsr.getEntries();
                        for (OFFlowStatsEntry fse : list) {
                            listFlow2.put(fse,1);
                        }
                    }
                }
            }else {
                List<OFFlowStatsEntry> listNewFlow = new ArrayList<>();
                for (Map.Entry<OFFlowStatsEntry, Integer> e : listFlow2.entrySet()) {
                    if(listFlow1.get(e.getKey()) == null){
                        listNewFlow.add(e.getKey());
                    }
                }
                Parameter parameter = getParameter(listNewFlow);


                double[] input = {parameter.getENTROPY_IP_SRC(), parameter.getENTROPY_PORT_SRC(), parameter.getENTROPY_PORT_DST(), parameter.getENTROPY_PROTOCOL(),parameter.getTotal_pkt()};
                System.out.println("hung hung hung" + input[0]);

                //chạy thuật toán
                input = normalization(input);
                KNN knnAlgorithm = new KNN(3);
                Matrix udp_dataset = knnAlgorithm.readFile("E:\\hoc tap\\floodlight\\udp_dataset.csv");
                int result = knnAlgorithm.Calculate(udp_dataset, input);
                if(result == 1) {
                    System.out.println("Attack");
                    System.out.println("Attack");
                    System.out.println("Attack");
                    System.out.println("Attack");
                }

                listFlow1.clear();
                listFlow1.putAll(listFlow2);
                listFlow2.clear();
                start = end;
            }
        }
    }

    private Parameter getParameter(List<OFFlowStatsEntry> listNewFlow){
        Map<String,Long> ip_src = new HashMap<>();
        Map<Integer,Long> port_src = new HashMap<>();
        Map<Integer,Long> port_dst = new HashMap<>();
        Map<String,Long> protocol = new HashMap<>();
        long total = 0;
        for(OFFlowStatsEntry entry : listNewFlow){
            Match match = entry.getMatch();

            total += entry.getPacketCount().getValue();
            IPv4Address ip_s = match.get(MatchField.IPV4_SRC);
            TransportPort port_s = TransportPort.of(0);
            TransportPort port_d = TransportPort.of(0);
            String pro = "ICMP";
            IpProtocol proto = match.get(MatchField.IP_PROTO);
            if(proto.getIpProtocolNumber() == IpProtocol.TCP.getIpProtocolNumber()){
                port_s = match.get(MatchField.TCP_SRC);
                port_d = match.get(MatchField.TCP_DST);
                pro = "TCP";
            }else if(proto.getIpProtocolNumber() == IpProtocol.UDP.getIpProtocolNumber()){
                port_s = match.get(MatchField.UDP_SRC);
                port_d = match.get(MatchField.UDP_DST);
                pro = "UDP";
            }
            long number_pkt = entry.getPacketCount().getValue();
            if(ip_src.get(ip_s.toString()) == null){
                ip_src.put(ip_s.toString(),number_pkt);
            }else ip_src.put(ip_s.toString(),ip_src.get(ip_s.toString())+number_pkt);

            if(port_src.get(port_s.getPort()) == null){
                port_src.put(port_s.getPort(),number_pkt);
            }else port_src.put(port_s.getPort(),port_src.get(port_s.getPort())+number_pkt);

            if(port_dst.get(port_d.getPort()) == null){
                port_dst.put(port_d.getPort(),number_pkt);
            }else port_dst.put(port_d.getPort(),port_dst.get(port_d.getPort())+number_pkt);

            if(protocol.get(pro) == null){
                protocol.put(pro,number_pkt);
            }else protocol.put(pro,protocol.get(pro)+number_pkt);
        }
        Parameter parameter = new Parameter(entropy(ip_src),entropy(port_src),entropy(port_dst),entropy(protocol),total);
        return parameter;
    }

    private double entropy(Map<? extends Object,Long> map){
        long sum = 0;
        double entro = 0;
        for(Map.Entry<? extends Object,Long> entry : map.entrySet()){
            sum += entry.getValue();
        }
        for(Map.Entry<? extends Object,Long> entry : map.entrySet()){
            double p = entry.getValue()*1.0/sum;

            entro += p*Math.log(p)/Math.log(2);
        }
        return Math.abs(entro);
    }

    private Map<DatapathId, List<OFStatsReply>> getAllFlowStatistics(Set<DatapathId> dpids ){
        Map<DatapathId, List<OFStatsReply>> map = new HashMap<>();
        for (DatapathId d : dpids) {
            List<OFStatsReply> list = getFlowStatistics(d);
            map.put(d,list);
        }
        return map;
    }

    protected List<OFStatsReply> getFlowStatistics(DatapathId switchId ) {
        IOFSwitch sw = switchService.getSwitch(switchId);
        ListenableFuture<?> future;
        List<OFStatsReply> values = null;
        Match match = sw.getOFFactory().buildMatch().build();
        OFStatsRequest<?> req = sw.getOFFactory().buildFlowStatsRequest()
                .setMatch(match)
                .setOutPort(OFPort.ANY)
                .setTableId(TableId.ALL)
                .build();

        try {
            if (req != null) {
                future = sw.writeStatsRequest(req);
                values = (List<OFStatsReply>) future.get(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            return new ArrayList<OFStatsReply>();
        }
        return values;
    }
    private double[] normalization(double[] features){
        // 5 feature: entropy of IP source, port source, port des, packet type, total packet
        double[] maxFeature = {12.688703, 12.433445, 0.117221, 0.133031, 13218.000000};
        double[] minFeature = {6.606806, 7.539159, 0.000000, 0.000000, 186.000000};
        for(int i = 0; i < features.length; i++){
            features[i] = (features[i] - maxFeature[i])/(maxFeature[i] - minFeature[i]);
        }
        return features;
    }
}