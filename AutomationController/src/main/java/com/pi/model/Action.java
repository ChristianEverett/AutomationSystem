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

public class Action implements Serializable
{
	private String device;
	private String data;
	
	public Action()
	{
		
	}
	
	public Action(String device, String data)
	{
		this.device = device;
		this.data = data;
	}
	
	public Action(long id, String device, String data)
	{
		this.device = device;
		this.data = data;
	}
	
	public String getDevice()
	{
		return device;
	}
	public void setDevice(String device)
	{
		this.device = device;
	}
	public String getData()
	{
		return data;
	}
	public void setData(String data)
	{
		this.data = data;
	}

	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof Action))
			return false;
		Action action = (Action) object;
		
		return device.equals(action.device) && data.equals(action.data);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(device, data);
	}
}
