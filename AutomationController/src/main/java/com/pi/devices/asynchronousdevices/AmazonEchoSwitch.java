package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.UPNPBroadcastResponderService;
import com.pi.infrastructure.util.UPNPDevice;
import com.pi.infrastructure.util.UPNPPacket;
import com.pi.model.DeviceState;

public class AmazonEchoSwitch extends AsynchronousDevice
{
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
	
	private static UPNPBroadcastResponderService responder;
	private Fauxmo fauxmo;
	private AtomicBoolean on = new AtomicBoolean(false);
	
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
	
	public AmazonEchoSwitch(String name) throws IOException
	{
		super(name);
		fauxmo = new Fauxmo(name, responder);
		createAsynchronousTask(5L, 1L, TimeUnit.SECONDS);
	}

	@Override
	protected void update() throws Exception
	{
		fauxmo.listen();
	}

	@Override
	protected void performAction(DeviceState state) throws Exception
	{
	}

	@Override
	public DeviceState getState(Boolean forDatabase) throws IOException
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.ON, on.get());
		
		return state;
	}

	@Override
	protected void tearDown() throws Exception
	{
		fauxmo.close();
	}

	private class Fauxmo extends UPNPDevice
	{
		private String name;

		public Fauxmo(String name, UPNPBroadcastResponderService responder) throws IOException
		{
			super(responder, String.valueOf(name.hashCode()), "'X-User-Agent: redsonic'");

			this.name = name;
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
			if(request.contains("<BinaryState>1</BinaryState>"))
			{
				on.set(true);;
			}
			else if(request.contains("<BinaryState>0</BinaryState>"))
			{
				on.set(false);;
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
	}
	
	@XmlRootElement(name = DEVICE)
	public static class AmazonEchoSwitchConfig extends DeviceConfig
	{
		@Override
		public Device buildDevice() throws IOException
		{
			return new AmazonEchoSwitch(name);
		}
	}
}
