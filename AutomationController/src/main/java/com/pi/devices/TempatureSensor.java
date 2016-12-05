/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.HttpClient;
import com.pi.model.Action;

/**
 * @author Christian Everett
 *
 */
public class TempatureSensor extends Device
{
	private Connection httpConnection;
	private String location = "";
	private int tempatureReadingTask;

	private int sensorTempature = -1;
	private int sensorHumidity = -1;
	private int locationTempature = -1;

	public TempatureSensor(String name, int headerPin, String location) throws IOException
	{
		super(name);
		this.headerPin = headerPin;
		this.location = location;

		httpConnection = Jsoup.connect("http://www.google.com/search?q=weahther+" + location).userAgent(
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

		tempatureReadingTask = bgp.getThreadExecutorService().scheduleTask(()->
		{
			try
			{
				SensorReading reading = readSensor(pins.get(headerPin).getBCM_Pin());
				sensorTempature = Math.round((1.8F * reading.getTempature()) + 32);
				sensorHumidity = Math.round(reading.getHumidity());

				Document doc = httpConnection.get();
				Element element = doc.getElementById("wob_tm");

				if (element != null)
				{
					locationTempature = Integer.parseInt(element.html());
				}
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getClass() + " - " + e.getMessage());
			}

		}, 5, 30, TimeUnit.SECONDS);
	}

	@Override
	public void performAction(Action action)
	{
		if (isClosed)
			return;
	}

	@Override
	public Action getState()
	{
		if (isClosed)
			return null;

		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair(QueryParams.ROOM_TEMP, String.valueOf(sensorTempature)));
		params.add(new BasicNameValuePair(QueryParams.LOCATION_TEMPATURE, String.valueOf(locationTempature)));

		return new Action(name, HttpClient.URLEncodeData(params));
	}

	@Override
	public void close()
	{
		isClosed = true;
		bgp.getThreadExecutorService().cancelTask(tempatureReadingTask);
	}

	private native SensorReading readSensor(int pin);

	static
	{
		System.loadLibrary("TempDriver");
	}
	
	public class SensorReading
	{
		private float tempature = -1;
		private float humidity = -1;

		public SensorReading(float tempature, float humidity)
		{
			this.tempature = tempature;
			this.humidity = humidity;
		}

		/**
		 * @return the tempature
		 */
		public float getTempature()
		{
			return tempature;
		}

		/**
		 * @return the humidity
		 */
		public float getHumidity()
		{
			return humidity;
		}
	}

	public interface QueryParams
	{
		public static final String ROOM_TEMP = "room_temp";
		public static final String LOCATION_TEMPATURE = "location_temp";
	}
}
