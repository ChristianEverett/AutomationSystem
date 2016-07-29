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

public class Timer
{
	private String time;
	private boolean evaluated;
	private Action action;
	
	public Timer()
	{
		
	}
	
	public Timer(String time, boolean evaluated, Action action)
	{
		this.time = time;
		this.evaluated = evaluated;
		this.action = action;
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
	public Action getAction()
	{
		return action;
	}
	public void setAction(Action action)
	{
		this.action = action;
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
