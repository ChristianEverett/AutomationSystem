package com.pi.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "DeviceStateRecord")
public class DeviceStateRecord extends Model implements Comparable<DeviceStateRecord>
{
	@Transient
	public static final DateTimeFormatter  sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy a hh:mm:ss");
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	@Column(length = 3000) 
	private DeviceState state = null;
	private LocalDateTime date;
	
	public DeviceStateRecord(DeviceState state)
	{
		this.state = state;
		date = LocalDateTime.now();
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
	public LocalDateTime getDate()
	{
		return date;
	}

	@Override
	public int compareTo(DeviceStateRecord arg)
	{
		return date.compareTo(arg.date);
	}
}
