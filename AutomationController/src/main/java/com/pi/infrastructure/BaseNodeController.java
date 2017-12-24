package com.pi.infrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device.DeviceConfig;
import com.pi.infrastructure.util.DeviceDoesNotExist;
import com.pi.model.DeviceState;

public abstract class BaseNodeController implements NodeControllerAPI
{
	protected static BaseNodeController singleton = null;
	private ConcurrentHashMap<String, Device> deviceMap = new ConcurrentHashMap<>();
	
	protected static final String RMI_NAME = "node_controller";
	
	public static BaseNodeController getInstance()
	{
		if (singleton == null)
			throw new RuntimeException("Node has not been created");
		return singleton;
	}
	
	protected void addDevice(Device device)
	{
		deviceMap.put(device.getName(), device);
	}
	
	public void scheduleAction(DeviceState state)
	{
		Device device = deviceMap.get(state.getName());
		
		if(device == null)
			throw new DeviceDoesNotExist(state.getName());
		
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
		try
		{
			Device device = lookupDevice(name);
			return (device != null) ? device.getCurrentDeviceState() : null;
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			return null;
		}
	}
	
	public List<DeviceState> getStates()
	{
		List<DeviceState> stateList = new ArrayList<>();

		for (Entry<String, Device> device : deviceMap.entrySet())
		{
			DeviceState state = getDeviceState(device.getKey());
			if (state != null)
				stateList.add(state);
		}

		return stateList;
	}
	
	public synchronized Device createNewDevice(DeviceConfig config) throws Exception
	{
		try
		{
			if (deviceMap.containsKey(config.getName()))
				return null;		

			Device device = config.buildDevice();
			addDevice(device);
			SystemLogger.getLogger().info("Loaded: " + config.getName());
			return device;
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
