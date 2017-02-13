/**
 * 
 */
package com.pi.devices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class DeviceDetector extends Device
{
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	private Task scanningTask = null;

	private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	private HashMap<String, Date> macToTimestamp = new HashMap<>();
	
	public DeviceDetector(String name, List<String> addresses) throws IOException
	{
		super(name);
		setupScanner();
		
		for(int x = 0; x < addresses.size(); x++)
		{
			Matcher match = regex.matcher(addresses.get(x));
			if(match.matches())
				registerAddress(addresses.get(x));
			else
				Application.LOGGER.severe("MAC Address is not in a valid format: " + addresses.get(x));
		}		
		
		scanningTask = createTask(() -> 
		{
			try
			{
				String MAC = scan();
				macToTimestamp.put(MAC, new Date());
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, 1000L, 1L, TimeUnit.MILLISECONDS);
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
		
		state.setParam(DeviceState.MAC, new ArrayList<>(macToTimestamp.entrySet()));
		
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

		@Override
		public Device buildDevice() throws IOException
		{
			return new DeviceDetector(name, addresses);
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
	}
}
