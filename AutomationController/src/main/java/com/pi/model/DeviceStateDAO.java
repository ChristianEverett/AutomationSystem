package com.pi.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SavedDeviceStates")
public class DeviceStateDAO extends Model
{
	@Id
	@Column(length = 100)
	private String deviceName;
	
	@Column(length = 3000) 
	private DeviceState deviceState;
	
	public DeviceStateDAO()
	{
		
	}

	public DeviceStateDAO(DeviceState deviceState)
	{
		this.deviceName = deviceState.getName();
		this.deviceState = deviceState;
	}
	
	public String getDeviceName()
	{
		return deviceName;
	}

	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}

	public DeviceState getDeviceState()
	{
		return deviceState;
	}

	public void setDeviceState(DeviceState deviceState)
	{
		this.deviceState = deviceState;
	}
}
