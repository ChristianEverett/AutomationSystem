/**
 * 
 */
package com.thermostat.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.sun.glass.ui.Application;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.thermostat.Main;
import com.thermostat.Thermostat;
import com.thermostat.Thermostat.ThermostatSetting;
import com.thermostat.ThermostatMode;

/**
 * @author Christian Everett
 *
 */
public class TempatureHandler implements HttpHandler
{
	Thermostat thermostat = Thermostat.getInstance();
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException
	{
		try
		{
			String requestBody = "";
			Scanner input = new Scanner(new BufferedInputStream(httpExchange.getRequestBody()));

			while(input.hasNext())
			{
				requestBody += input.nextLine();
			}
			input.close();
			
			List<NameValuePair> params = ThermostatServer.parseURLEncodedData(requestBody);
			
			ThermostatSetting newSetting = thermostat.performAction(extractAndValidateTempature(params));
			String urlEncodedResponseBody = ThermostatServer.URLEncodeData(new ArrayList<NameValuePair>()
					{{
						add(new BasicNameValuePair(QueryParams.TEMP, String.valueOf(thermostat.getTempature())));
						add(new BasicNameValuePair(QueryParams.MODE, newSetting.getMode().toString()));
						add(new BasicNameValuePair(QueryParams.TARGET, String.valueOf(newSetting.getTemp())));
					}});
			
			httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, urlEncodedResponseBody.getBytes().length);
			PrintWriter output = new PrintWriter(httpExchange.getResponseBody());
			
			output.print(urlEncodedResponseBody);
			output.close();
		}
		catch (Exception e)
		{
			httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
			httpExchange.getResponseBody().close();
			Main.LOGGER.severe(e.getMessage());
		}
	}
	
	private ThermostatSetting extractAndValidateTempature(List<NameValuePair> params)
	{
		int temp = 0;
		ThermostatMode temp_mode = ThermostatMode.OFF_MODE;
		
		for(NameValuePair pair : params)
		{
			switch (pair.getName())
			{
			case QueryParams.TARGET:
				temp = Integer.parseInt(pair.getValue());
				break;
			case QueryParams.MODE:
				temp_mode = ThermostatMode.valueOf(pair.getValue().toUpperCase());
				break;
			default:
				break;
			}
		}
		
		return new ThermostatSetting(temp_mode, temp);
	}
	
	private interface QueryParams
	{
		public static final String TEMP = "temp";
		public static final String MODE = "mode";
		public static final String TARGET = "target";
	}
}
