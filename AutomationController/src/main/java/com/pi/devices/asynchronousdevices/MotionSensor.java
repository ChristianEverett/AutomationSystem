/**
 * 
 */
package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * @author Christian Everett
 *
 */
public class MotionSensor extends AsynchronousDevice
{
	private final GpioPinDigitalInput gpioPin;
	
	public MotionSensor(String name, int headerPin) throws IOException
	{
		super(name);
		gpioPin = gpioController.provisionDigitalInputPin(GPIO_PIN.getWiringPI_Pin(headerPin), name);
		
		gpioPin.addListener(new GpioPinListenerDigital()
		{
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpioEvent)
			{	
				MotionSensor.super.run();
			}
		});
	}

	@Override
	protected void update() throws Exception
	{
	}
	
	@Override
	protected void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		
		state.setParam(Params.IS_ON, gpioPin.isHigh());
		
		return state;
	}

	@Override
	protected void tearDown()
	{
		gpioController.unprovisionPin(gpioPin);
	}
	
	@XmlRootElement(name = DEVICE)
	public static class MotionSensorConfig extends DeviceConfig
	{
		private int headerPin; 

		@Override
		public Device buildDevice() throws IOException
		{
			return new MotionSensor(name, headerPin);
		}

		@XmlElement
		public void setHeader(int headerPin)
		{
			this.headerPin = headerPin;
		}
	}
}
