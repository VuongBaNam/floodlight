package net.floodlightcontroller.test;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TestModule implements IFloodlightModule, IOFMessageListener {

    protected static ServerSocket serverSocket;
    protected static final int serverPort = 50000;

    protected static List<Match> listMatch = new ArrayList<>();
    protected IFloodlightProviderService floodlightProvider;
    protected static final Logger logger = LoggerFactory.getLogger(TestModule.class);
    protected static IOFSwitchService switchService;


    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        switchService = context.getServiceImpl(IOFSwitchService.class);
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);

        try {
            serverSocket = new ServerSocket(serverPort);
            logger.info("[+] SOCKET SERVER STARTED AT PORT {}", serverPort);
        } catch (Exception e) {
            logger.info("[!] CANNOT START SOCKET SERVER");
        }

    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {

        while (true){
            try{
                Socket connSocket = serverSocket.accept();
                communicate(connSocket);
            } catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private static void communicate(Socket connSocket){
        try {
            ObjectInputStream in = new ObjectInputStream(connSocket.getInputStream());
            Map<String,Object> map;
            while(( map =  (Map<String,Object>)in.readObject())!= null){
                if(map != null){
                    System.out.println("Received");
                    String ip_src = (String)map.get(Flow.IP_SRC.toString());

                    System.out.println("Build message: "+ip_src);

                    IOFSwitch sw = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
                    Match match;

                    match = sw.getOFFactory().buildMatch()
                            .setExact(MatchField.IPV4_SRC,IPv4Address.of(ip_src))
                            .build();
                    OFFlowDelete flowAdd = sw.getOFFactory().buildFlowDelete()
                            .setBufferId(OFBufferId.NO_BUFFER)
                            .setHardTimeout(3600)
                            .setIdleTimeout(10)
                            .setPriority(32768)
                            .setMatch(match)
                            .setTableId(TableId.of(0))
                            .build();
                    sw.write(flowAdd);
                }
            }
        } catch(IOException e){
            System.out.println("Cannot communicate to client!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
