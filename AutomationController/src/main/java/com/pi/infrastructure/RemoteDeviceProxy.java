/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Collection;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.util.HttpClient;
import com.pi.infrastructure.util.HttpClient.Response;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class RemoteDeviceProxy extends Device implements RepositoryObserver
{
	public static final String REMOTE_CONFIG_PATH = "/config";

	private HttpClient client = null;
	private String type;
	private boolean isAsyncDevice = false;
	private DeviceAPI device;

	@SuppressWarnings("unchecked")
	public RemoteDeviceProxy(String name, String type, String host, DeviceConfig config) throws Exception
	{
		super(name);
		this.type = type;
		client = new HttpClient(host, 8080);

		Response response = client.sendPostObject(null, REMOTE_CONFIG_PATH, (Serializable) config);

		if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Error creating device. Status Code: " + response.getStatusCode() + " on device: " + name);
		
		device = (DeviceAPI) Naming.lookup("//" + host + "/" + name);
		isAsyncDevice = device.isAsynchronousDevice();
	}

	@Override
	protected void performAction(DeviceState state)
	{
		try
		{
			device.execute(state);
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Remote Device performAction failure - " + e.getMessage());
		}
	}

	@Override
	public DeviceState getState(DeviceState state)
	{
		try
		{
			return device.getCurrentDeviceState();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Remote Device getState failure - " + e.getMessage());
		}

		return null;
	}

	@Override
	protected void tearDown()
	{
		try
		{
			device.close();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Remote Device tearDown failure - " + e.getMessage());
		}
	}

	@Override
	public void newActionProfile(Collection<String> actionProfileNames) throws RemoteException
	{
		device.newActionProfile(actionProfileNames);
	}
	
	@Override
	public boolean isAsynchronousDevice()
	{
		return isAsyncDevice;
	}
	
	@Override
	public String getType()
	{
		return type;
	}

	@XmlRootElement(name = DEVICE)
	public static class RemoteDeviceConfig extends DeviceConfig
	{
		private DeviceConfig config;
		private String type, host;
		private String nodeID;

		@Override
		public Device buildDevice() throws Exception
		{
			return new RemoteDeviceProxy(name, type, host, config);
		}

		public void setConfig(DeviceConfig config)
		{
			this.config = config;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public void setHost(String url)
		{
			this.host = url;
		}
		
		public void setNodeID(String nodeID)
		{
			this.nodeID = nodeID;
		}
		
		public String getNodeID()
		{
			return nodeID;
		}
	}
}
