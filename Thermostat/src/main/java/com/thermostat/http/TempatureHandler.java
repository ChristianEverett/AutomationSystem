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
	private static final String POST = "POST";
	Thermostat thermostat = Thermostat.getInstance();
	
	@Override
	public void handle(HttpExchange httpExchange) throws IOException
	{
		try
		{
			String urlEncodedResponseBody;
			String s = httpExchange.getRequestMethod();
			
			if (httpExchange.getRequestMethod().equalsIgnoreCase(POST))
			{
				String requestBody = "";
				Scanner input = new Scanner(new BufferedInputStream(httpExchange.getRequestBody()));
				while (input.hasNext())
				{
					requestBody += input.nextLine();
				}
				input.close();
				List<NameValuePair> params = ThermostatServer.parseURLEncodedData(requestBody);
				thermostat.performAction(extractAndValidateTempature(params));
				urlEncodedResponseBody = ThermostatServer.URLEncodeData(new ArrayList<NameValuePair>()
				{
					{
						add(new BasicNameValuePair(QueryParams.TEMP,
								String.valueOf(thermostat.getTempature())));
						add(new BasicNameValuePair(QueryParams.MODE,
								thermostat.getMode().toString()));
						add(new BasicNameValuePair(QueryParams.TARGET_TEMP,
								String.valueOf(thermostat.getTargetTemp())));
						add(new BasicNameValuePair(QueryParams.TARGET_MODE,
								String.valueOf(thermostat.getTargetMode())));
						add(new BasicNameValuePair(QueryParams.HUMIDITY,
								String.valueOf(thermostat.getHumidity())));
					}
				});
			}
			else 
			{
				urlEncodedResponseBody = ThermostatServer.URLEncodeData(new ArrayList<NameValuePair>()
				{
					{
						add(new BasicNameValuePair(QueryParams.TEMP,
								String.valueOf(thermostat.getTempature())));
						add(new BasicNameValuePair(QueryParams.MODE,
								thermostat.getMode().toString()));
						add(new BasicNameValuePair(QueryParams.HUMIDITY,
								String.valueOf(thermostat.getHumidity())));
					}
				});
			}
			
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
		float temp = thermostat.getTargetTemp();
		ThermostatMode temp_mode = ThermostatMode.OFF_MODE;
		
		for(NameValuePair pair : params)
		{
			switch (pair.getName())
			{
			case QueryParams.TARGET_TEMP:
				temp = Float.parseFloat(pair.getValue());
				break;
			case QueryParams.TARGET_MODE:
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
		public static final String HUMIDITY = "humidity";
		public static final String TARGET_MODE = "target_mode";
		public static final String TARGET_TEMP = "target_temp";
	}
}
