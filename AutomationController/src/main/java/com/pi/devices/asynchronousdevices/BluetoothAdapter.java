package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService.Task;

public class BluetoothAdapter extends AsynchronousDevice
{//TODO refactor class
	private static final String MAC_ADDRESS_REGEX = "([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$";
	private Pattern regex = Pattern.compile(MAC_ADDRESS_REGEX);
	private Map<String, Boolean> macToLastPing = new ConcurrentHashMap<>();
	private Task scanningTask = null;

	public BluetoothAdapter(String name) throws IOException
	{
		super(name);
		setupBluetooth();
		createAsynchronousTask(5L, 1L, TimeUnit.SECONDS);
	}

	@Override
	public void update() throws InterruptedException, IOException
	{
		Set<String> keys = macToLastPing.keySet();
		
		for(String key : keys)
		{
			String result = ping(key);
			if(!result.isEmpty())
			{
				macToLastPing.put(key, true);
			}
			else
			{
				macToLastPing.put(key, false);
			}
		}
		
		if (keys.isEmpty())
			Thread.sleep(2000);
		else
			update(getState());
	}

	@Override
	protected void performAction(DeviceState state)
	{
		@SuppressWarnings("unchecked")
		List<String> addresses = (List<String>) state.getParamNonNull(Params.MACS);
		Boolean runScan = state.getParamTyped(Params.SCAN, Boolean.class, false);
		
		for (String address : addresses)
		{
			Matcher match = regex.matcher(address);
			if (match.matches())
			{
				macToLastPing.put(address, false);
			} 
		}
		if(runScan)
		{
			String newAddresses[] = scanForBluetoothDevices();
			
			for(String item : newAddresses)
			{
				macToLastPing.put(item, true);
			}
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase) throws IOException
	{
		DeviceState state = Device.createNewDeviceState(name);
		
		if (forDatabase)
		{
			state.setParam(Params.MACS, new ArrayList<>(macToLastPing.keySet()));
			state.setParam(Params.SCAN, false);
		}
		else 
		{
			for (Entry<String, Boolean> entry : macToLastPing.entrySet())
			{
				state.setParam(entry.getKey(), entry.getValue());
			}
		}
		
		return state;
	}

	@Override
	protected void tearDown() throws IOException
	{
		closeBluetooth();
	}
	
	private native void setupBluetooth();
	private native String[] scanForBluetoothDevices();
	private native String ping(String address);
	private native void closeBluetooth();
	
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
