package com.pi.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.google.common.base.Objects;

/**
 * @author Christian Everett
 */
public class TimerRepository
{
	private static TimerRepository singlton = null;
	private final static HashMap<Long, Timer> timerMap = new HashMap<>();
	
	private TimerRepository()
	{
		
	}
	
	public static TimerRepository getInstance()
	{
		if(singlton == null)
			singlton = new TimerRepository();
		
		return singlton;
	}
	
	/**
	 * @param timer
	 * @return id of added timer
	 */
	public Long add(Timer timer)
	{
		Long id = (long) Objects.hashCode(timer.getTime(), timer.getAction());
		
		timerMap.put(id, timer);
		
		return id;
	}
	
	public void update(Long id, Timer timer)
	{
		Timer oldTimer = timerMap.get(id);
		
		if(oldTimer == null)
		{
			return;
		}
		
		timerMap.put(id, timer);
	}
	
	/**
	 * @param id
	 * @return stored timer, or null if not found
	 */
	public Timer get(Long id)
	{
		return timerMap.get(id);
	}
	
	public Set<Long> getAllKeys()
	{
		return timerMap.keySet();
	}
	
	public Set<Entry<Long, Timer>> getAllElement()
	{
		return timerMap.entrySet();
	}
	
	public Timer delete(Long id)
	{
		return timerMap.remove(id);
	}
	
	public void deleteAll()
	{
		timerMap.clear();
	}
}
