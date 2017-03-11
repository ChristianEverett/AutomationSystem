package com.pi.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Event extends DatabaseElement
{
	//In order for the event to be considered triggered, all device states in triggerEvents must be met
	private List<DeviceState> triggerEvents = new LinkedList<>();
	//Map to hold this events current view of the dependency devices and there last known state
	private HashMap<String, DeviceState> triggerSetStateCache = new HashMap<>();
	
	private List<Entry<String, DeviceState>> registeredDevices = new LinkedList<>();

	public Event()
	{
	}
	
	@JsonIgnore
	public boolean updateAndCheckIfTriggered(DeviceState state)
	{
		triggerSetStateCache.put(state.getName(), state);

		for(DeviceState triggerState : triggerEvents)
		{
			if(!triggerState.equals(triggerSetStateCache.get(triggerState.getName())))
				return false;
		};

		return true;
	}

	@JsonIgnore
	public List<String> getDependencyDevices()
	{
		List<String> deviceNames = new ArrayList<String>(triggerEvents.size());
		
		for(DeviceState state : triggerEvents)
		{
			deviceNames.add(state.getName());
		}
		
		return deviceNames;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(triggerEvents.hashCode(), triggerSetStateCache.hashCode());
	}
	
	@JsonIgnore
	public void registerListener(DeviceState state)
	{
		registeredDevices.add(new AbstractMap.SimpleEntry<String, DeviceState>(state.getName(), state));
	}

	@JsonIgnore
	public void unRegisterListener(String deviceName)
	{
		for(Iterator<Entry<String, DeviceState>> iter = registeredDevices.iterator(); iter.hasNext();)
			if(iter.next().getKey().equals(deviceName))
				iter.remove();
	}
	
	@JsonIgnore
	public void setTriggerSetStateCache(HashMap<String, DeviceState> triggerSetStateCache)
	{
		this.triggerSetStateCache = triggerSetStateCache;
	}
	
	//Json Fields ------------------------------------
	public List<Entry<String, DeviceState>> getRegisteredDevices()
	{
		return registeredDevices;
	}
	
	public List<DeviceState> getTriggerEvents()
	{
		return triggerEvents;
	}

	public void setTriggerEvents(List<DeviceState> triggerEvents)
	{
		this.triggerEvents = triggerEvents;
	}

	public HashMap<String, DeviceState> getTriggerSetStateCache()
	{
		return triggerSetStateCache;
	}
}
