/**
 * 
 */
package com.pi.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.tomcat.util.net.URL;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi.Main;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceLoader;
import com.pi.infrastructure.HttpClient;
import com.pi.infrastructure.HttpClient.ObjectResponse;
import com.pi.infrastructure.HttpClient.Response;
import com.pi.infrastructure.RemoteDevice;
import com.pi.infrastructure.RemoteDevice.Node;
import com.pi.model.Action;
import com.pi.model.DeviceState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Christian Everett
 *
 */
public class NodeController implements HttpHandler, Node
{
	private static NodeController instance = null;
	
	private HttpServer server = null;
	private static final int QUEUE = 5;
	
	private static final String AUTOMATION_CONTROLLER_API = "http://10.0.0.24:8080";
	
	public static NodeController getInstance()
	{
		if(instance == null)
			instance = new NodeController();
		
		return instance;
	}
	
	private NodeController()
	{
		try
		{
			Device.registerRemoteDeviceLookup(this);
			
			server = HttpServer.create(new InetSocketAddress(8080), QUEUE);
			server.createContext(RemoteDevice.REMOTE_CONFIG_PATH, this);
			server.setExecutor(null); // creates a default executor
			server.start();
		}
		catch (Exception e)
		{
			Main.LOGGER.severe(e.getMessage());
		}
	}

	public void handle(HttpExchange request) throws IOException
	{
		try
		{
			ObjectInputStream input = new ObjectInputStream(request.getRequestBody());
			Element element = (Element) input.readObject();
			
			String name = element.getAttribute(DeviceLoader.DEVICE_NAME);
			Device.createNewDevice(element);

			server.createContext("/" + name, new DeviceHandler(Device.lookupDevice(name)));
			request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
		}
		catch (Exception e)
		{
			Main.LOGGER.severe(e.getMessage());
			request.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
		}
	}

	@Override
	public boolean requestAction(Action action)
	{
		String json;
		
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			json = mapper.writeValueAsString(new Action());
			
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_API);
			Response response = client.sendPost(null, "/action/add", json);
			
			if(!response.isHTTP_OK())
				throw new IOException("Could not request action from Controller got response: " + response.getStatusCode());
		}
		catch (IOException e)
		{
			Main.LOGGER.severe("Could not request action. " + e.getMessage());
		}
		
		return false;
	}

	@Override
	public DeviceState getDeviceState(String name)
	{
		try
		{
			HttpClient client = new HttpClient(AUTOMATION_CONTROLLER_API);
			ObjectResponse response = client.sendGetObject(null, "/action/AC/" + name);
			
			return (DeviceState) response.getResponseObject();
		}
		catch (IOException | ClassNotFoundException e)
		{
			Main.LOGGER.severe("Could not connect to Automation Controller. " + e.getMessage());
		}
		
		return null;
	}
}
