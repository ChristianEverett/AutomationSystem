/**
 * 
 */
package com.pi.devices.thermostat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.pi.Application;
import com.pi.backgroundprocessor.Processor;
import com.pi.devices.TempatureSensor;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.Email;
import com.pi.infrastructure.HttpClient;
import com.pi.infrastructure.PropertyManger;
import com.pi.infrastructure.PropertyManger.PropertyKeys;
import com.pi.model.Action;
import com.pi.infrastructure.HttpClient.Response;

/**
 * @author Christian Everett
 *
 */
public class ThermostatController extends Device
{
	private int taskID = -1;
	private Lock lock = new ReentrantLock();
	private HttpClient httpClient = null;
	private String sensorDevice = null;

	private ThermostatMode currentMode = ThermostatMode.OFF_MODE;
	private ThermostatMode targetMode = ThermostatMode.OFF_MODE;
	
	private int targetTempInFehrenheit = 68;
	private int currentTempInFehrenheit = -1;

	private int currentHumidity = 0;
	
	private static final String PATH = "/tempature";

	private final int MAX_TEMP;
	private final int MIN_TEMP;

	private AtomicBoolean isTurnOffDelayEnabled = new AtomicBoolean(false);
	private long turnOffDelay;
	private boolean thermostatDisconnected = false;

	public ThermostatController(String name, String url, String sensorDevice, int maxTemp, int mintemp, long turnOffDelay)
			throws IOException
	{
		super(name);
		httpClient = new HttpClient(url);
		this.sensorDevice = sensorDevice;

		MAX_TEMP = maxTemp;
		MIN_TEMP = mintemp;

		this.turnOffDelay = turnOffDelay;

		taskID = bgp.getThreadExecutorService().scheduleTask(() -> 
		{
			try
			{
				Response URLEncodedResponse = httpClient.sendGet(null, PATH);

				if (URLEncodedResponse.getStatusCode() != HttpURLConnection.HTTP_OK)
					throw new IOException("Got status: " + URLEncodedResponse.getStatusCode());
				
				thermostatDisconnected = false;
				updateControllerState(URLEncodedResponse.getReponseBody(), true);

				if (targetMode.equals(ThermostatMode.COOL_MODE) || targetMode.equals(ThermostatMode.HEAT_MODE))
				{
					if (targetTempatureReached() && !isTurnOffDelayEnabled.get())
					{
						setMode(ThermostatMode.OFF_MODE);
					}
					else if(!targetMode.equals(currentMode))
					{
						isTurnOffDelayEnabled.set(true);
						setModeAndTargetTempature();
						bgp.getThreadExecutorService().scheduleTask(() -> 
						{
							isTurnOffDelayEnabled.set(false);
						}, turnOffDelay, TimeUnit.SECONDS);
					}
				}
			}
			catch (Throwable e)
			{
				thermostatDisconnected = true;
				Application.LOGGER.severe(e.getClass() + " - " + e.getMessage());
			}
		}, 5L, 15L, TimeUnit.SECONDS);
	}

	@Override
	public void performAction(Action action)
	{
		if (isClosed)
			return;

		updateControllerState(action.getData(), false);
		if (!targetTempatureReached())
		{
			setModeAndTargetTempature();
		}
	}

