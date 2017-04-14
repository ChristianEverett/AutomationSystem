package com.pi.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.pi.Application;
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
	
	public boolean scheduleAction(DeviceState state)
	{
		Device device = deviceMap.get(state.getName());
		
		if(device == null)
			return false;
		
		device.execute(state);
		return true;
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
				Application.LOGGER.info("Closing: " + name);
				device.close();
				return true;
			}
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Failed to close " + name + " Got:" + e.getMessage());
		}

		return false;
	}

	public void closeAllDevices()
	{
		deviceMap.entrySet().forEach((Entry<String, Device> entry) ->
		{
			Application.LOGGER.info("closing: " + entry.getKey());
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
			Application.LOGGER.severe(e.getMessage());
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
	
	public synchronized String createNewDevice(DeviceConfig config, boolean isRemoteDevice)
	{
		try
		{
			if (deviceMap.containsKey(config.getName()))
				return null;		

			Device device = config.buildDevice();
			deviceMap.put(config.getName(), device);
			Application.LOGGER.info("Loaded: " + config.getName());
			return config.getName();
		}
		catch (Exception e)
		{
			Application.LOGGER.severe("Error creating Device: " + config.getName() + ". Exception: " + e.getMessage());
		}
		
		return null;
	}
}
