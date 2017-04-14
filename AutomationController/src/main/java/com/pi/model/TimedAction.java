/**
 * 
 */
package com.pi.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pi.Application;

/**
 * @author Christian Everett
 *
 */

public class TimedAction extends DatabaseElement implements Comparable<TimedAction>
{
	private String time;
	private DeviceState state;

	public TimedAction()
	{

	}

	public TimedAction(String time, DeviceState state)
	{
		this.time = time;
		this.state = state;
	}

	public TimedAction(Integer id, String time, DeviceState state)
	{
		super(id);
		this.time = time;
		this.state = state;
	}

	public String getTime()
	{
		return time;
	}

	public void setTime(String time)
	{
		this.time = time;
	}

	public DeviceState getAction()
	{
		return state;
	}

	public void setAction(DeviceState state)
	{
		this.state = state;
	}

	public String get12Hour()
	{
		SimpleDateFormat format = new SimpleDateFormat("H:mm");
		try
		{
			return new SimpleDateFormat("K:mm").format(format.parse(time)).toString();
		}
		catch (ParseException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}

		return "";
	}

	@JsonIgnore
	public Integer getHour()
	{
		return Integer.parseInt(time.substring(0, time.indexOf(":")));
	}

	@JsonIgnore
	public Integer getMinute()
	{
		return Integer.parseInt(time.substring(time.indexOf(":") + 1, time.length()));
	}

	@JsonIgnore
	@Override
	public boolean equals(Object object)
	{
		if (!(object instanceof TimedAction))
			return false;
		TimedAction timedAction = (TimedAction) object;

		return (time.equals(timedAction.time) && state.equals(timedAction.state));
	}

	@Override
	@JsonGetter
	public int hashCode()
	{
		return Objects.hash(state.hashCode(), time.hashCode());
	}

	@JsonIgnore
	@Override
	public int compareTo(TimedAction other)
	{
		int result = getHour().compareTo(other.getHour());

		if (result == 0)
			result = getMinute().compareTo(other.getMinute());

		return result;
	}
}
