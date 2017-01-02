/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.Action;
import com.pi.model.DeviceState;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 * @author Christian Everett
 *
 */
public class MotionSensor extends Device
{
	private final GpioPinDigitalInput gpioPin;
	private List<Action> actions;
	
	public MotionSensor(String name, int headerPin, List<Action> actions) throws IOException
	{
		super(name);
		gpioPin = gpioController.provisionDigitalInputPin(pins.get(headerPin).getWiringPI_Pin(), name);
		this.actions = actions;
		
		gpioPin.addListener(new GpioPinListenerDigital()
		{
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event)
			{
				if(event.getState().isHigh())
				{
					for(Action action : actions)
					{
						queueAction(action);
					}
				}	
			}
		});
	}

	@Override
	public void performAction(Action action)
	{
		
	}

	@Override
	public DeviceState getState()
	{
		return new MotionSensorState(name, gpioPin.isHigh());
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
	
	public static class MotionSensorState extends DeviceState
	{
		private boolean motionSensed;
		
		public MotionSensorState(String deviceName, boolean motionSensed)
		{
			super(deviceName);
			this.motionSensed = motionSensed;
		}

		public boolean getMotionSensed()
		{
			return motionSensed;
		}

		@Override
		public String getType()
		{
			return DeviceType.MOTION_SENSOR;
		}
	}
	
	@XmlRootElement(name = DEVICE)
	public static class MotinoSensorConfig extends DeviceConfig
	{
		private int headerPin; 
		private List<Action> actions;

		@Override
		public Device buildDevice() throws IOException
		{
			return new MotionSensor(name, headerPin, actions);
		}

		@XmlElement
		public void setHeader(int headerPin)
		{
			this.headerPin = headerPin;
		}

		@XmlElement
		public void setActions(List<Action> actions)
		{
			this.actions = actions;
		}
	}
}
