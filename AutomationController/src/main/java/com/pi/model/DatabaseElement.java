package com.pi.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DatabaseElement implements Serializable
{
	public DatabaseElement()
	{
		
	}
	
	protected String toMySqlString(String string)
	{
		return ("'" + string + "'");
	}
	
	@JsonIgnore
	public int getDatabaseIdentification()
	{
		return hashCode();
	}
	
	public String getName()
	{
		return null;
	}
}
