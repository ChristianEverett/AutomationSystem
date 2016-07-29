/**
 * 
 */
package com.pi.repository;

/**
 * @author Christian Everett
 *
 */

public class Action
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
}
