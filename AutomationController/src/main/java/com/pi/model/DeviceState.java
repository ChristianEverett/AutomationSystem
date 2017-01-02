/**
 * 
 */
package com.pi.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Christian Everett
 *
 */
public abstract class DeviceState implements Serializable
{
	private String deviceName;

	public DeviceState(String deviceName)
	{
		this.deviceName = deviceName;
	}
	
	public abstract String getType();
	
	/**
	 * @return the deviceName
	 */
	public String getDeviceName()
	{
		return deviceName;
	}

	/**
	 * @param deviceName the deviceName to set
	 */
	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof DeviceState))
			return false;
		DeviceState deviceState = (DeviceState) object;
		
		return Objects.equals(deviceName, deviceState.getDeviceName());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(deviceName);
	}
}
