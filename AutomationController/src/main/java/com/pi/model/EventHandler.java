package com.pi.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "EventHandler")
public class EventHandler extends Model
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	// In order for the event to be considered triggered, all device states in
	// triggerEvents must be met
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(length = 3000) 
	private List<DeviceStateTriggerRange> triggerStates = new LinkedList<>();

	private String actionProfileName;
	
	private Boolean requireAll = true;
	
	public EventHandler()
	{
	}

	@JsonIgnore
	public boolean checkIfTriggered(Map<String, DeviceState> stateCache, DeviceState newState)
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
	private boolean allStatesMet(Map<String, DeviceState> stateCache, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerStates)
		{
			if (!triggerState.isTriggered(stateCache.get(triggerState.getName()), newState))
				return false;
		}

		return true;
	}

	@JsonIgnore
	private boolean anyStateMet(Map<String, DeviceState> stateCache, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerStates)
		{
			if (triggerState.isTriggered(stateCache.get(triggerState.getName()), newState))
				return true;
		}

		return false;
	}

	@JsonIgnore
	public List<String> getDependencyDevices()
	{
		List<String> deviceNames = new ArrayList<>(triggerStates.size());

		for (DeviceStateTriggerRange state : triggerStates)
		{
			deviceNames.add(state.getName());
		}

		return deviceNames;
	}
	
	// Json Fields ------------------------------------
	@Override
	@JsonGetter
	public int hashCode()
	{
		return Objects.hash(triggerStates.hashCode(), requireAll);
	}
	
	public String getActionProfileName()
	{
		return actionProfileName;
	}

	public void setActionProfileName(String name)
	{	
		this.actionProfileName = name;
	}
	
	public List<DeviceStateTriggerRange> getTriggerStates()
	{
		return triggerStates;
	}

	public void setTriggerStates(List<DeviceStateTriggerRange> triggerStates)
	{
		this.triggerStates = triggerStates;
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
