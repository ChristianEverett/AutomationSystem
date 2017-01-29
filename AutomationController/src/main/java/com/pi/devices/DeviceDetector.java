/**
 * 
 */
package com.pi.devices;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.DeviceState;
import com.pi.model.Event;

/**
 * @author Christian Everett
 *
 */
public class DeviceDetector extends Device
{
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	private Task scanningTask = null;
	private Map<String, Event> addressEventMap;
	private PrintWriter writer = new PrintWriter(new FileWriter("MAC.log"));

	public DeviceDetector(String name, List<String> addresses, List<Event> events) throws IOException
	{
		super(name);
		addressEventMap = new HashMap<>(addresses.size());
		
//		for(int x = 0; x < addresses.size() && x < events.size(); x++)
//		{
//			Matcher match = regex.matcher(addresses.get(x));
//			if(match.matches())
//				addressEventMap.put(addresses.get(x), events.get(x));
//			else
//				Application.LOGGER.severe("MAC Address is not in a valid format: " + addresses.get(x));
//		}
//		setupScanner();
//		
//		for(String address : addresses)
//			registerAddress(address);
//		
//		scanningTask = createTask(() -> 
//		{
//			try
//			{
//				String MAC = scan();
//				//addressEventMap.get(MAC).triggerEvent();
//				writer.println("Got MAC: " + MAC);
//			}
//			catch (Throwable e)
//			{
//				Application.LOGGER.severe(e.getMessage());
//			}
//		}, 5L, 1L, TimeUnit.SECONDS);
	}

	@Override
	public String getType()
	{
		return DeviceType.DEVICE_SENSOR;
	}

	@Override
	public void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState()
	{
		DeviceState state = new DeviceState(name);
		ArrayList<Map.Entry<String, Boolean>> triggeredEvents = new ArrayList<>(addressEventMap.size());
		
		for(String key : addressEventMap.keySet())
			triggeredEvents.add(new AbstractMap.SimpleEntry<String, Boolean>(key, addressEventMap.get(key).isTriggered()));
		
		state.setParam(DeviceState.EVENTS, triggeredEvents);
		
		return state;
	}

	@Override
	public void close()
	{
		scanningTask.cancel();
		stopScanning();
	}

	private native void setupScanner();
	private native void registerAddress(String address);
	private native String scan();
	private native void stopScanning();

	static
	{
		System.loadLibrary("ARPDriver");
	}
	
	@XmlRootElement(name = DEVICE)
	public static class DeviceDetectorConfig extends DeviceConfig
	{
		private List<String> addresses;
		private List<Event> events;

		@Override
		public Device buildDevice() throws IOException
		{
			return new DeviceDetector(name, addresses, events);
		}

		@XmlElement
		public void setAddress(List<String> addresses)
		{
			this.addresses = addresses;
		}
		
		public List<String> getAddress()
		{
			return addresses;
		}
		
		@XmlElement
		public void setEvent(List<Event> events)
		{
			this.events = events;
		}
		
		public List<Event> getEvent()
		{
			return events;
		}
	}
}
