/**
 * 
 */
package com.pi.devices;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.HttpClient;
import com.pi.repository.Action;


/**
 * @author Christian Everett
 *
 */
public class Led extends Device
{
	private final int RED_PIN;
	private final int GREEN_PIN;
	private final int BLUE_PIN;

	private Color currentColor = new Color(0, 0, 0);
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);
		this.RED_PIN = red;
		this.GREEN_PIN = green;
		this.BLUE_PIN = blue;
	}

	@Override
	public void performAction(Action action)
	{
		if(isClosed)
			return;
		try
		{
			int red = 0, green = 0, blue = 0;
			
			for(NameValuePair pair : HttpClient.parseURLEncodedData(action.getData()))
			{
				switch (pair.getName())
				{
				case QueryParams.RED:
					red = Integer.parseInt(pair.getValue());
					break;
				case QueryParams.GREEN:
					green = Integer.parseInt(pair.getValue());
					break;
				case QueryParams.BLUE:
					blue = Integer.parseInt(pair.getValue());
					break;
	
				default:
					break;
				}
			}
		
			rt.exec("pigs p " + RED_PIN + " " + (255 - red) + " &");
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - green) + " &");
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - blue) + " &");
			
			currentColor = new Color(red, green, blue);
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public void close()
	{
		try
		{
			rt.exec("pigs p " + RED_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - 0) + " &");
			isClosed = true;
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public Action getState()
	{
		if(isClosed)
			return null;
		
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair(QueryParams.RED, String.valueOf(currentColor.getRed())));
		params.add(new BasicNameValuePair(QueryParams.GREEN, String.valueOf(currentColor.getGreen())));
		params.add(new BasicNameValuePair(QueryParams.BLUE, String.valueOf(currentColor.getBlue())));
		
		return new Action(name, HttpClient.URLEncodeData(params));
	}
	
	private interface QueryParams
	{
		public static final String RED = "red";
		public static final String GREEN = "green";
		public static final String BLUE = "blue";
	}
}
