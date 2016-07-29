/**
 * 
 */
package com.pi.devices.thermostat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.HttpClient;
import com.pi.infrastructure.HttpClient.Response;
import com.pi.repository.Action;


/**
 * @author Christian Everett
 *
 */
public class ThermostatHandler extends Device
{
	private ThermostatMode mode = ThermostatMode.OFF_MODE;
	private int targetTempInFehrenheit = 68;
	private int currentTempInFehrenheit = -1;
	HttpClient httpClient = null;
	
	public ThermostatHandler(String name, String url) throws MalformedURLException, IOException
	{
		super(name);
		httpClient = new HttpClient(url);
	}
	
	@Deprecated
	public void getCurrentTempature()
	{
		try
		{
			Response URLEncodedResponse = httpClient.sendGet(null, null);
			List<NameValuePair> response = URLEncodedResponse.parseURLEncodedData();
			
			String temp = extractParam(response, QueryParams.TEMP);
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	public void setModeAndTargetTempature(String requestBody)
	{
		try
		{
			Response URLEncodedResponse = httpClient.sendPost(null, null, requestBody);
			List<NameValuePair> response = URLEncodedResponse.parseURLEncodedData();
			
			for(NameValuePair pair : response)
			{
				switch (pair.getName())
				{
				case QueryParams.TARGET:
					this.targetTempInFehrenheit = Integer.parseInt(pair.getValue());
					break;
				case QueryParams.MODE:
					this.mode = ThermostatMode.valueOf(pair.getValue());
					break;
				case QueryParams.TEMP:
					this.currentTempInFehrenheit = Integer.parseInt(pair.getValue());
				default:
					break;
				}
			}
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	/**
	 * @param URLEncodedResponse
	 */
	private String extractParam(List<NameValuePair> URLEncodedResponse, String name)
	{
		for(int x = 0; x < URLEncodedResponse.size(); x++)
		{
			if(URLEncodedResponse.get(x).getName().equals(name))
			{
				return URLEncodedResponse.get(x).getValue();
			}
		}
		
		return null;
	}

	@Override
	public void performAction(Action action)
	{
		setModeAndTargetTempature(action.getData());
	}

	@Override
	public void close()
	{
		ArrayList<NameValuePair> paramsList = new ArrayList<>();
		paramsList.add(new BasicNameValuePair(QueryParams.TEMP, String.valueOf(68)));
		paramsList.add(new BasicNameValuePair(QueryParams.MODE, ThermostatMode.OFF_MODE.toString()));

		setModeAndTargetTempature(HttpClient.URLEncodeData(paramsList));
		isClosed = true;
	}

	@Override
	public Action getState()
	{
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair(QueryParams.TEMP, String.valueOf(currentTempInFehrenheit)));
		params.add(new BasicNameValuePair(QueryParams.MODE, mode.toString()));
		params.add(new BasicNameValuePair(QueryParams.TARGET, String.valueOf(targetTempInFehrenheit)));
		
		return new Action(name, HttpClient.URLEncodeData(params));
	}
	
	private interface QueryParams
	{
		public static final String TEMP = "temp";
		public static final String MODE = "mode";
		public static final String TARGET = "target";
	}
}
