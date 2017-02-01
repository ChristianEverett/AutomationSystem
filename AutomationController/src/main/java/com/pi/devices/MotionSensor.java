/**
 * 
 */
package com.pi.devices;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi.model.Event;
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
	private List<Event> events;
	private PrintWriter writer = new PrintWriter(new FileWriter("Motion.log"));
	
	public MotionSensor(String name, int headerPin, List<Event> events) throws IOException
	{
		super(name);
		gpioPin = gpioController.provisionDigitalInputPin(GPIO_PIN.getWiringPI_Pin(headerPin), name);
		this.events = events;
		
		gpioPin.addListener(new GpioPinListenerDigital()
		{
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpioEvent)
			{
//				if(gpioEvent.getState().isHigh())
//				{
//					for(Event event : events)
//					{
//						event.triggerEvent();
//					}
//				}	
				writer.println("Motion triggered");
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
//		ArrayList<Boolean> triggeredEvents = new ArrayList<>(events.size());
//		
//		for(Event event : events)
//			triggeredEvents.add(event.isTriggered());
		
		state.setParam(DeviceState.IS_ON, gpioPin.isHigh());
//		state.setParam(DeviceState.EVENTS, triggeredEvents);
		
		return state;
	}

	@Override
	public void close()
	{
		gpioController.unprovisionPin(gpioPin);
		
		for(Event event : events)
			event.cancel();
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
		private List<Event> actions;

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
		public void setActions(List<Event> actions)
		{
			this.actions = actions;
		}
	}
}
