package com.pi.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActionProfile
{
	private String name;
	private Set<DeviceState> deviceStates = new HashSet<>();
	
	public ActionProfile()
	{
		
	}
	
	public ActionProfile(String name, Set<DeviceState> deviceStates)
	{
		this.name = name;
		this.deviceStates = deviceStates;
	}
	
	public String getName()
	{
		return name;
	}

	public Set<DeviceState> getDeviceStates()
	{
		return deviceStates;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setDeviceStates(Set<DeviceState> deviceStates)
	{
		this.deviceStates = deviceStates;
	}
}
