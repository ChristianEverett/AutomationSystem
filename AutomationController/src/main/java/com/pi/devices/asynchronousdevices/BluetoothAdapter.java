package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;

public class BluetoothAdapter extends AsynchronousDevice
{
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	private Map<String, Date> macToLastPing = new ConcurrentHashMap<>();
	private Task scanningTask = null;

	public BluetoothAdapter(String name) throws IOException
	{
		super(name);
		setupBluetooth();
		
		scanningTask = createTask(() -> 
		{
			try
			{
				Set<String> keys = macToLastPing.keySet();
				
				for(String key : keys)
				{
					String result = ping(key);
					if(!result.isEmpty())
						macToLastPing.put(key, new Date());
				}
				
				if (keys.isEmpty())
					Thread.sleep(2000);
			}
			catch (Throwable e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, 1000L, 5L, TimeUnit.MILLISECONDS);
	}

	@Override
	public String getType()
	{
		return DeviceType.BLUETOOTH_ADAPTER;
	}

	@Override
	protected void performAction(DeviceState state)
	{//TODO finish class
		String address = (String) state.getParam(Params.MAC);
		Boolean runScan = (Boolean) state.getParam(Params.MAC);
		
		Matcher match = regex.matcher(address);
		
		if(match.matches())
		{
			macToLastPing.put(address, new Date());
		}
		
		if(runScan)
		{
			String addresses[] = scanForBluetoothDevices();
			
			for(String item : addresses)
			{
				macToLastPing.put(item, new Date());
			}
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase) throws IOException
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.MAC, new ArrayList<>(macToLastPing.entrySet()));
		return state;
	}

	@Override
	public void close() throws IOException
	{
		closeBluetooth();
	}

	@Override
	public List<String> getExpectedParams()
	{
		List<String> list = new ArrayList<>();
		list.add(Params.MAC);
		list.add(Params.SCAN);
		return list;
	}
	
	private native void setupBluetooth();
	private native String[] scanForBluetoothDevices();
	private native String ping(String address);
	private native void closeBluetooth();

	static
	{
		System.loadLibrary("BluetoothDriver");
	}
	
	@XmlRootElement(name = DEVICE)
	public static class BluetoothAdapterConfig extends DeviceConfig
	{
		@Override
		public Device buildDevice() throws IOException
		{
			return new BluetoothAdapter(name);
		}
	}
}
