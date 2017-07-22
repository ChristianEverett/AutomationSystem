/**
 * 
 */
package com.pi.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.SystemLogger;
import com.pi.controllers.ActionController;
import com.pi.controllers.EventController;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.NodeController;
import com.pi.infrastructure.RemoteDevice;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.util.HttpClient;
import com.pi.infrastructure.util.HttpClient.ObjectResponse;
import com.pi.infrastructure.util.HttpClient.Response;
import com.pi.model.DeviceState;
import com.pi.services.NodeDiscovererService;
import com.pi.services.TaskExecutorService.Task;
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
	private String nodeID = null;
	
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
			
			Device.registerNodeManger(this);
			
			server = HttpServer.create(new InetSocketAddress(8080), QUEUE);
			server.createContext(RemoteDevice.REMOTE_CONFIG_PATH, this);
			server.setExecutor(null); // creates a default executor
			server.start();
			
			setAutomationControllerAddress(NodeDiscovererService.broadCastFromNode(nodeID));
			SystemLogger.getLogger().info("This Node has been registered as: " + nodeID);	
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			System.exit(1);
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
	public void scheduleAction(DeviceState state)
	{
		try
		{
			try
			{
				super.scheduleAction(state);
			}
			catch (RuntimeException e)
			{
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(state);
				
				HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_ADDRESS);
				Response response = client.sendPostJson(null, ActionController.PATH + "/" + state.getName(), json);
				
				if(!response.isHTTP_OK())
					throw new IOException("Could not request action from Controller got response: " + response.getStatusCode());
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not request action. " + e.getMessage());
		}
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
