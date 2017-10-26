package com.pi.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.BaseNodeController;

public class DeviceState implements Serializable
{
	private String name;
	private String type;

	private Map<String, Object> params = new HashMap<>();
	
	public DeviceState()
	{
		
	}
	
	public DeviceState(String name, String type)
	{	
		this.name = name;
		this.type = type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		type = BaseNodeController.getInstance().getDeviceType(name);
	}

	public Map<String, Object> getParams()
	{
		return params;
	}

	public void setParams(Map<String, Object> params)
	{
		this.params = params;
	}
	
	public String getType()
	{
		return type;
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	@JsonIgnore
	public void setParam(String key, Object object)
	{
		params.put(key, object);
	}
	
	@JsonIgnore
	public Object getParam(String key, boolean required) 
	{		
		try
		{
			return getParamNonNull(key);
		}
		catch (RuntimeException e)
		{
			if(required)
				throw e;			
		}
		
		return null;
	}
	
	@JsonIgnore
	public Object getParamNonNull(String key)
	{		
		Object object = params.get(key);
		
		if(object == null)
			throw new RuntimeException("Could not find param -> " + key);
		
		return object;
	}
	
	@JsonIgnore
	public <T> T getParamTypedNonNull(String key)
	{
		return (T) getParamNonNull(key);
	}
	
	@JsonIgnore
	public <T> T getParamTyped(String key, T defaultValue)
	{
		try
		{
			return getParamTypedNonNull(key);
		}
		catch (RuntimeException e)
		{
		}
		
		return defaultValue;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if(object == null)
			return false;
		if(!(object instanceof DeviceState))
			return false;
		DeviceState deviceState = (DeviceState) object;
		
		return Objects.equals(name, deviceState.getName()) && params.equals(deviceState.getParams());
	}

	@JsonIgnore
	public boolean contains(DeviceState state)
	{	
		if(state == null)
			return false;
		
		for(String paramName : state.params.keySet())
		{
			if(!Objects.equals(params.get(paramName), state.params.get(paramName)))
				return false;
		}
		
		return true;
	}
	
	public boolean hasData()
	{
		if(params.isEmpty())
			return false;
		
		return (params.containsKey(Params.LOCK) && params.size() > 1) || (!params.containsKey(Params.LOCK));
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name, params.hashCode());
	}
}
