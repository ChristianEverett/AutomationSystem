/**
 * 
 */
package com.pi.infrastructure;

import java.io.IOException;
import java.net.HttpURLConnection;
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
	public static final String METHOD_QUERY_PARAM = "method";
	public static final String DEVICE_NAME_QUERY_PARAM = "device";
	public static final String DEVICE_TYPE_QUERY_PARAM = "type";

	private HttpClient client = null;

	private String type;

	public RemoteDevice(String name, String type, String url, Element element) throws IOException
	{
		super(name);
		this.type = type;
		client = new HttpClient(url);

		Response response = client.sendPostObject(null, REMOTE_CONFIG_PATH, element);

		if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
			throw new IOException("Error creating device. Status Code: " + response.getStatusCode() + " on device: " + name);
	}

	@Override
	public void performAction(DeviceState state)
	{
		try
		{
			ObjectResponse response = client.sendPostObject(METHOD_QUERY_PARAM + "=" + PERFORM_ACTION, "/" + name, state);
			if (response.getStatusCode() != HttpURLConnection.HTTP_OK)
				throw new IOException("Status Code: " + response.getStatusCode() + " on device: " + name);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Remote Device performAction failure - " + e.getMessage());
		}
	}

	@Override
	public DeviceState getState()
	{
		try
		{
			ObjectResponse response = client.sendGetObject(METHOD_QUERY_PARAM + "=" + GET_STATE, "/" + name);
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
	public void close()
	{
		try
		{
			ObjectResponse response = client.sendGetObject(METHOD_QUERY_PARAM + "=" + CLOSE, "/" + name);
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

	public interface Node
	{
		public boolean requestAction(DeviceState state);
		public DeviceState getDeviceState(String name);
	}

	@XmlRootElement(name = DEVICE)
	public static class RemoteDeviceConfig extends DeviceConfig
	{
		private Element element;
		private String type, url;

		@Override
		public Device buildDevice() throws IOException
		{
			return new RemoteDevice(name, type, url, element);
		}

		public void setElement(Element element)
		{
			this.element = element;
		}

		public void setType(String type)
		{
			this.type = type;
		}

		public void setUrl(String url)
		{
			this.url = url;
		}
	}
}
