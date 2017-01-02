/**
 * 
 */
package com.pi.devices;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.HttpClient;
import com.pi.model.Action;
import com.pi.model.DeviceState;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;


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
		Process pr = rt.exec("sudo pigpiod");
		
		this.RED_PIN = pins.get(red).getBCM_Pin();
		this.GREEN_PIN = pins.get(green).getBCM_Pin();
		this.BLUE_PIN = pins.get(blue).getBCM_Pin();
	}

	@Override
	public void performAction(Action action)
	{
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
			boolean testSwitch = false;
			if (testSwitch)
			{
				Gpio.wiringPiSetup();
				SoftPwm.softPwmCreate(23, 20, 100);
				SoftPwm.softPwmWrite(23, 40);
			}
			//TODO use better strategy
			rt.exec("pigs p " + RED_PIN + " " + (255 - red));
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - green));
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - blue));
			
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
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public DeviceState getState()
	{
		return new LedState(name, currentColor);
	}
	
	@Override
	public String getType()
	{
		return DeviceType.LED;
	}
	
	public static class LedState extends DeviceState
	{
		int red, green, blue;
		
		public LedState(String deviceName, Color color)
		{
			super(deviceName);
			red = color.getRed();
			green = color.getGreen();
			blue = color.getBlue();
		}

		public int getRed()
		{
			return red;
		}

		public int getGreen()
		{
			return green;
		}

		public int getBlue()
		{
			return blue;
		}

		@Override
		public String getType()
		{
			return DeviceType.LED;
		}
	}
	
	@XmlRootElement(name = DEVICE)
	public static class LedConfig extends DeviceConfig
	{
		int red, green, blue;

		@Override
		public Device buildDevice() throws IOException
		{
			return new Led(name, red, green, blue);
		}

		@XmlElement
		public void setRed(int red)
		{
			this.red = red;
		}

		@XmlElement
		public void setGreen(int green)
		{
			this.green = green;
		}

		@XmlElement
		public void setBlue(int blue)
		{
			this.blue = blue;
		}
	}
	
	private interface QueryParams
	{
		public static final String RED = "red";
		public static final String GREEN = "green";
		public static final String BLUE = "blue";
	}
}
