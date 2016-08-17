/**
 * 
 */
package com.thermostat.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.sun.net.httpserver.HttpServer;
import com.thermostat.Main;

/**
 * @author Christian Everett
 *
 */
public class ThermostatServer extends Thread
{
	private static ThermostatServer singleton = null;
	private HttpServer server = null;
	
	private ThermostatServer()
	{
		try
		{
			server = HttpServer.create(new InetSocketAddress(8080), 5);
			server.createContext("/tempature", new TempatureHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
		}
		catch (IOException e)
		{
			Main.LOGGER.severe(e.getMessage());
		} 
	}
	
	public static List<NameValuePair> parseURLEncodedData(String data)
	{
		return URLEncodedUtils.parse(data, Charset.forName("utf8"));
	}
	
	public static String URLEncodeData(List<NameValuePair> params)
	{
		return URLEncodedUtils.format(params, Charset.forName("utf8"));
	}
	
	public static void runThermostatServer()
	{
		if(singleton == null)
			singleton = new ThermostatServer();
	}
}
