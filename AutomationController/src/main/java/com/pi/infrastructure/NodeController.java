package com.pi.infrastructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.Application;
import com.pi.SystemLogger;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.model.DeviceState;

public abstract class NodeController
{
	protected static NodeController singleton = null;
	
	protected Multimap<String, DeviceConfig> uninitializedRemoteDevices = ArrayListMultimap.create();
	protected HashMap<String, Device> deviceMap = new HashMap<>();
	
	public static NodeController getInstance()
	{
		if (singleton == null)
			throw new RuntimeException("Node has not been created");
		return singleton;
	}
	
	public abstract void update(DeviceState state);	
	
	public void scheduleAction(DeviceState state) throws IOException
	{
		Device device = deviceMap.get(state.getName());
		
		if(device == null)
			throw new RuntimeException("Device does not exist");
		
		device.execute(state);
	}
	
	/**
	 * @param name of device
	 * @return Device
	 */
	public final Device lookupDevice(String name)
	{
		return deviceMap.get(name);
	}

	public String getDeviceType(String name)
	{
		Device device = lookupDevice(name);
		
		return device != null ? device.getType() : DeviceType.UNKNOWN;
	}
	
	/**
	 * @return all devices
	 */
	public final Set<Entry<String, Device>> getDeviceSet()
	{
		return deviceMap.entrySet();
	}
	
	public boolean closeDevice(String name)
	{
		Device device = deviceMap.remove(name);

		try
		{
			if (device != null)
			{
				SystemLogger.getLogger().info("Closing: " + name);
				device.close();
				return true;
			}
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Failed to close " + name + " Got:" + e.getMessage());
		}

		return false;
	}

	public void closeAllDevices()
	{
		deviceMap.entrySet().forEach((Entry<String, Device> entry) ->
		{
			SystemLogger.getLogger().info("closing: " + entry.getKey());
			closeDevice(entry.getKey());
		});
		deviceMap.clear();
	}
	
	public DeviceState getDeviceState(String name)
	{
		return getDeviceState(name, false);
	}
	
	public DeviceState getDeviceState(String name, boolean isForDatabase)
	{
		try
		{
			Device device = deviceMap.get(name);
			return (device != null) ? device.getState(isForDatabase) : null;
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			return null;
		}
	}
	
	/**
	 * @param forDatabase
	 * @return all device states
	 */
	public List<DeviceState> getStates(Boolean forDatabase)
	{
		List<DeviceState> stateList = new ArrayList<>();

		for (Entry<String, Device> device : deviceMap.entrySet())
		{
			DeviceState state = getDeviceState(device.getKey(), forDatabase);
			if (state != null)
				stateList.add(state);
		}

		return stateList;
	}
	
	public synchronized String createNewDevice(DeviceConfig config, boolean isRemoteDevice) throws IOException
	{
		try
		{
			if (deviceMap.containsKey(config.getName()))
				return null;		

			Device device = config.buildDevice();
			deviceMap.put(config.getName(), device);
			SystemLogger.getLogger().info("Loaded: " + config.getName());
			return config.getName();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe("Error initialize Device: " + config.getName() + ". Exception: " + e.getMessage());
		}
		
		return null;
	}
	
	static
	{
		System.loadLibrary("AutomationDriver");
	}
}
