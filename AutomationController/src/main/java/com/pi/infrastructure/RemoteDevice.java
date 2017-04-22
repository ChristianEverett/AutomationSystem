/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.w3c.dom.Element;

import com.pi.Application;
import com.pi.infrastructure.util.HttpClient;
import com.pi.infrastructure.util.HttpClient.ObjectResponse;
import com.pi.infrastructure.util.HttpClient.Response;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class RemoteDevice extends Device
{
	public static final String REMOTE_CONFIG_PATH = "/config";
	public static final String DEVICE_NAME_QUERY_PARAM = "device";
	public static final String DEVICE_TYPE_QUERY_PARAM = "type";

	private HttpClient client = null;

	private String type;
	private List<String> expectedTypes = null;

	@SuppressWarnings("unchecked")
	public RemoteDevice(String name, String type, String url, DeviceConfig config) throws Exception
	{
		super(name);
		this.type = type;
		client = new HttpClient(url);

		Response response = client.sendPostObject(null, REMOTE_CONFIG_PATH, (Serializable) config);

		if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Error creating device. Status Code: " + response.getStatusCode() + " on device: " + name);
		
		ObjectResponse objectResponse = client.sendPostObject(null, "/" + name, new RemoteDeviceMessage(GET_EXPECTED_PARAMS, null));
		
		if (objectResponse.getStatusCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Error getting expected params. Status Code: " + response.getStatusCode() + " on device: " + name);
		
		expectedTypes = (List<String>) objectResponse.getResponseObject();
	}

	@Override
	protected void performAction(DeviceState state)
	{
		try
		{
			ObjectResponse response = client.sendPostObject(null, "/" + name, new RemoteDeviceMessage(PERFORM_ACTION, state));
			if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("Status Code: " + response.getStatusCode() + " on device: " + name);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Remote Device performAction failure - " + e.getMessage());
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		try
		{
			ObjectResponse response = client.sendPostObject(null, "/" + name, new RemoteDeviceMessage(GET_STATE, forDatabase));
			if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("Status Code: " + response.getStatusCode() + " on device: " + name);

			return (DeviceState) response.getResponseObject();
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Remote Device performAction failure - " + e.getMessage());
		}

		return null;
	}

	@Override
	protected void tearDown()
	{
		try
		{
			ObjectResponse response = client.sendPostObject(null, "/" + name, new RemoteDeviceMessage(CLOSE, null));
			if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("Status Code: " + response.getStatusCode() + " on device: " + name);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Remote Device performAction failure - " + e.getMessage());
		}
	}

	@Override
	public String getType()
	{
		return type;
	}

	@Override
	public List<String> getExpectedParams()
	{
		return expectedTypes;
	}
	
	public static class RemoteDeviceMessage implements Serializable
	{
		private int methodID;
		private Object data;
		
		public RemoteDeviceMessage(int methodID, Object data)
		{
			this.methodID = methodID;
			this.data = data;
		}

		public int getMethodID()
		{
			return methodID;
		}

		public Object getData()
		{
			return data;
		}
	}

	@XmlRootElement(name = DEVICE)
	public static class RemoteDeviceConfig extends DeviceConfig
	{
		private DeviceConfig config;
		private String type, url;
		private String nodeID;

		@Override
		public Device buildDevice() throws Exception
		{
			return new RemoteDevice(name, type, url, config);
		}

		public void setElement(DeviceConfig config)
		{
			this.config = config;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public void setUrl(String url)
		{
			this.url = url;
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
