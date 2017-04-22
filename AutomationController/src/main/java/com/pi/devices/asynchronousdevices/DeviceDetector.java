/**
 * 
 */
package com.pi.devices.asynchronousdevices;

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
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */
public class DeviceDetector extends AsynchronousDevice
{
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	private Task scanningTask = null;

	private String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
	private HashMap<String, Date> macToTimestamp = new HashMap<>();
	
	public DeviceDetector(String name) throws IOException
	{
		super(name, 10000L, 1L, TimeUnit.MILLISECONDS);
		setupScanner();	
	}

	private void registerMACAddress(String address)
	{
		Matcher match = regex.matcher(address);
		if(match.matches())
		{
			registerAddress(address);
			macToTimestamp.put(address, null);
		}
		else
			Application.LOGGER.severe("MAC Address is not in a valid format: " + address);
	}

	@Override
	protected void update() throws Exception
	{
		String MAC = scan();
		macToTimestamp.put(MAC, new Date());	
	}
	
	@Override
	public String getType()
	{
		return DeviceType.DEVICE_SENSOR;
	}

	@Override
	protected void performAction(DeviceState state)
	{
		@SuppressWarnings("unchecked")
		List<String> addresses = (List<String>) state.getParam(Params.MACS);
		
		for (String element : addresses)
		{
			registerMACAddress(element);
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		
		if (!forDatabase)
		{
			state.setParam(Params.MACS, new ArrayList<>(macToTimestamp.entrySet()));
		}
		else
		{
			state.setParam(Params.MACS, new ArrayList<>(macToTimestamp.keySet()));
		}
		
		return state;
	}
	
	@Override
	public List<String> getExpectedParams()
	{
		List<String> list = new ArrayList<>();
		list.add(Params.MACS);
		return list;
	}

	@Override
	protected void tearDown()
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
			return new DeviceDetector(name);
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
