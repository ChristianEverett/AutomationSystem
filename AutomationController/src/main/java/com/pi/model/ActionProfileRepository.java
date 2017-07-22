package com.pi.model;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class ActionProfileRepository extends ConcurrentHashMap<String, ActionProfile>
{
	public ActionProfileRepository()
	{
		super();
	}
	
	public void add(ActionProfile profile)
	{
		put(profile.getName(), profile);
	}
	
	public Collection<ActionProfile> getAll()
	{
		return values();
	}
}
