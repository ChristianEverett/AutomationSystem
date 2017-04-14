package com.pi.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeviceStateRecord extends DatabaseElement implements Comparable<DeviceStateRecord>
{
	private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy a hh:mm:ss");
	private DeviceState state = null;
	private Date date = null;
	
	public DeviceStateRecord(DeviceState state)
	{
		this.date = new Date();
		this.state = state;
	}

	public DeviceState getState()
	{
		return state;
	}

	public String getDateString()
	{
		return sdf.format(date);
	}
	
	@JsonIgnore
	public Date getDate()
	{
		return date;
	}

	@Override
	public int compareTo(DeviceStateRecord arg)
	{
		return date.compareTo(arg.date);
	}
}
