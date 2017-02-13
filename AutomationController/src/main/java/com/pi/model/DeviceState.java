package com.pi.model;

import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeviceState extends DatabaseElement
{
	private String name;
	private HashMap<String, Object> params = new HashMap<>();
	
	public final static String RED = "red";
	public final static String GREEN = "green";
	public final static String BLUE = "blue";
	
	public final static String IS_ON = "isOn";
	
	public final static String TEMPATURE = "temperature";
	public final static String HUMIDITY = "humidity";
	
	public final static String TARGET_TEMPATURE = "target_temp";
	public final static String MODE = "mode";
	public final static String TARGET_MODE = "target_mode"; 
	
	public final static String MAC = "mac";
	
	public DeviceState()
	{
		
	}
	
	public DeviceState(String name)
	{
		this.name = name;
	}
	
	public DeviceState(String name, Integer id)
	{
		super(id);
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public HashMap<String, Object> getParams()
	{
		return params;
	}

	public void setParams(HashMap<String, Object> params)
	{
		this.params = params;
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
		return toMySqlString(name);
	}
}
