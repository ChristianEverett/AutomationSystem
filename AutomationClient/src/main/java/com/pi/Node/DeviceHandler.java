/**
 * 
 */
package com.pi.Node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

import com.pi.Main;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.RemoteDevice;
import com.pi.infrastructure.RemoteDevice.RemoteDeviceMessage;
import com.pi.infrastructure.util.HttpClient;
import com.pi.model.DeviceState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * @author Christian Everett
 *
 */
class DeviceHandler implements HttpHandler
{
	private Device device = null;

	public DeviceHandler(Device device)
	{
		this.device = device;
	}

	public void handle(HttpExchange request) throws IOException
	{
		ObjectInputStream input = null;
		ObjectOutputStream output = null;

		try
		{
			input = new ObjectInputStream(request.getRequestBody());
			RemoteDeviceMessage message = (RemoteDeviceMessage) input.readObject();
			input.close();

			switch (message.getMethodID())
			{
			case RemoteDevice.PERFORM_ACTION:
			{
				DeviceState action = (DeviceState) message.getData();

				if (action == null)
					throw new IOException("Action null");
				device.execute(action);
				request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				
				break;
			}
			case RemoteDevice.GET_STATE:
			{
				Boolean forDatabase = (Boolean) message.getData();
				request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				output = new ObjectOutputStream(request.getResponseBody());
				output.writeObject(device.getState(forDatabase));
				output.close();
				break;
			}
			case RemoteDevice.CLOSE:
			{
				device.close();
				Main.LOGGER.info("Closed: " + device.getName());
				request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				break;
			}
			case RemoteDevice.GET_EXPECTED_PARAMS:
			{
				request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				output = new ObjectOutputStream(request.getResponseBody());
				output.writeObject(device.getExpectedParams());
				output.close();
				break;
			}
			default:
				throw new IOException("Unknown method request");
			}
		}
		catch (Exception e)
		{
			Main.LOGGER.severe("Bad device request: " + e.getMessage());
			request.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
		}
		finally
		{
			if (input != null)
				input.close();
			if (output != null)
				output.close();
			request.close();
		}
	}
}
