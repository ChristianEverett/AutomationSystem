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
	
	public Switch(int headerPin, String name) throws IOException
	{
		this.headerPin = headerPin;
		gpioPin = gpioController.provisionDigitalOutputPin(pins.get(headerPin).getWiringPI_Pin(), name, PinState.HIGH);
	}

	@Override
	public boolean performAction(Action action)
	{
		if(isClosed)
			return false;
		
		gpioPin.setState(!Boolean.parseBoolean(action.getData()));

		return true;
	}

	@Override
	public void close()
	{
		gpioPin.setState(PinState.HIGH);
		isClosed = true;
	}
}
