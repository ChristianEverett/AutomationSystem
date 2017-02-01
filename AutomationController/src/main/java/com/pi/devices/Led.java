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
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.infrastructure.util.HttpClient;
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
	boolean testSwitch = false;
	private Color currentColor = new Color(0, 0, 0);
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);
		Process pr = rt.exec("sudo pigpiod");
//		Gpio.wiringPiSetup();
//		SoftPwm.softPwmCreate(23, 0, 100);
//		SoftPwm.softPwmCreate(24, 0, 100);
//		SoftPwm.softPwmCreate(25, 0, 100);
		
		this.RED_PIN = GPIO_PIN.getBCM_Pin(red);
		this.GREEN_PIN = GPIO_PIN.getBCM_Pin(green);
		this.BLUE_PIN = GPIO_PIN.getBCM_Pin(blue);
	}

	@Override
	public void performAction(DeviceState state)
	{
		try
		{
			Integer red = (Integer) state.getParam(DeviceState.RED);
			Integer green = (Integer) state.getParam(DeviceState.GREEN);
			Integer blue = (Integer) state.getParam(DeviceState.BLUE);
			
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
		DeviceState state = new DeviceState(name);
		state.setParam(DeviceState.RED, currentColor.getRed());
		state.setParam(DeviceState.GREEN, currentColor.getGreen());
		state.setParam(DeviceState.BLUE, currentColor.getBlue());
		
		return state;
	}
	
	@Override
	public String getType()
	{
		return DeviceType.LED;
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
}
