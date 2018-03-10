package com.pi.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.SystemLogger;
import com.pi.devices.asynchronousdevices.Timer;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.DeviceTypeMap;
import com.pi.infrastructure.DeviceType.Params;

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
	
	public Boolean getTriggerOnChange()
	{
		return triggerOnChange;
	}
	
	@JsonIgnore
	public boolean isTriggered(DeviceState currentState, DeviceState newState)
	{		
		if(currentState == null || !getName().equals(currentState.getName()) || !getType().equals(currentState.getType()))
			return false;
	
		if(triggerOnChange)
		{
			return newState != null && newState.getName().equals(getName());
		}

		if(CollectionUtils.isEmpty(endRange))
			return currentState.contains(this);
		
		for (String key : getParams().keySet())
		{
			Object currentValue = currentState.getParam(key);
			Object start = getParam(key);
			Object end = endRange.get(key);

			if (!checkBound(start, currentValue, key) || !checkBound(currentValue, end, key))
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
			if(Params.TIME.equals(key))
			{
				try
				{
					Date time1 = Timer.formatter.parse((String) lower);
					Date time2 = Timer.formatter.parse((String) greater);
					
					return time1.before(time2);
				}
				catch (ParseException e)
				{
					SystemLogger.LOGGER.severe(e.getMessage());
				}
			}
			
			Method method = lower.getClass().getMethod(COMPARE_TO, DeviceTypeMap.getParamType(key));
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
