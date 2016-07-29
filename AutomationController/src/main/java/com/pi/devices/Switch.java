/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import com.pi.infrastructure.Device;
import com.pi.repository.Action;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;


/**
 * @author Christian Everett
 *
 */
public class Switch extends Device
{
	private final GpioPinDigitalOutput gpioPin;
	
	public Switch(String name, int headerPin) throws IOException
	{
		super(name);
		this.headerPin = headerPin;
		gpioPin = gpioController.provisionDigitalOutputPin(pins.get(headerPin).getWiringPI_Pin(), name, PinState.HIGH);
	}

	@Override
	public void performAction(Action action)
	{
		if(isClosed)
			return;
		
		gpioPin.setState(!Boolean.parseBoolean(action.getData()));
	}

	@Override
	public void close()
	{
		gpioPin.setState(PinState.HIGH);
		isClosed = true;
	}

	@Override
	public Action getState()
	{
		if(isClosed)
			return null;
		return new Action(name, String.valueOf(gpioPin.isLow()));
	}
}
