package net.floodlightcontroller.fuzzy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;

import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.statistics.StatisticsCollector;

public class Fuzzy implements IFloodlightModule, IOFMessageListener {
	private static IOFSwitchService switchService;
	public final static int DEFAULT_PORT = 5000;
	public static ServerSocket servSocket;
	public static java.net.Socket socket;
	static Logger logger = Logger.getLogger(Fuzzy.class.getName());
	static Double Z1 = (double) 50;
	static Double Z2 = (double) 98;
	static Double Z3 = (double) 80;
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
	public void init(FloodlightModuleContext context) throws FloodlightModuleException, IOException {
		// TODO Auto-generated method stub
		// Tạo ServerSocket lắng nghe trên port 5000
		servSocket = new ServerSocket(DEFAULT_PORT);
		System.out.println("start server Socket");
	}
//	public void sendFlowDeleteMessage(double z) {
//		try {
//			Integer dem = 0;
//			logger.info(dem.toString());
//			dem++;
//		} catch (Exception e) {
//			// TODO: handle exception
//			System.out.println(e.toString());
//		}
//		StatisticsCollector statisticsCollector = (StatisticsCollector) switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
//		Map<DatapathId, List<OFStatsReply>> replies = statisticsCollector.getSwitchStatistics(switchService.getAllSwitchDpids(), OFStatsType.FLOW);
//		int numberFlowDeleted = 0;
//		for (Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
//		for (OFStatsReply r : e.getValue()) {
//		OFFlowStatsReply fsr = (OFFlowStatsReply) r ;
//		List<OFFlowStatsEntry> list = fsr.getEntries();
//		//Sắp xếp các flow từ nhỏ đến lớn theo số gói tin của flow sử dụng HeapSort
//		sort(list);
//		for (OFFlowStatsEntry fse : list) {
//		Match match = fse.getMatch();
//		numberFlowDeleted++;
//		//Kiểm tra xem số flow bị delete đã đủ số Z chưa
//		if(numberFlowDeleted*1.0/list.size() < z){
//
//		// listMatch.add(match);
//		OFFactory myFactory = OFFactories.getFactory(OFVersion.OF_10);
//		IOFSwitch sw = switchService.getSwitch(e.getKey());
//
//		//Build bản tin FlowDelete
//		OFFlowDelete flowDelete = myFactory.buildFlowDelete()
//		.setCookie(fse.getCookie()).setMatch(match).build();
//		
//		//gửi bản tin FlowDelete xuống Switch
//		sw.write(flowDelete);
//					}	
//		else return;
//				}
//			}
//		}
// 	}
//
//	public static void sort(List<OFFlowStatsEntry> IP) {
//		int n = IP.size();
//		for (int i = n / 2 - 1; i >= 0; i--) {
//			heapify(IP, n, i);
//		}
//
//		// Heap sort
//		for (int i = n - 1; i >= 0; i--) {
//			OFFlowStatsEntry temp = IP.get(0);
//			IP.set(0,IP.get(i));
//			IP.set(i,temp);
//			// Heapify root element
//			heapify( IP, i, 0);
//		}
//	}
//	public static void heapify(List<OFFlowStatsEntry> IP, int n,int i) {
//		int largest = i;
//		int l = 2 * i + 1; // ben trai
//		int r = 2 * i + 2;  // ben phai
//		if (l < n && IP.get(l).getPacketCount().getValue() > IP.get(largest).getPacketCount().getValue()) {
//			largest = l;
//		}
//		if (r < n && IP.get(r).getPacketCount().getValue() > IP.get(largest).getPacketCount().getValue()) {
//			largest = r;
//		}
//		if (largest != i) {
//			OFFlowStatsEntry swap = IP.get(i);
//			IP.set(i,IP.get(largest));
//			IP.set(largest,swap);
//			heapify(IP, n, largest);
//		}
//	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		System.out.println("Begin module Fuzzy");
//		while(true)
//		{
//			sendFlowDeleteMessage(Z1);
//		}
		while (true){
			try{
			socket = servSocket.accept();
			communicate(socket);
			} catch (IOException e){
			System.out.println(e.getMessage());
			}
		}
	}
	private void communicate(java.net.Socket connSocket){
		try {
			ObjectInputStream in = new ObjectInputStream(connSocket.getInputStream());
			Double z;
			try{
				while((z = in.readDouble()) != 0) {
					System.out.println(z);
					// Đọc số Z từ collector gửi lên
					//			StatisticsCollector statisticsCollector = new StatisticsCollector();
					//			statisticsCollector.sendFlowDeleteMessage(z);// Delete Flow theo tham số Z
				logger.info(z.toString());
				}
			} catch(IOException e){
				System.out.println("Client stopped sending data");
			}
		} catch(IOException e){
			System.out.println("Cannot communicate to client");
		}	
	}
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
		// TODO Auto-generated method stub
		return null;
	}
}