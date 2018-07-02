package net.floodlightcontroller.test;

import Jama.Matrix;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import net.floodlightcontroller.OCSVM.DataPoint;
import net.floodlightcontroller.OCSVM.OneclassSVM;
import net.floodlightcontroller.accesscontrollist.IACLService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.forwarding.Forwarding;
import net.floodlightcontroller.fuzzy.Fuzzy;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.udp_detection.KNN;
import net.floodlightcontroller.util.FlowModUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientSocket implements IFloodlightModule {

    private boolean check = false;
    private static final Logger log = LoggerFactory.getLogger(ClientSocket.class);
    private final static int DEFAULT_PORT = 5000;
    private static ServerSocket servSocket;
    private Socket socket;
    private Gson gson;
    private IOFSwitchService switchService;

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
        l.add(IThreadPoolService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException, IOException {
        gson = new Gson();
        servSocket = new ServerSocket(DEFAULT_PORT);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        log.info("Start server Socket");
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        while (true){
            try{
                socket = servSocket.accept();
                communicate(socket);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        }

    }
    private void communicate(Socket connSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(connSocket.getInputStream()));
            try {
                while (true) {
                    // Xử lý dữ liệu đầu vào được gửi từ analyzer
                    String json = in.readLine();
//                    System.out.println(json);
                    if (json == null) break;
                    if (json.contains("null")) continue;
                    String a[] = json.split("@");
                    if(a[0].equals("5S")){
                        Data dataModel = gson.fromJson(a[1], Data.class);
//                        detectUDP(dataModel);
                    }else {
                        DataModel dataModel = gson.fromJson(a[1], DataModel.class);

                        //drop ip tấn công http get
                        doDropIPAttack(dataModel.getList());

//                        Module OCSVM và giải pháp dropICMP
//                        OCSVM(dataModel.getTOTAL_PKT(), dataModel.getPKT_SIZE_AVG());
//
//                        //Module Fuzzy và giải pháp xóa z % flow ưu tiên flow có 1 packet
//                        Fuzzy(dataModel.getPPF(), dataModel.getP_IAT());
//
//                        //Module DNS và giải pháp chặn bản tin DNS response
//                        DNS(dataModel.getRATE_DNSRESPONE(), dataModel.getTOTAL_DNSRESPONE());
                    }
                }
            } catch (IOException e) {
                log.info("Cannot communicate to client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void detectUDP(Data dataModel){
        double[] input = {dataModel.getENTROPY_IP_SRC(), dataModel.getENTROPY_PORT_SRC(), dataModel.getENTROPY_PORT_DST(), dataModel.getENTROPY_PROTOCOL(),dataModel.getTotal_pkt()};

        //chạy thuật toán
        input = normalization(input);
        for(int i = 0;i < input.length;i++){
            System.out.print(input[i] +" ");
        }
        System.out.println();
        KNN knnAlgorithm = new KNN(3);
        Matrix udp_dataset = knnAlgorithm.readFile("E:\\hoc tap\\floodlight\\udp_dataset.csv");
        int result = knnAlgorithm.Calculate(udp_dataset, input);
        if(result == 1) {
            System.out.println("Attack");
        }else {
            System.out.println("Normal");
        }
    }

    private void doDropIPAttack(List<String> ip){
        for(DatapathId datapathId : switchService.getAllSwitchDpids()){
            IOFSwitch sw = switchService.getSwitch(datapathId);
            OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
            for(String IP : ip){
                Match match = sw.getOFFactory().buildMatch()
                        .setExact(MatchField.ETH_TYPE,EthType.IPv4)
                        .setExact(MatchField.IPV4_SRC,IPv4Address.of(IP))
                        .build();
                List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
                fmb.setMatch(match).setIdleTimeout(30).setPriority(1000);//drop ip attack trong 30s

                FlowModUtils.setActions(fmb, actions, sw);

                sw.write(fmb.build());
            }
        }
    }

    private void DNS(double rateDNS,double totalDNS){
        if(rateDNS > 0.5 || totalDNS > 6000){
            log.info("Attack DNS");
            doDropFlowDNS();
        }
    }

    private void OCSVM(double numberOfPackets, double averageSize){
        DataPoint dataPoint = new DataPoint(numberOfPackets,averageSize);
        OneclassSVM oneclassSVM = new OneclassSVM();
        double result = oneclassSVM.predict(dataPoint);
        if(result > 0){
            log.info("Normal");
        }else {
//            doDropFlowICMP();
            sendFlowDeleteMessage();
            log.info("Abnormal");
        }
    }

    private void Fuzzy(double PPF,double P_IAT){
        double z = Fuzzy.FIS(PPF,P_IAT);
        if(z > 0.95){
            sendTableDeleteMessage();
        }else{
            sendFlowDeleteMessage(z);
        }
    }

    private void doDropFlowDNS(){
        for(DatapathId datapathId : switchService.getAllSwitchDpids()){
            IOFSwitch sw = switchService.getSwitch(datapathId);
            OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
            Match match = sw.getOFFactory().buildMatch()
                    .setExact(MatchField.ETH_TYPE,EthType.IPv4)
                    .setExact(MatchField.UDP_SRC,TransportPort.of(53))
                    .build();
            List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
            fmb.setMatch(match).setIdleTimeout(Forwarding.FLOWMOD_DEFAULT_IDLE_TIMEOUT);

            FlowModUtils.setActions(fmb, actions, sw);

            sw.write(fmb.build());
        }
    }

    private void doDropFlowICMP(){
        for(DatapathId datapathId : switchService.getAllSwitchDpids()){
            IOFSwitch sw = switchService.getSwitch(datapathId);
            OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
            Match match = sw.getOFFactory().buildMatch()
                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                    .setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
                    .build();
            List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
            fmb.setMatch(match).setIdleTimeout(Forwarding.FLOWMOD_DEFAULT_IDLE_TIMEOUT).setPriority(1000);

            FlowModUtils.setActions(fmb, actions, sw);

            sw.write(fmb.build());
        }
    }

    private void sendTableDeleteMessage() {
        Set<DatapathId> datapathIds = switchService.getAllSwitchDpids();
        for(DatapathId datapathId : datapathIds) {
            IOFSwitch sw = switchService.getSwitch(datapathId);

            OFFlowDelete flowDelete = sw.getOFFactory().buildFlowDelete()
                    .setOutPort(OFPort.ANY).build();
            sw.write(flowDelete);
        }
    }

    private void sendFlowDeleteMessage(double z) {
        if(z == 0){
            return;
        }
        Map<DatapathId, List<OFStatsReply>> replies = getAllFlowStatistics(switchService.getAllSwitchDpids());
        int numberFlowDeleted = 0;
        int numberFlow = 0;
        for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
            for (OFStatsReply r : e.getValue()) {
                OFFlowStatsReply fsr = (OFFlowStatsReply) r;
                List<OFFlowStatsEntry> list = fsr.getEntries();
                sort(list);
                for (OFFlowStatsEntry fse : list) {
                    numberFlow++;
                    Match match = fse.getMatch();

                    if(numberFlowDeleted*1.0/numberFlow < z){
                        IOFSwitch sw = switchService.getSwitch(e.getKey());

                        OFFlowDelete flowDelete = sw.getOFFactory().buildFlowDelete()
                                .setOutPort(OFPort.ANY)
                                .setMatch(match).build();
                        sw.write(flowDelete);
                        numberFlowDeleted++;
                    }
                }
            }
        }
        System.out.println("==========================");
        System.out.println(numberFlowDeleted+" "+numberFlow);
    }

    private void sendFlowDeleteMessage() {
        Map<DatapathId, List<OFStatsReply>> replies = getAllFlowStatistics(switchService.getAllSwitchDpids());
        int numberFlowDeleted = 0;
        int numberFlow = 0;
        for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
            for (OFStatsReply r : e.getValue()) {
                OFFlowStatsReply fsr = (OFFlowStatsReply) r;
                List<OFFlowStatsEntry> list = fsr.getEntries();
                sort(list);
                for (OFFlowStatsEntry fse : list) {
                    numberFlow++;
                    Match match = fse.getMatch();

                    if(fse.getPacketCount().getValue() == 0){
                        numberFlowDeleted++;
                        IOFSwitch sw = switchService.getSwitch(e.getKey());

                        OFFlowDelete flowDelete = sw.getOFFactory().buildFlowDelete()
                                .setOutPort(OFPort.ANY)
                                .setMatch(match).build();
                        sw.write(flowDelete);
                    }
                }
            }
        }
        System.out.println("==========================");
        System.out.println(numberFlowDeleted+" "+numberFlow);
    }

    private static void sort(List<OFFlowStatsEntry> IP) {
        int n = IP.size();
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(IP, n, i);
        }

        // Heap sort
        for (int i = n - 1; i >= 0; i--) {
            OFFlowStatsEntry temp = IP.get(0);
            OFFlowStatsEntry temp2 = IP.get(i);


            OFFlowStatsEntry.Builder builder = IP.get(i).createBuilder();
            builder.setCookie(temp.getCookie());
            builder.setPacketCount(temp.getPacketCount());

            OFFlowStatsEntry.Builder builder2 = IP.get(0).createBuilder();
            builder2.setCookie(temp2.getCookie());
            builder2.setPacketCount(temp2.getPacketCount());
            builder.build();
            builder2.build();
            // Heapify root element
            heapify( IP, i, 0);
        }
    }

    private static void heapify(List<OFFlowStatsEntry> IP, int n,int i) {
        int largest = i;
        int l = 2 * i + 1; // ben trai
        int r = 2 * i + 2;  // ben phai
        if (l < n && IP.get(l).getPacketCount().getValue() > IP.get(largest).getPacketCount().getValue()) {
            largest = l;
        }
        if (r < n && IP.get(r).getPacketCount().getValue() > IP.get(largest).getPacketCount().getValue()) {
            largest = r;
        }
        if (largest != i) {
            OFFlowStatsEntry temp = IP.get(largest);
            OFFlowStatsEntry temp2 = IP.get(i);

            OFFlowStatsEntry.Builder builder = IP.get(i).createBuilder();
            builder.setCookie(temp.getCookie());
            builder.setPacketCount(temp.getPacketCount());

            OFFlowStatsEntry.Builder builder2 = IP.get(largest).createBuilder();
            builder2.setCookie(temp2.getCookie());
            builder2.setPacketCount(temp2.getPacketCount());
            builder.build();
            builder2.build();

            heapify(IP, n, largest);
        }
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
            log.error("Failure retrieving statistics from switch {}. {}", sw, e);
            return new ArrayList<OFStatsReply>();
        }
        return values;
    }

    private double[] normalization(double[] features){
        // 5 feature: entropy of IP source, port source, port des, packet type, total packet
        double[] maxFeature = {12.688703, 12.433445, 0.117221, 0.133031, 13218.000000};
        double[] minFeature = {6.606806, 7.539159, 0.000000, 0.000000, 186.000000};
        for(int i = 0; i < features.length; i++){
            features[i] = (features[i] - minFeature[i])/(maxFeature[i] - minFeature[i]);
        }
        return features;
    }
}
