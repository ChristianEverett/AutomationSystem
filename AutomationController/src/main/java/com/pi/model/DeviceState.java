package com.pi.model;

import java.util.HashMap;
import java.util.Objects;

import org.hibernate.type.SetType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.NodeController;

public class DeviceState extends DatabaseElement
{
	private String name;
	private String type;
	private HashMap<String, Object> params = new HashMap<>();
	
	public DeviceState()
	{	
	}
	
	public static DeviceState create(String name, String type)
	{
		DeviceState state = new DeviceState();
		state.name = name;
		state.type = type;
		return state;
	}
	
	public static DeviceState load(String name, String type, Integer id)
	{
		DeviceState state = new DeviceState();
		state.setName(name);
		state.setDatabaseID(id);
		state.type = type;
		return state;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		type = NodeController.getInstance().getDeviceType(name);
	}

	public HashMap<String, Object> getParams()
	{
		return params;
	}

	public void setParams(HashMap<String, Object> params)
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
	
	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof DeviceState))
			return false;
		DeviceState deviceState = (DeviceState) object;
		
		return Objects.equals(name, deviceState.getName()) && params.equals(deviceState.getParams());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, params.hashCode());
	}

	@Override
	public Object getDatabaseIdentification()
	{
		return name;
	}

	@Override
	public String getDatabaseIdentificationForQuery()
	{
		return name;
	}
}
