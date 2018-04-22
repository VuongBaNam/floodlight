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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientSocket implements IFloodlightModule {

    private boolean check = false;
    private static final Logger log = LoggerFactory.getLogger(ClientSocket.class);
    private final static int DEFAULT_PORT = 9999;
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
//                communicate(socket);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        }

    }
    private void communicate(Socket connSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(connSocket.getInputStream());

            String data;
            try {
                while ((data = in.readUTF()) != null) {
                    System.out.println(data);
                    DataModel dataModel = gson.fromJson(data,DataModel.class);
                    DataPoint dataPoint = new DataPoint(dataModel.getNumberOfPackets(),dataModel.getAverageSize());
                    OneclassSVM oneclassSVM = new OneclassSVM();
                    double result = oneclassSVM.predict(dataPoint);
                    if(result > 0){
                        log.info("Normal");
                    }else
                        log.info("Abnormal");
//                    double z = Fuzzy.FIS(dataModel.getRATE_ICMP(),dataModel.P_IAT);
//                    sendFlowDeleteMessage(z);
//                    if(dataModel.getRATE_ICMP() > 0.7 || dataModel.P_IAT > 0.9){
//                        doDropFlowICMP();
//                    }
                }
            } catch (IOException e) {
                log.info("Cannot communicate to client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doDropFlowICMP(){
        for(DatapathId datapathId : switchService.getAllSwitchDpids()){
            IOFSwitch sw = switchService.getSwitch(datapathId);
            OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
            Match match = sw.getOFFactory().buildMatch()
                    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
                    .setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
                    .build();;
            System.out.println(match.get(MatchField.IP_PROTO));
            List<OFAction> actions = new ArrayList<OFAction>(); // set no action to drop
            fmb.setMatch(match).setIdleTimeout(Forwarding.FLOWMOD_DEFAULT_IDLE_TIMEOUT);

            FlowModUtils.setActions(fmb, actions, sw);

            sw.write(fmb.build());
        }
    }

    private void sendFlowDeleteMessage(double z) {

        Map<DatapathId, List<OFStatsReply>> replies = getAllFlowStatistics(switchService.getAllSwitchDpids());
        int numberFlowDeleted = 0;
        int numberFlow = 0;
        for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
            for (OFStatsReply r : e.getValue()) {
                OFFlowStatsReply fsr = (OFFlowStatsReply) r;
                List<OFFlowStatsEntry> list = fsr.getEntries();
                System.out.println("No Sort:");
                for (OFFlowStatsEntry fse : list) {
                    System.out.println(fse.getCookie().getValue() +" "+ fse.getPacketCount().getValue());
                }
                sort(list);
                System.out.println("Sorted:");
                for (OFFlowStatsEntry fse : list) {
                    System.out.println(fse.getCookie().getValue() +" "+ fse.getPacketCount().getValue());
                }
                numberFlow = list.size();
                for (OFFlowStatsEntry fse : list) {
                    Match match = fse.getMatch();

                    if(numberFlowDeleted*1.0/numberFlow < z){
                        IOFSwitch sw = switchService.getSwitch(e.getKey());

                        OFFlowDelete flowDelete = sw.getOFFactory().buildFlowDelete()
                                .setTableId(TableId.ALL)
                                .setOutPort(OFPort.ANY)
                                .setMatch(match).build();
                        sw.write(flowDelete);
                        numberFlowDeleted++;
                    }
                }
            }
        }
        System.out.println("---------------------------------------------");
        System.out.println(numberFlowDeleted +" "+ numberFlow);
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
