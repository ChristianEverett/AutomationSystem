package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.RepositoryObserver;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.UPNPBroadcastResponderService;
import com.pi.infrastructure.util.UPNPDevice;
import com.pi.infrastructure.util.UPNPPacket;
import com.pi.model.ActionProfile;
import com.pi.model.DeviceState;
import com.pi.model.repository.RepositoryType;

public class AmazonEcho extends AsynchronousDevice implements BiConsumer<String, Boolean>, RepositoryObserver
{
	protected static UPNPBroadcastResponderService responder;

	static
	{
		try
		{
			responder = new UPNPBroadcastResponderService();
			responder.start(1);
		}
		catch (IOException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	public AmazonEcho(String name) throws IOException
	{
		super(name);
		createAsynchronousTask(5L, 1L, TimeUnit.SECONDS);
		registerAllCommands();
	}

	private void registerAllCommands() throws IOException
	{
		Collection<ActionProfile> actionProfiles = getRepositoryValues(RepositoryType.ActionProfile);

		for(ActionProfile actionProfile : actionProfiles)
		{
			addNewFauxmoDevice(actionProfile.getName());
		}
	}
	
	private void addNewFauxmoDevice(String actionProfileName)
	{
		try
		{
			responder.addDevice(new FauxmoSwitch(actionProfileName, this));
		}
		catch (IOException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	@Override
	public void newActionProfile(Collection<String> actionProfileNames)
	{
//		for(String actionProfileName : actionProfileNames) TODO
//			addNewFauxmoDevice(actionProfileName);
	}
	
	@Override
	public void accept(String actionProfileName, Boolean on)
	{
		if(on)
			trigger(actionProfileName);
	}
	
	@Override
	protected void update() throws Exception
	{
		responder.listenForRequests();
	}

	@Override
	protected void performAction(DeviceState state) throws Exception
	{
		Set<String> actionProfileNames = state.getParamTypedNonNull(DeviceType.Params.ACTION_PROFILE_NAMES);
		
		for(String profileName : actionProfileNames)
			addNewFauxmoDevice(profileName);
	}

	@Override
	public DeviceState getState(DeviceState state) throws IOException
	{	
		return state;//TODO implement
	}

	@Override
	protected void tearDown() throws Exception
	{
		responder.close();
	}

	private class FauxmoSwitch extends UPNPDevice
	{
		private AtomicBoolean recognized = new AtomicBoolean(false);
		private String actionProfileName;
		private AtomicBoolean isOn = new AtomicBoolean(false);
		private BiConsumer<String, Boolean> handler;

		public FauxmoSwitch(String actionProfileName, BiConsumer<String, Boolean> handler) throws IOException
		{
			super(String.valueOf(actionProfileName.hashCode()), "'X-User-Agent: redsonic'");

			this.actionProfileName = actionProfileName;
			this.handler = handler;
		}

		@Override
		protected String handleRequest(InetAddress address, String request) throws IOException
		{
			if(request.contains("GET /setup.xml HTTP/1.1"))
			{
				return handleSetup();
			}
			else
			{
				return handleStateChange(request);
			}
		}

		private String handleSetup()
		{
			SystemLogger.getLogger().info("Registering Fauxmo Device - " + name);
			//responder.removeDevice(this);
			StringBuilder response = new StringBuilder();
			String xml = SETUP_XML.replace("(device_name)", name);
			xml = xml.replace("(device_serial)", persistent_uuid);
			createOkResponse(response, xml.length());
			response.append(xml);
			return response.toString();
		}
		
		private String handleStateChange(String request) throws IOException
		{
			//SOAPACTION: "urn:Belkin:service:basicevent:1#SetBinaryState"
			recognized.set(true);
			
			if(request.contains("<BinaryState>1</BinaryState>"))
			{
				isOn.set(true);
				handler.accept(actionProfileName, true);
			}
			else if(request.contains("<BinaryState>0</BinaryState>"))
			{
				isOn.set(false);
				handler.accept(actionProfileName, false);
			}
			else
			{
				throw new IOException("Did not get a valid state back from the echo");
			}
			
			StringBuilder response = new StringBuilder();
			createOkResponse(response, 0);
			return response.toString();
		}

		@Override
		protected boolean isValidSearch(UPNPPacket result)
		{
			return result.getData().contains("M-SEARCH") && result.getData().contains("urn:Belkin:device:**");
		}
		
		public boolean isRecognized()
		{
			return recognized.get();
		}
		
		public boolean isOn()
		{
			return isOn.get();
		}
		
		private static final String SETUP_XML = "<?xml version=\"1.0\"?>\n" +
				"<root>\n" + 
				  "<device>\n" +
				    "<deviceType>urn:MakerMusings:device:controllee:1</deviceType>\n" +
				    "<friendlyName>(device_name)</friendlyName>\n" +
				    "<manufacturer>Belkin International Inc.</manufacturer>\n" +
				    "<modelName>Emulated Socket</modelName>\n" +
				    "<modelNumber>3.1415</modelNumber>\n" +
				    "<UDN>uuid:Socket-1_0-(device_serial)</UDN>\n" +
				  "</device>\n" +
				"</root>\n";
	}
	
	@XmlRootElement(name = DEVICE)
	public static class AmazonEchoSwitchConfig extends DeviceConfig
	{
		@Override
		public Device buildDevice() throws IOException
		{
			return new AmazonEcho(name);
		}
	}
}
