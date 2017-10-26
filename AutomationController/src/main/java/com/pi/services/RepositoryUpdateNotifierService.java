package com.pi.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.infrastructure.RepositoryObserver;
import com.pi.model.ActionProfile;

@Service
public class RepositoryUpdateNotifierService
{
	@Autowired
	private PrimaryNodeControllerImpl primaryNodeControllerImpl;
	
	public void newActionProfile(ActionProfile profile)
	{
		Collection<ActionProfile> profiles = new HashSet<>();
		profiles.add(profile);
		
		newActionProfile(profiles);
	}
	
	public void newActionProfile(Collection<ActionProfile> profiles)
	{
		Collection<String> profileNames = profiles.stream().map(profile -> profile.getName()).collect(Collectors.toSet());
		
		primaryNodeControllerImpl.getDeviceSet() 
				.stream().filter(entry -> entry.getValue() instanceof RepositoryObserver)
				.map(entry -> (RepositoryObserver)entry.getValue())
				.forEach(observer -> 
				{
					try
					{
						observer.newActionProfile(profileNames);
					}
					catch (Exception e)
					{
					}
				});
	}
}
