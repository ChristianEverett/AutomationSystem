package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;
import com.pi.model.MacAddress;
import com.pi.model.repository.RepositoryType;
import com.pi.services.TaskExecutorService.Task;

public class BluetoothAdapter extends AsynchronousDevice
{
	private Map<MacAddress, Boolean> macToLastPing = new ConcurrentHashMap<>();
	private Task scanningTask = null;

	public BluetoothAdapter(String name) throws IOException
	{
		super(name);
		setupBluetooth();
		
		Collection<MacAddress> addresses = getRepositoryValues(RepositoryType.MACAddress);
		addresses = addresses.stream().filter(address -> address.isBluetoothAddress()).collect(Collectors.toList());
		
		for(MacAddress address : addresses)
				macToLastPing.put(address, false);
		
		start(5L, 1L, TimeUnit.SECONDS);
	}

	@Override
	public void update() throws Exception 
	{
		Set<MacAddress> keys = macToLastPing.keySet();
		
		for(MacAddress key : keys)
		{
			String result = synchronizedPing(key.getAddressString());

			macToLastPing.put(key, !result.isEmpty());
		}
		
		if (keys.isEmpty())
			Thread.sleep(2000);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		@SuppressWarnings("unchecked")
		List<String> addresses = (List<String>) state.getParamNonNull(Params.MACS);
		Boolean runScan = state.getParamTyped(Params.SCAN, false);
		
		for (String address : addresses)
		{
			MacAddress macAddress = new MacAddress(address);
			macToLastPing.put(macAddress, false);	
		}
		if(runScan)
		{
			String newAddresses[] = scanForBluetoothDevices();
			
			for(String item : newAddresses)
			{
				String result = synchronizedPing(item);
				if (!result.isEmpty())
				{
					MacAddress macAddress = new MacAddress(item);
					macToLastPing.put(macAddress, true);
					setRepositoryValue(RepositoryType.MACAddress, result, macAddress);
				}
			}
		}
	}

	@Override
	public DeviceState getState(DeviceState state)
	{
		for (Entry<MacAddress, Boolean> entry : macToLastPing.entrySet())
		{
			state.setParam(entry.getKey().getAddressString(), entry.getValue());
		}
		
		return state;
	}

	private String synchronizedPing(String address)
	{
		synchronized (this)
		{
			return ping(address);
		}
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
