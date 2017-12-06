package com.pi.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EventHandler")
public class EventHandler extends Model
{
	@Id
	private int id;
	// In order for the event to be considered triggered, all device states in
	// triggerEvents must be met
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(length = 3000) 
	private List<DeviceStateTriggerRange> triggerStates = new LinkedList<>();

	private String actionProfileName;
	
	private Boolean requireAll = true;
	private Boolean unTrigger = false; 
	
	public EventHandler()
	{
		
	}

	@JsonIgnore
	public boolean checkIfTriggered(Map<String, DeviceState> currentStates, DeviceState newState)
	{
		if (requireAll)
		{
			return allStatesMet(currentStates, newState);
		}
		else
		{
			return anyStateMet(currentStates, newState);
		}
	}

	@JsonIgnore
	private boolean allStatesMet(Map<String, DeviceState> currentStates, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerStates)
		{
			if (!triggerState.isTriggered(currentStates.get(triggerState.getName()), newState))
				return false;
		}

		return true;
	}

	@JsonIgnore
	private boolean anyStateMet(Map<String, DeviceState> currentStates, DeviceState newState)
	{
		for (DeviceStateTriggerRange triggerState : triggerStates)
		{
			if (triggerState.isTriggered(currentStates.get(triggerState.getName()), newState))
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
	
	@JsonIgnore
	@Override
	public int hashCode()
	{
		return Objects.hash(triggerStates, actionProfileName);
	}
	
	// Json Fields ------------------------------------
	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
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

	public Boolean getUnTrigger()
	{
		return unTrigger;
	}

	public void setUnTrigger(Boolean unTrigger)
	{
		this.unTrigger = unTrigger;
	}
}
