/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.HttpClient;
import com.pi.repository.Action;

/**
 * @author Christian Everett
 *
 */
public class TempatureSensor extends Device
{
	private String location = "";
	
	public TempatureSensor(String name, int headerPin, String location) throws IOException
	{
		super(name);
		this.headerPin = headerPin;
		this.location = location;
	}

	@Override
	public void performAction(Action action)
	{
		if(isClosed)
			return;
	}

	@Override
	public Action getState()
	{
		if(isClosed)
			return null;
		
		List<NameValuePair> params = new ArrayList<>();
		
		double tempature = (int)(1.8 * readSensor(pins.get(headerPin).getBCM_Pin())) + 32;
		params.add(new BasicNameValuePair(queryParams.ROOM_TEMP, String.valueOf(tempature)));
		
		try
		{
			Document doc = Jsoup.connect("http://www.google.com/search?q=weahther+" + location)
					.userAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
					.get();
			Element element = doc.getElementById("wob_tm");
			
			if(element != null)
			{
				params.add(new BasicNameValuePair(queryParams.TEMP, element.html()));
			}

		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		
		return new Action(name, HttpClient.URLEncodeData(params));
	}

	@Override
	public void close()
	{
		isClosed = true;
	}

	private native double readSensor(int pin);
	
	static
	{
		System.loadLibrary("TempDriver");
	}
	
	interface queryParams
	{
		public static final String ROOM_TEMP = "room_temp";
		public static final String TEMP = "temp";
	}
}
