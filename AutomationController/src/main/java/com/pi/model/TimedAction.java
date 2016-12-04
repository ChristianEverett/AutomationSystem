/**
 * 
 */
package com.pi.model;

import com.google.common.base.Objects;

/**
 * @author Christian Everett
 *
 */

public class TimedAction
{
	private String time;
	private boolean evaluated;
	private Action action;
	
	public TimedAction()
	{
		
	}
	
	public TimedAction(String time, boolean evaluated, Action action)
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

	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof TimedAction))
			return false;
		TimedAction timedAction = (TimedAction) object;
		
		return (time.equals(timedAction.time) && action.equals(timedAction.action));
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(action.hashCode(), time.hashCode());
	}
}
