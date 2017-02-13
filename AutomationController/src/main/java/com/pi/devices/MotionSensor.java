/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * @author Christian Everett
 *
 */
public class MotionSensor extends Device
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
				DeviceState d = new DeviceState("pi_led1");
				d.setParam("isOn", gpioEvent.getState().isHigh() ? false : true);
				Device.queueAction(d);
			}
		});
	}

	@Override
	public void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState()
	{
		DeviceState state = new DeviceState(name);
		
		state.setParam(DeviceState.IS_ON, gpioPin.isHigh());
		
		return state;
	}

	@Override
	public void close()
	{
		gpioController.unprovisionPin(gpioPin);
	}

	@Override
	public String getType()
	{
		return DeviceType.MOTION_SENSOR;
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
