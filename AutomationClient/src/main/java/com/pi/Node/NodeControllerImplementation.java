/**
 * 
 */
package com.pi.Node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.Main;
import com.pi.SystemLogger;
import com.pi.backgroundprocessor.NodeDiscovererService;
import com.pi.backgroundprocessor.NodeDiscovererService.Probe;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.controllers.ActionController;
import com.pi.controllers.EventController;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.NodeController;
import com.pi.infrastructure.RemoteDevice;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.util.HttpClient;
import com.pi.infrastructure.util.HttpClient.ObjectResponse;
import com.pi.infrastructure.util.HttpClient.Response;
import com.pi.model.DeviceState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Christian Everett
 *
 */
public class NodeControllerImplementation extends NodeController implements HttpHandler
{
	private HttpServer server = null;
	private static final int QUEUE = 5;
	
	private static final String BROADCAST_ADDRESS = "255.255.255.255";
	private DatagramSocket clientSocket = null;
	private Probe probe = null;
	private String nodeID = null;
	private Task broadcastTask = null;
	
	private static String AUTOMATION_CONTROLLER_ADDRESS = null;
	private static String AUTOMATION_CONTROLLER_PORT = "8080";
	
	public static NodeControllerImplementation start(String nodeID)
	{
		if(singleton == null)
			singleton = new NodeControllerImplementation(nodeID);
		
		return (NodeControllerImplementation) singleton;
	}
	
	private NodeControllerImplementation(String nodeID)
	{
		try
		{
			this.nodeID = nodeID;
			probe = new Probe(nodeID, Probe.BROAD_CAST);
			clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(5000);
			Device.registerNodeManger(this);
			
			server = HttpServer.create(new InetSocketAddress(8080), QUEUE);
			server.createContext(RemoteDevice.REMOTE_CONFIG_PATH, this);
			server.setExecutor(null); // creates a default executor
			server.start();
			
			broadcastTask = Device.createTask(() -> 
			{		
				try
				{
					InetAddress IPAddress = InetAddress.getByName(BROADCAST_ADDRESS);
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
					objectOutput.writeObject(probe);

					DatagramPacket sendPacket = new DatagramPacket(outputStream.toByteArray(), outputStream.toByteArray().length, IPAddress, NodeDiscovererService.DISCOVERY_PORT);
					clientSocket.send(sendPacket);
					
					byte[] packet = new byte[1024];
					DatagramPacket receivePacket = new DatagramPacket(packet, packet.length);
					Probe probe = null;
					while (probe == null || probe.getType() != Probe.BROAD_CAST_ACK)
					{
						clientSocket.receive(receivePacket);
						probe = NodeDiscovererService.extractProbeFromDatagram(receivePacket);
					}
					
					SystemLogger.getLogger().info("This Node has been registered as: " + nodeID);
					setAutomationControllerAddress(receivePacket.getAddress());
					broadcastTask.cancel();
				}
				catch(SocketTimeoutException e)
				{		
				}
				catch (Exception e)
				{
					SystemLogger.getLogger().severe(e.getMessage());
				}
				
			}, 5L, 5L, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private void setAutomationControllerAddress(InetAddress address)
	{
		AUTOMATION_CONTROLLER_ADDRESS = "http://" + address.getHostAddress() + ":" + AUTOMATION_CONTROLLER_PORT;
	}
	
	public void handle(HttpExchange request) throws IOException
	{
		try(ObjectInputStream input = new ObjectInputStream(request.getRequestBody()))
		{
			DeviceConfig config = (DeviceConfig) input.readObject();
			
			createNewDevice(config, false);

			server.createContext(createPathFromName(config.getName()), new DeviceHandler(lookupDevice(config.getName())));
			request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
			request.close();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			request.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
		}
	}

	private String createPathFromName(String name)
	{
		return "/" + name;
	}

	@Override
	public boolean closeDevice(String name)
	{
		boolean result = super.closeDevice(name);
		server.removeContext(createPathFromName(name));
		
		if(deviceMap.isEmpty())
		{
			SystemLogger.getLogger().severe("Shutting Down");
			System.exit(0);
		}
		return result;
	}
	
	@Override
	public boolean scheduleAction(DeviceState state)
	{
		try
		{
			if(super.scheduleAction(state))
				return true;
			
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(state);
			
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_ADDRESS);
			Response response = client.sendPostJson(null, ActionController.PATH + "/" + state.getName(), json);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not request action from Controller got response: " + response.getStatusCode());
			
			return true;
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not request action. " + e.getMessage());
		}
		
		return false;
	}

	@Override
	public DeviceState getDeviceState(String name, boolean isForDatabase)
	{
		try
		{
			DeviceState state = super.getDeviceState(name, isForDatabase);
			
			if(state != null)
				return state;
			
			// Don't pass forDatabase param, automation node should never need device config
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_ADDRESS);
			ObjectResponse response = client.sendGetObject(null, ActionController.PATH + "/AC/" + name);
			
			return (DeviceState) response.getResponseObject();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
		
		return null;
	}

	@Override
	public void update(DeviceState state)
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(state);
			
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_ADDRESS);
			Response response = client.sendPostJson(null, EventController.PATH + "/AC/update", json);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not update event record on automation controller got: " + response.getStatusCode());
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
	}
}
