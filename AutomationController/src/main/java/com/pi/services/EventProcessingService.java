package com.pi.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.SystemLogger;
import com.pi.infrastructure.EventRegistry;
import com.pi.infrastructure.util.ActionProfileDoesNotExist;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;

@Service
public class EventProcessingService
{
	@Autowired
	private PrimaryNodeControllerImpl nodeControllerImpl;
		
	@Autowired
	private EventRegistry eventRegistry;
	
	private Set<Integer> activeSet = ConcurrentHashMap.newKeySet();
	
	private EventProcessingService()
	{	
	}
	
	void update(DeviceState state)
	{
		Set<EventHandler> events = eventRegistry.getAllEventsThatAreTriggerByDevice(state.getName());
		
		if (events != null)
		{
			for (EventHandler event : events)
			{
				checkIfTriggered(event, state);
			} 
		}
	}

	public void checkIfTriggered(EventHandler event, DeviceState stateThatChanged)
	{
		synchronized (event)
		{
			// Get Cache state for each device this event is listening too
			Map<String, DeviceState> currentStates = event.getTriggerStates().stream()
					.map((range) -> nodeControllerImpl.getDeviceState(range.getName()))
					.collect(Collectors.toMap(DeviceState::getName, cacheState -> cacheState));
			
			boolean eventTriggered = event.checkIfTriggered(currentStates, stateThatChanged);
		
			if (!activeSet.contains(event.getId()) && eventTriggered)
			{
				try
				{		
					nodeControllerImpl.trigger(event.getActionProfileName());
					activeSet.add(event.getId());
					SystemLogger.getLogger().info("Event: " + event.getId() + " Active. Triggered: " + event.getActionProfileName());
				}
				catch (ActionProfileDoesNotExist e)
				{
					SystemLogger.getLogger().severe("Removing event can't find action profile: " + event.getActionProfileName());
					eventRegistry.removeEvent(event.getId());
					
				}
			}
			else if(!eventTriggered)
			{
				if(activeSet.remove(event.getId()) && event.getUnTrigger())
				{
					nodeControllerImpl.unTrigger(event.getActionProfileName());
					SystemLogger.getLogger().info("Event: " + event.getId() + " Inactive. unTriggered: " + event.getActionProfileName());
				}
			}
		}	
	}
	
	public void checkAllIfTriggered(List<EventHandler> events)
	{
		for(EventHandler event : events)
		{
			if(activeSet.contains(event.getId()))
				activeSet.remove(event.getId());
			checkIfTriggered(event, null);
		}
	}
	
	public boolean isActive(EventHandler event)
	{
		return activeSet.contains(event.getId());
	}
	
	public boolean clearActiveStatus(int id)
	{
		return activeSet.remove(id);
	}
}
