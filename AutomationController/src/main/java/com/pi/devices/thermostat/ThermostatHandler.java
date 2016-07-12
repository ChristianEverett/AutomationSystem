/**
 * 
 */
package com.pi.devices.thermostat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
	HttpClient httpClient = null;
	
	public ThermostatHandler(String url) throws MalformedURLException, IOException
	{
		httpClient = new HttpClient(url);
	}
	
	public int getCurrentTempatureInFahrenheit()
	{
		try
		{
			Response response = httpClient.sendGet(null);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//TODO extract value
		
		return 0;
	}
	
	public void setTargetTempatureInFahrenheit(int temp)
	{
		try
		{
			Response response = httpClient.sendPost(null, null);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		targetTempInFehrenheit = temp;
	}
	
	public void setThermostatMode(ThermostatMode mode)
	{
		this.mode = mode;
	}
	
	public ThermostatMode getTherostatMode()
	{
		return mode;
	}

	@Override
	public boolean performAction(Action action)
			throws IOException, InterruptedException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub
		
	}
}
