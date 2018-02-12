/**
 * 
 */
package com.pi.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.BaseNodeController;
import com.pi.infrastructure.RemoteDeviceProxy;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.DeviceAPI;
import com.pi.infrastructure.NodeControllerAPI;
import com.pi.infrastructure.util.DeviceDoesNotExist;
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
	
	private NodeControllerAPI primaryNodeController;
	
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
			LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			
			server = HttpServer.create(new InetSocketAddress(8080), QUEUE);
			server.createContext(RemoteDeviceProxy.REMOTE_CONFIG_PATH, this);
			server.setExecutor(null); // creates a default executor
			server.start();
			
			setAutomationControllerAddress(NodeDiscovererService.broadCastFromNode(nodeID));
			SystemLogger.getLogger().info("This Node has been registered as: " + nodeID);	
			
			SystemLogger.getLogger().info("Looking up primary node rmi");	
			primaryNodeController = (NodeControllerAPI) Naming.lookup("//" + AUTOMATION_CONTROLLER_HOST + "/" + RMI_NAME);	
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
			
			InvocationHandler handler = new RemoteDeviceHandler(this, device);
			DeviceAPI proxy = (DeviceAPI) Proxy.newProxyInstance(DeviceAPI.class.getClassLoader(), new Class[] { DeviceAPI.class }, handler);
			Naming.rebind(device.getName(), UnicastRemoteObject.exportObject(proxy, 0));

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
			catch (DeviceDoesNotExist e)
			{
				primaryNodeController.scheduleAction(state);
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
			
			return primaryNodeController.getDeviceState(name);
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
			primaryNodeController.update(state);
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
			return primaryNodeController.getRepositoryValue(type, key);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
		
		return null;
	}

	@Override
	public <T extends Serializable, K extends Serializable> void setRepositoryValue(String type, T value)
	{
		try
		{
			primaryNodeController.setRepositoryValue(type, value);
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
			return primaryNodeController.getRepositoryValues(type);
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
			primaryNodeController.trigger(actionProfileName);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}
	}

	@Override
	public void unTrigger(String profileName)
	{
		try
		{
			primaryNodeController.unTrigger(profileName);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Could not connect to Automation Controller. " + e.getMessage());
		}		
	}
}
