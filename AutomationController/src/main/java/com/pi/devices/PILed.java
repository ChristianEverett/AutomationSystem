package com.pi.devices;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
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
	protected void performAction(DeviceState state)
	{
		Boolean isOn = (Boolean) state.getParamNonNull(Params.ON);
		
		SetLedState(isOn);
	}

	private void SetLedState(Boolean isOn) 
	{
		try
		{
			if (isOn)
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
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public DeviceState getState(DeviceState state) 
	{
		state.setParam(Params.ON, ledOn);
		
		return state;
	}

	@Override
	protected void tearDown() throws IOException
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
