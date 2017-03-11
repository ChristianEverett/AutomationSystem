package com.pi.model;

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
	public void setDatabaseID(Integer databaseID)
	{
		this.databaseID = databaseID;
	}
	
	protected String toMySqlString(String string)
	{
		return ("'" + string + "'");
	}
	
	@JsonIgnore
	public Object getDatabaseIdentification()
	{
		return getDatabaseID();
	}

	@JsonIgnore
	public String getDatabaseIdentificationForQuery()
	{
		return getDatabaseID().toString();
	}
}
