package com.pi.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ActionProfile")
public class ActionProfile extends Model
{
	@Id
	@Column(length = 100)
	private String name;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(length = 3000) 
	private Set<DeviceState> deviceStates = new HashSet<>();
	
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(length = 3000) 
	private Set<DeviceState> invertedDeviceStates = new HashSet<>();
	
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

	public void setName(String name)
	{
		this.name = name;
	}
	
	public Set<DeviceState> getDeviceStates()
	{
		return deviceStates;
	}

	public void setDeviceStates(Set<DeviceState> deviceStates)
	{
		this.deviceStates = deviceStates;
	}

	public Set<DeviceState> getInvertedDeviceStates()
	{
		return invertedDeviceStates;
	}

	public void setInvertedDeviceStates(Set<DeviceState> invertedDeviceStates)
	{
		this.invertedDeviceStates = invertedDeviceStates;
	}
}
