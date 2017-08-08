package com.pi.model.repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.pi.infrastructure.util.ActionProfileDoesNotExist;
import com.pi.model.ActionProfile;

@Repository(RepositoryType.ActionProfile)
public class ActionProfileRepository extends BaseRepository<String, ActionProfile>
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

	@Override
	public ActionProfile get(Object key)
	{
		ActionProfile profile = super.get(key);
		
		if(profile == null)
			throw new ActionProfileDoesNotExist((String) key);
		
		return profile;
	}

	@Override
	public ActionProfile remove(Object key)
	{
		ActionProfile profile = remove(key);
		
		if(profile == null)
			throw new ActionProfileDoesNotExist((String) key);
		
		return profile;
	}
	
	
}