	private synchronized void updateControllerState(String response, boolean fromThermostat)
	{
		HashMap<String, String> responseParams = HttpClient.URLEncodedDataToHashMap(response);

		try
		{
			lock.lock();

			if (fromThermostat)
			{
				this.currentMode = ThermostatMode.valueOf(responseParams.get(QueryParams.MODE).toUpperCase());
				this.currentTempInFehrenheit = celsiusToFahrenheit(Float.parseFloat(responseParams.get(QueryParams.TEMP)));
				this.currentHumidity = (int) Float.parseFloat(responseParams.get(QueryParams.HUMIDITY));
			}
			else
			{
				int targetTemp = (int) Float.parseFloat(responseParams.get(QueryParams.TARGET_TEMP));
				ThermostatMode mode = ThermostatMode.valueOf(responseParams.get(QueryParams.TARGET_MODE).toUpperCase());

				if (targetTemp < MAX_TEMP && targetTemp > MIN_TEMP)
				{
					targetTempInFehrenheit = targetTemp;
					targetMode = mode;
				}
			}
		}
		catch (Exception e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		finally
		{
			lock.unlock();
		}
	}

	private void setModeAndTargetTempature()
	{
		setMode(targetMode);
	}
	
	private void setMode(ThermostatMode mode)
	{
		lock.lock();
		if (targetTempInFehrenheit != currentTempInFehrenheit || !mode.equals(currentMode))
		{
			ArrayList<NameValuePair> paramsList = new ArrayList<>();
			paramsList.add(new BasicNameValuePair(QueryParams.TARGET_TEMP, String.valueOf(fahrenheitToCelsius(targetTempInFehrenheit))));
			paramsList.add(new BasicNameValuePair(QueryParams.TARGET_MODE, mode.toString()));
			try
			{
				Response URLEncodedResponse = httpClient.sendPost(null, PATH, HttpClient.URLEncodeData(paramsList));
				updateControllerState(URLEncodedResponse.getReponseBody(), true);
			}
			catch (Exception e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}
		lock.unlock();
	}
	
	private int getLocationTempature()
	{
		Action action = bgp.getDeviceByName(sensorDevice).getState();
		HashMap<String, String> map = HttpClient.URLEncodedDataToHashMap(action.getData());
		String locationTempature = map.get(TempatureSensor.QueryParams.LOCATION_TEMPATURE);
		if (locationTempature != null) 
			return Integer.parseInt(locationTempature);
		
		return -1;
	}

	@Override
	public void close()
	{
		isClosed = true;
		bgp.getThreadExecutorService().cancelTask(taskID);
		setMode(ThermostatMode.OFF_MODE);	
	}

	@Override
	public Action getState()
	{
		if (isClosed)
			return null;
		lock.lock();
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair(QueryParams.TEMP, String.valueOf(currentTempInFehrenheit)));
		params.add(new BasicNameValuePair(QueryParams.MODE, currentMode.toString()));
		params.add(new BasicNameValuePair(QueryParams.HUMIDITY, String.valueOf(currentHumidity)));
		params.add(new BasicNameValuePair(QueryParams.TARGET_MODE, targetMode.toString()));
		params.add(new BasicNameValuePair(QueryParams.TARGET_TEMP, String.valueOf(targetTempInFehrenheit)));
		lock.unlock();

		return new Action(name, HttpClient.URLEncodeData(params));
	}

	private int celsiusToFahrenheit(float c)
	{
		return (int) (Math.round(c * 1.8) + 32);
	}

	private float fahrenheitToCelsius(int f)
	{
		return (float) ((f - 32) / 1.8);
	}

	private boolean targetTempatureReached()
	{
		int temp = getLocationTempature();
		
		return (targetMode.equals(ThermostatMode.COOL_MODE) && currentTempInFehrenheit <= targetTempInFehrenheit)
				|| (targetMode.equals(ThermostatMode.HEAT_MODE) && currentTempInFehrenheit >= targetTempInFehrenheit)
				|| (temp != -1 && targetMode.equals(ThermostatMode.COOL_MODE) && temp <= targetTempInFehrenheit)
				|| (temp != -1 && targetMode.equals(ThermostatMode.HEAT_MODE) && temp >= targetTempInFehrenheit);
	}
	
	private interface QueryParams
	{
		public static final String TEMP = "temp";
		public static final String MODE = "mode";
		public static final String HUMIDITY = "humidity";
		public static final String TARGET_TEMP = "target_temp";
		public static final String TARGET_MODE = "target_mode";
	}
}
