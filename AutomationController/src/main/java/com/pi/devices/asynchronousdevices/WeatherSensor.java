/**
 * 
 */
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class WeatherSensor extends Device
{
	private Connection weatherHttpConnection;
	private Connection lightHttpConnection;
	private String location;
	private Task weatherReadingTask;
	
	private int locationTempature = 0;
	private Boolean isDark = false;
	
	private static final long updateFrequency = 45L;
	
	public WeatherSensor(String name, String location) throws IOException
	{
		super(name);
		this.location = URLEncoder.encode(location, "UTF-8");
		
		weatherHttpConnection = Jsoup.connect("https://www.google.com/search?q=weahther+" + this.location).userAgent(
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
		
		lightHttpConnection = Jsoup.connect("http://www.isitdarkoutside.com/").userAgent(
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
		
		createTask(() -> 
		{
			try
			{
				Document doc = weatherHttpConnection.get();
				Element element = doc.getElementById("wob_tm");

				if (element != null)
				{
					locationTempature = Integer.parseInt(element.html());
					update(getState());
				}
				
				doc = lightHttpConnection.get();
				element = doc.getElementById("answer");
				
				if("YES".equalsIgnoreCase(element.html()))
					isDark = true;
				else
					isDark = false;
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getClass() + " - " + e.getMessage());
			}
		}, 5L, updateFrequency, TimeUnit.SECONDS);
	}

	@Override
	protected void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.TEMPATURE, locationTempature);
		state.setParam(Params.IS_DARK, isDark);
		
		return state;
	}

	@Override
	public void close()
	{
		weatherReadingTask.cancel();
	}

	@Override
	public List<String> getExpectedParams()
	{
		return new ArrayList<String>();
	}
	
	@Override
	public String getType()
	{
		return DeviceType.WEATHER_SENSOR;
	}
	
	@XmlRootElement(name = DEVICE)
	public static class WeatherSensorConfig extends DeviceConfig
	{
		private String location;
		
		@Override
		public Device buildDevice() throws IOException
		{
			return new WeatherSensor(name, location);
		}

		@XmlElement
		public void setLocation(String location)
		{
			this.location = location;
		}
	}
}
