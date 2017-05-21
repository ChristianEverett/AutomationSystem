package com.pi.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.SystemLogger;
import com.pi.infrastructure.DeviceType;

public class DeviceStateTriggerRange extends DeviceState
{
	private static final String COMPARE_TO = "compareTo";
	private Map<String, Object> endRange = new HashMap<>();
	private Boolean triggerOnChange = false;

	public Map<String, Object> getEndRange()
	{
		return endRange;
	}

	public void setEndRange(Map<String, Object> endRange)
	{
		this.endRange = endRange;
	}
	
	public void setTriggerOnChange(Boolean triggerOnChange)
	{
		this.triggerOnChange = triggerOnChange;
	}
	
	@JsonIgnore
	public boolean isTriggered(DeviceState state, DeviceState newState)
	{		
		if(state == null || !getName().equals(state.getName()) || !getType().equals(state.getType()))
			return false;
	
		if(triggerOnChange)
		{
			return newState != null && newState.getName().equals(getName());
		}
		
		if(endRange.isEmpty())
			return state.contains(this);
		
		for (String key : getParams().keySet())
		{
			Object value = state.getParam(key);
			Object start = getParam(key);
			Object end = endRange.get(key);


			if (!checkBound(start, value, key) || !checkBound(value, end, key))
				return false;
		} 
		
		
		return true;
	}

	private boolean checkBound(Object lower, Object greater, String key)
	{
		if(lower == null)
			return false;
		if(greater == null)
			return true;
		
		try
		{
			Method method = lower.getClass().getMethod(COMPARE_TO, DeviceType.paramTypes.get(key));
			Integer compare = (Integer) method.invoke(lower, greater);
			return (compare < 1) ? true : false;
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
		
		return false;
	}

	@Override
	@JsonIgnore
	public boolean equals(Object object)
	{
		if(!(object instanceof DeviceStateTriggerRange))
			return false;
		DeviceStateTriggerRange range = (DeviceStateTriggerRange) object;
		
		return super.equals(range) && getEndRange().equals(range.getEndRange());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), endRange.hashCode());
	}
}
