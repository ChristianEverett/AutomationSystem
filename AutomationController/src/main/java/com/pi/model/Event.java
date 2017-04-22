package com.pi.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.NodeController;

import java.util.Objects;

public class Event extends DatabaseElement
{
	// In order for the event to be considered triggered, all device states in
	// triggerEvents must be met
	private List<DeviceStateTriggerRange> triggerEvents = new LinkedList<>();

	private List<DeviceState> triggerStates = new LinkedList<>();
//	private List<DeviceState> invertedTriggerStates = new LinkedList<>();
	
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
		triggerStates.add(state);
	}

	@JsonIgnore
	public void unRegisterListener(String deviceName)
	{
		for (Iterator<DeviceState> iter = triggerStates.iterator(); iter.hasNext();)
			if (iter.next().getName().equals(deviceName))
				iter.remove();
	}
	
	@JsonIgnore
	public void replace(Event event)
	{
		setTriggerEvents(event.getTriggerEvents());
		setRequireAll(event.getRequireAll());
	}

	// Json Fields ------------------------------------
	public List<DeviceState> getTriggerStates()
	{
		return triggerStates;
	}

	public void setTriggerStates(List<DeviceState> devices)
	{
		triggerStates.addAll(devices);
	}
	
//	public List<DeviceState> getInvertedTriggerStates()
//	{
//		return invertedTriggerStates;
//	}
//
//	public void setInvertedTriggerStates(List<DeviceState> devices)
//	{
//		invertedTriggerStates.addAll(devices);
//	}
	
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
