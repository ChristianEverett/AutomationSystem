package com.pi.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.mockito.internal.matchers.Contains;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.NodeController;

public class DeviceState extends DatabaseElement
{
	private String name;
	private String type;
	private Map<String, Object> params = new HashMap<>();
	
	public DeviceState()
	{	
	}
	
	public static DeviceState create(String name)
	{
		DeviceState state = new DeviceState();
		state.setName(name);
		return state;
	}
	
	public static DeviceState create(String name, String type)
	{
		DeviceState state = new DeviceState();
		state.name = name;
		state.type = type;
		return state;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		type = NodeController.getInstance().getDeviceType(name);
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
	public Object getParam(String key)
	{		
		return params.get(key);
	}
	
	@JsonIgnore
	public <T> T getParamTyped(String key, Class<T> type)
	{
		return type.cast(params.get(key));
	}
	
	@JsonIgnore
	public <T> T getParamTyped(String key, Class<T> type, T defaultValue)
	{
		T value = getParamTyped(key, type);
		return value == null ? defaultValue : value;
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
		
		for(String paramName : state.getParams().keySet())
		{
			if(!state.getParam(paramName).equals(params.get(paramName)))
				return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name, params.hashCode());
	}

	@Override
	public int getDatabaseIdentification()
	{
		return name.hashCode();
	}
}
