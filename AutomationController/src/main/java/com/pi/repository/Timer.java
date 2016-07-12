/**
 * 
 */
package com.pi.repository;

import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


/**
 * @author Christian Everett
 *
 */
@Entity
public class Timer
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	private String time;
	private boolean evaluated;
	private String command;
	private String data;
	
	public Timer()
	{
		
	}
	
	public Timer(String time, boolean evaluated, String command, String data)
	{
		this.time = time;
		this.evaluated = evaluated;
		this.command = command;
		this.data = data;
	}
	
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getTime()
	{
		return time;
	}
	public void setTime(String time)
	{
		this.time = time;
	}
	public boolean getEvaluated()
	{
		return evaluated;
	}
	public void setEvaluated(boolean evaluated)
	{
		this.evaluated = evaluated;
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
	
	public int getHour()
	{
		return Integer.parseInt(time.substring(0, time.indexOf(":")));
	}
	
	public int getMinute()
	{
		return Integer.parseInt(time.substring(time.indexOf(":") + 1, time.length()));
	}
}