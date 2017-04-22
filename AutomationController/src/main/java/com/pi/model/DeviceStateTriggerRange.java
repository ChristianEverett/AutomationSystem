package com.pi.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.Application;
import com.pi.infrastructure.DeviceType;

public class DeviceStateTriggerRange extends DeviceState
{
	private static final String COMPARE_TO = "compareTo";
	private HashMap<String, Object> endRange = new HashMap<>();

	public HashMap<String, Object> getEndRange()
	{
		return endRange;
	}

	public void setEndRange(HashMap<String, Object> endRange)
	{
		this.endRange = endRange;
	}
	
	@JsonIgnore
	public boolean isInRange(DeviceState state)
	{		
		if(!getName().equals(state.getName()) || !getType().equals(state.getType()))
			return false;
	
		for (String key : getParams().keySet())
		{
			Object value = state.getParam(key);
			Object start = getParam(key);
			Object end = endRange.get(key);

			if (!endRange.isEmpty())
			{
				if (!checkBound(start, value, key) || !checkBound(value, end, key))
					return false;
			}
			else if(!start.equals(value))
			{
				return false;
			}
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
			Application.LOGGER.severe(e.getMessage());
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
