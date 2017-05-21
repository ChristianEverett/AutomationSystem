package com.pi.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.NodeController;

import java.util.Objects;
import java.util.function.Function;

public class Event extends DatabaseElement
{
	// In order for the event to be considered triggered, all device states in
	// triggerEvents must be met
	private List<DeviceStateTriggerRange> triggerEvents = new LinkedList<>();

	private List<DeviceState> applyStates = new LinkedList<>();
	
	private Boolean requireAll = true;

	public Event()
	{
	}

	@JsonIgnore
	public boolean checkIfTriggered(Function<String, DeviceState> stateCache, DeviceState newState)
	{
		if (requireAll)
		{
			return allStatesMet(stateCache, newState);
		}
		else
		{
			return anyStateMet(stateCache, newState);
		}
	}

	@JsonIgnore
	private boolean allStatesMet(Function<String, DeviceState> stateCache, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerEvents)
		{
			if (!triggerState.isTriggered(stateCache.apply(triggerState.getName()), newState))
				return false;
		}

		return true;
	}

	@JsonIgnore
	private boolean anyStateMet(Function<String, DeviceState> stateCache, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerEvents)
		{
			if (triggerState.isTriggered(stateCache.apply(triggerState.getName()), newState))
				return true;
		}

		return false;
	}

	@JsonIgnore
	public List<String> getDependencyDevices()
	{
		List<String> deviceNames = new ArrayList<>(triggerEvents.size());

		for (DeviceStateTriggerRange state : triggerEvents)
		{
			deviceNames.add(state.getName());
		}

		return deviceNames;
	}

	@JsonIgnore
	public void registerListener(DeviceState state)
	{
		applyStates.add(state);
	}

	@JsonIgnore
	public void unRegisterListener(String deviceName)
	{
		for (Iterator<DeviceState> iter = applyStates.iterator(); iter.hasNext();)
			if (iter.next().getName().equals(deviceName))
				iter.remove();
	}
	
	@JsonIgnore
	public void replace(Event event)
	{
		setTriggerEvents(event.getTriggerEvents());
		setRequireAll(event.getRequireAll());
	}

	@Override
	@JsonIgnore
	public String getName()
	{
		return super.getName();
	}
	
	// Json Fields ------------------------------------
	@Override
	@JsonGetter
	public int hashCode()
	{
		return Objects.hash(triggerEvents.hashCode(), requireAll);
	}
	
	public List<DeviceState> getApplyStates()
	{
		return applyStates;
	}

	public void setApplyStates(List<DeviceState> devices)
	{
		applyStates.addAll(devices);
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
