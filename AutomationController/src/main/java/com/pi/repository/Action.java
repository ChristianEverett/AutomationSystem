/**
 * 
 */
package com.pi.repository;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Christian Everett
 *
 */

@Entity
public class Action
{
	@Id
	private String command;
	private String data;
	
	public Action()
	{
		
	}
	
	public Action(String command, String data)
	{
		this.command = command;
		this.data = data;
	}
	
	public Action(long id, String command, String data)
	{
		this.command = command;
		this.data = data;
	}
	
	public String getCommand()
	{
		return command;
	}
	public void setCommand(String command)
	{
		this.command = command;
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
