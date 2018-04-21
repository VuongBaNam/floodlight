package net.floodlightcontroller.test;

import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ClientSocket implements IFloodlightModule {

    private static final Logger log = LoggerFactory.getLogger(ClientSocket.class);
    private final static int DEFAULT_PORT = 9999;
    private static ServerSocket servSocket;
    private Socket socket;
    private static IOFSwitchService switchService;

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
        servSocket = new ServerSocket(DEFAULT_PORT);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        System.out.println("Start server Socket");
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        while (true){
            try{
                socket = servSocket.accept();
                //communicate(socket);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }
    private void communicate(Socket connSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(connSocket.getInputStream());

            double z;
            try {
                while ((z = in.readDouble()) != -1) {
                    System.out.println(z);
                    sendFlowDeleteMessage(z);
                }
            } catch (IOException e) {
                System.out.println("Cannot communicate to client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFlowDeleteMessage(double z) {

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
    public static void sort(List<OFFlowStatsEntry> IP) {
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

    public static void heapify(List<OFFlowStatsEntry> IP, int n,int i) {
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

    Map<DatapathId, List<OFStatsReply>> getAllFlowStatistics(Set<DatapathId> dpids ){
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
