/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;


/**
 * @author Christian Everett
 *
 */
public class Switch extends Device
{
	private int headerPin = -1;
	private final GpioPinDigitalOutput gpioPin;
	
	public Switch(String name, int headerPin) throws IOException
	{
		super(name);
		this.headerPin = headerPin;
		gpioPin = gpioController.provisionDigitalOutputPin(pins.get(headerPin).getWiringPI_Pin(), name, PinState.HIGH);
	}

	@Override
	public void performAction(DeviceState state)
	{
		gpioPin.setState(! (Boolean)state.getParam(DeviceState.IS_ON));
	}

	@Override
	public void close()
	{
		gpioPin.setState(PinState.HIGH);
		gpioController.unprovisionPin(gpioPin);
	}

	@Override
	public DeviceState getState()
	{
		DeviceState state = new DeviceState(name);
		state.setParam(DeviceState.IS_ON, gpioPin.isLow());
		
		return state;
	}

	@Override
	public String getType()
	{
		return DeviceType.SWITCH;
	}
	
	@XmlRootElement(name = DEVICE)
	public static class SwitchConfig extends DeviceConfig
	{
		private int header;
		
		@Override
		public Device buildDevice() throws IOException
		{
			return new Switch(name, header);
		}
	
		@XmlElement
		public void setHeader(int header)
		{
			this.header = header;
		}
	}
}
