/**
 * 
 */
package com.pi.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.SystemLogger;
import com.pi.controllers.ActionController;
import com.pi.controllers.EventController;
import com.pi.controllers.DataRepositoryController;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.util.HttpClient;
import com.pi.infrastructure.util.HttpClient.ObjectResponse;
import com.pi.infrastructure.util.HttpClient.Response;
import com.pi.model.DeviceState;
import com.pi.services.NodeDiscovererService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Christian Everett
 *
 */
public class NodeControllerImpl extends BaseNodeController implements HttpHandler
{
	private HttpServer server = null;
	private static final int QUEUE = 5;
	
	private String nodeID = null;
	
	private static String AUTOMATION_CONTROLLER_HOST = null;
	private static int AUTOMATION_CONTROLLER_PORT = 8080;
	
	public static NodeControllerImpl start(String nodeID)
	{
		if(singleton == null)
			singleton = new NodeControllerImpl(nodeID);
		
		return (NodeControllerImpl) singleton;
	}
	
	private NodeControllerImpl(String nodeID)
	{
		try
		{
			this.nodeID = nodeID;
			
			Device.registerNodeManger(this);
			System.setProperty("java.rmi.server.hostname", NodeDiscovererService.getLocalIPv4Address()); 
			LocateRegistry.createRegistry(1099);
			
			server = HttpServer.create(new InetSocketAddress(8080), QUEUE);
			server.createContext(RemoteDeviceProxy.REMOTE_CONFIG_PATH, this);
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
		AUTOMATION_CONTROLLER_HOST = address.getHostAddress();
	}

	public void handle(HttpExchange request) throws IOException
	{
		try(ObjectInputStream input = new ObjectInputStream(request.getRequestBody()))
		{
			DeviceConfig config = (DeviceConfig) input.readObject();
			
			Device device = createNewDevice(config);
			Naming.rebind(device.getName(), new RemoteDeviceHandler(this, device));

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
				
				HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
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
	public DeviceState getDeviceState(String name)
	{
		try
		{
			DeviceState state = super.getDeviceState(name);
			
			if(state != null)
				return state;
			
			// Don't pass forDatabase param, automation node should never need device config
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			ObjectResponse response = client.sendGetObject(null, ActionController.PATH + "/AC/" + name);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not get device state: " + response.getStatusCode());
			
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
			
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			Response response = client.sendPostJson(null, EventController.PATH + "/AC/update", json);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not update event record on automation controller got: " + response.getStatusCode());
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
	}

	@Override
	public <T extends Serializable, K extends Serializable> T getRepositoryValue(String type, K key)
	{
		try
		{
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			ObjectResponse response = client.sendGetObject("key=" + key, DataRepositoryController.PATH + "/" + type);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not get repository " + type + " : " + response.getStatusCode());
			
			return (T) response.getResponseObject();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
		
		return null;
	}

	@Override
	public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, K key, T value)
	{
		try
		{
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			ObjectResponse response = client.sendPostObject("key=" + key, DataRepositoryController.PATH + "/" + type, value);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not set repository " + type + " : " + response.getStatusCode());
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}	
	}

	@Override
	public <T extends Serializable> Collection<T> getRepositoryValues(String type)
	{
		try
		{
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			ObjectResponse response = client.sendGetObject(null, DataRepositoryController.PATH + "/all" + type);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not get repository " + type + " : " + response.getStatusCode());
			
			return (Collection<T>) response.getResponseObject();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
		
		return null;
	}

	@Override
	public void trigger(String actionProfileName)
	{
		try
		{
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_HOST, AUTOMATION_CONTROLLER_PORT);
			Response response = client.sendPostJson(null, ActionController.PATH + "/trigger/" + actionProfileName, null);
			
			if (!response.isHTTP_OK())
				throw new IOException("Could not request action from Controller got response: " + response.getStatusCode());
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
	}
}
