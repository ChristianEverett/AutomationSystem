package com.pi.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.NodeController;

import java.util.Objects;

public class Event extends DatabaseElement
{
	// In order for the event to be considered triggered, all device states in
	// triggerEvents must be met
	private List<DeviceStateTriggerRange> triggerEvents = new LinkedList<>();
	// Map to hold this events current view of the dependency devices and there
	// last known state

	private List<Entry<String, DeviceState>> registeredDevices = new LinkedList<>();
	
	private Boolean requireAll = true;

	public Event()
	{
	}

	@JsonIgnore
	public boolean checkIfTriggered(NodeController node)
	{
		if (requireAll)
		{
			return allStatesMet(node);
		}
		else
		{
			return anyStateMet(node);
		}
	}

	@JsonIgnore
	private boolean allStatesMet(NodeController node)
	{
		for (DeviceStateTriggerRange triggerState : triggerEvents)
		{
			if (!triggerState.isInRange(node.getDeviceState(triggerState.getName())))
				return false;
		}

		return true;
	}

	@JsonIgnore
	private boolean anyStateMet(NodeController node)
	{
		for (DeviceStateTriggerRange triggerState : triggerEvents)
		{
			if (triggerState.isInRange(node.getDeviceState(triggerState.getName())))
				return true;
		}

		return false;
	}

	@JsonIgnore
	public List<String> getDependencyDevices()
	{
		List<String> deviceNames = new ArrayList<String>(triggerEvents.size());

		for (DeviceStateTriggerRange state : triggerEvents)
		{
			deviceNames.add(state.getName());
		}

		return deviceNames;
	}

	@Override
	@JsonGetter
	public int hashCode()
	{
		return Objects.hash(triggerEvents.hashCode(), requireAll);
	}

	@JsonIgnore
	public void registerListener(DeviceState state)
	{
		registeredDevices.add(new AbstractMap.SimpleEntry<String, DeviceState>(state.getName(), state));
	}

	@JsonIgnore
	public void unRegisterListener(String deviceName)
	{
		for (Iterator<Entry<String, DeviceState>> iter = registeredDevices.iterator(); iter.hasNext();)
			if (iter.next().getKey().equals(deviceName))
				iter.remove();
	}
	
	@JsonIgnore
	public void replace(Event event)
	{
		setTriggerEvents(event.getTriggerEvents());
		setRequireAll(event.getRequireAll());
	}

	// Json Fields ------------------------------------
	public List<Entry<String, DeviceState>> getRegisteredDevices()
	{
		return registeredDevices;
	}

	public void setRegisteredDevices(List<Entry<String, DeviceState>> devices)
	{
		registeredDevices.addAll(devices);
	}
	
	public List<DeviceStateTriggerRange> getTriggerEvents()
	{
		return triggerEvents;
	}

	public void setTriggerEvents(List<DeviceStateTriggerRange> triggerEvents)
	{
		this.triggerEvents = triggerEvents;
	}
	
	public void setRequireAll(Boolean value)
	{
		requireAll = value;
	}
	
	public Boolean getRequireAll()
	{
		return requireAll;
	}
}
