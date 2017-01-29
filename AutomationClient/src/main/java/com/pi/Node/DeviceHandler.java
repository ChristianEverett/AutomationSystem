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
		try
		{
			String query = request.getRequestURI().getQuery();
			if (!query.isEmpty())
			{
				HashMap<String, String> queryParams = HttpClient.URLEncodedDataToHashMap(query);

				int methodID = Integer.parseInt(queryParams.get(RemoteDevice.METHOD_QUERY_PARAM));		
				
				switch (methodID)
				{
				case RemoteDevice.PERFORM_ACTION:
					ObjectInputStream input = new ObjectInputStream(request.getRequestBody());
					DeviceState action = (DeviceState) input.readObject();
					input.close();
					if(action == null)
						throw new IOException("Action null");
					device.performAction(action);
					request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
					break;
				case RemoteDevice.GET_STATE:
					request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
					ObjectOutputStream output = new ObjectOutputStream(request.getResponseBody());
					output.writeObject(device.getState());
					output.close();
					break;
				case RemoteDevice.CLOSE:
					device.close();
					Main.LOGGER.info("Closed: " + device.getName());
					request.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
					request.close();
					break;
				default:
					throw new IOException("Unknown method request");
				}
			}
			else
			{
				throw new IOException("Empty query string");
			} 
		}
		catch (Exception e)
		{
			Main.LOGGER.severe("Bad device request: " + e.getMessage());
			request.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
		}
	}
}
