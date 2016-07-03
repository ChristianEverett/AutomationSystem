/**
 * 
 */
package com.pi.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Christian Everett
 *
 */
@Service
public class RepositoryContainer
{
	private static RepositoryContainer repositoryContainer;
	
	@Autowired
	private TimerRepository timerRepository;
	@Autowired
	private StateRepository stateRepository;
	
	public RepositoryContainer()
	{
		repositoryContainer = this;
	}
	
	public static RepositoryContainer getRepositorycontainer()
	{
		return repositoryContainer;
	}
	
	public StateRepository buildStateRepository()
	{
		return stateRepository;
	}
	
	public TimerRepository buildTimerRepository()
	{
		return timerRepository;
	}
}
