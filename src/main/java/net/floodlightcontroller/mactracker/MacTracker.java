package net.floodlightcontroller.mactracker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IListener;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.simpleframework.transport.Socket;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class MacTracker implements IFloodlightModule,IOFMessageListener{
	public static final int port = 9999;
	ServerSocket servSocket;

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
		servSocket = new ServerSocket(port);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		while (true){
			try{
				Socket socket = (Socket) servSocket.accept();
				communicate(socket);
			} catch (IOException e){
				System.out.println(e.getMessage());
			}
		}
		
		
	}
	private void communicate(Socket socket){
		try {
			ObjectInputStream in = new ObjectInputStream(((java.net.Socket) socket).getInputStream());
			double z;
			try{
			while((z = in.readDouble()) != 0) {
				System.out.println("So Z = "+z);
			}
		} catch(IOException e){
			System.out.println("Client stopped sending data");
		}
		} catch(IOException e){
			System.out.println("Cannot communicate to client");
		}	
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		return Command.CONTINUE;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}
}
