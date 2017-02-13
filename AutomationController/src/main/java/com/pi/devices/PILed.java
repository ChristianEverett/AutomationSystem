package com.pi.devices;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.DeviceState;

public class PILed extends Device
{
	private static final String PI_LED_PATH = "/sys/class/leds/led0/brightness";
	private FileWriter writer = null;
	private FileReader reader = null;
	
	private static final String OFF = "0";
	private static final String ON = "1";
	
	private Boolean ledOn = false;

	public PILed(String name) throws IOException
	{
		super(name);
		writer = new FileWriter(PI_LED_PATH);
		reader = new FileReader(PI_LED_PATH);
	}

	@Override
	public String getType()
	{
		return DeviceType.PI_LED;
	}

	@Override
	public void performAction(DeviceState state)
	{
		Boolean isOn = (Boolean) state.getParam(DeviceState.IS_ON);
		
		try
		{
			if(isOn)
			{
				writer.write(ON);
				writer.flush();
				ledOn = true;
			}
			else 
			{
				writer.write(OFF);
				writer.flush();
				ledOn = false;
			}
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}

	@Override
	public DeviceState getState() throws IOException
	{
		DeviceState state = new DeviceState(name);
		state.setParam(DeviceState.IS_ON, ledOn);
		
		return state;
	}

	@Override
	public void close() throws IOException
	{
		writer.write(OFF);
		writer.close();
		reader.close();
	}

	@XmlRootElement(name = DEVICE)
	public static class PILedConfig extends DeviceConfig
	{
		@Override
		public Device buildDevice() throws IOException
		{
			return new PILed(name);
		}
	}
}
