/**
 * 
 */
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.pi.Application;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService.Task;

/**
 * @author Christian Everett
 *
 */
public class WeatherSensor extends AsynchronousDevice
{
	private Connection weatherHttpConnection;
	private Connection lightHttpConnection;
	private Connection sunRiseHttpConnection;
	private String location;
	private Task weatherReadingTask;
	
	private int locationTempature = 0;
	private Boolean isDark = false;
	
	private DateTimeFormatter parseFormat = new DateTimeFormatterBuilder().appendPattern("h:mm a").toFormatter();
	
	private static final long updateFrequency = 45L;
	
	public WeatherSensor(String name, String location) throws IOException
	{
		super(name);
		this.location = URLEncoder.encode(location, "UTF-8");
		
		weatherHttpConnection = Jsoup.connect("http://www.google.com/search?q=weahther+" + this.location).userAgent(
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
				.validateTLSCertificates(false);
		
//		lightHttpConnection = Jsoup.connect("http://www.isitdarkoutside.com/").userAgent(
//				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
//				.validateTLSCertificates(false);
		
		sunRiseHttpConnection = Jsoup.connect("http://www.google.com/search?q=sun+rise+sun+set").userAgent(
				"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
		createAsynchronousTask(10L, updateFrequency, TimeUnit.SECONDS);
	}

	@Override
	protected void update() throws IOException, ParseException
	{
		Document document = weatherHttpConnection.get();
		Element element = document.getElementById("wob_tm");

		locationTempature = Integer.parseInt(element.html());

//		document = lightHttpConnection.get();
//		element = document.getElementById("answer");
//		
//		if ("YES".equalsIgnoreCase(element.html()))
//			isDark = true;
//		else
//			isDark = false;
		
		document = sunRiseHttpConnection.get();
		Elements elements = document.getElementsByClass("_I5m");
		
		String sunRiseString = elements.first().html();
		String sunSetString = elements.last().html();
		
		LocalTime sunRise = LocalTime.parse(sunRiseString, parseFormat);
		LocalTime sunSet = LocalTime.parse(sunSetString, parseFormat);
		
		LocalTime now = LocalTime.now();
		
		boolean isBeforeSunRise = now.isBefore(sunRise.minusHours(1));
		boolean isAfterSunSet = now.isAfter(sunSet.minusHours(1));
		
		if(isBeforeSunRise || isAfterSunSet)
			isDark = true;
		else
			isDark = false;
		
		update(getState());		
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
	protected void tearDown()
	{
		weatherReadingTask.cancel();
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
