package net.floodlightcontroller.test;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import net.floodlightcontroller.OCSVM.DataPoint;
import net.floodlightcontroller.OCSVM.OneclassSVM;
import net.floodlightcontroller.accesscontrollist.ACLRule;
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
    private IACLService iaclService;

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
        iaclService = context.getServiceImpl(IACLService.class);
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
            ObjectInputStream in = new ObjectInputStream(connSocket.getInputStream());

            byte[] data = new byte[1500];
            try {
                while ((in.read(data)) != -1) {

                    // Xử lý dữ liệu đầu vào được gửi từ analyzer
                    StringBuilder json = new StringBuilder();
                    for(int i = 0;i < data.length;i++) {
                        if(data[i] != 0){
                            json.append(new Character((char)data[i]));
                            if('}' == (char)data[i]){
                                break;
                            }
                        }
                    }
                    System.out.println(json);
                    if (json.toString().contains("null")) continue;
                    DataModel dataModel = gson.fromJson(json.toString(),DataModel.class);

                    //Module OCSVM và giải pháp dropICMP
                    OCSVM(dataModel.getTOTAL_PKT(),dataModel.getPKT_SIZE_AVG());

                    //Module Fuzzy và giải pháp xóa z % flow ưu tiên flow có 1 packet
                    Fuzzy(dataModel.getPPF(),dataModel.getP_IAT());

                    //Module DNS và giải pháp chặn bản tin DNS response
                    DNS(dataModel.getRATE_DNSRESPONE(),dataModel.getTOTAL_DNSRESPONE());
                }
            } catch (IOException e) {
                log.info("Cannot communicate to client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            doDropFlowICMP();
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
                numberFlow = list.size();
                for (OFFlowStatsEntry fse : list) {
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
}
