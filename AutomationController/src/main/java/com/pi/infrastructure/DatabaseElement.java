package com.pi.infrastructure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DatabaseElement implements Serializable
{
	private Integer databaseID = new Integer(-1);
	
	public DatabaseElement()
	{
		
	}
	
	public DatabaseElement(Integer id)
	{
		databaseID = id;
	}
	
	public Integer getDatabaseID()
	{
		return databaseID;
	}
	
	@JsonIgnore
	public boolean isInDatabase()
	{
		return databaseID > 0;
	}
}
