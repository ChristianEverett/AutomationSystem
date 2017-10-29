package com.pi.services;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.SystemLogger;
import com.pi.infrastructure.util.ActionProfileDoesNotExist;
import com.pi.infrastructure.util.EventRegistry;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;

@Service
public class EventProcessingService
{
	@Autowired
	private PrimaryNodeControllerImpl processor;
		
	@Autowired
	private EventRegistry eventRegistry;
	
	private Set<EventHandler> activeSet = new HashSet<>();
	
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

	public void checkIfTriggered(EventHandler event, DeviceState state)//TODO finish
	{
		Map<String, DeviceState> cachedStates = event.getTriggerStates().stream()
				.map((range) -> processor.getDeviceState(range.getName()))
				.collect(Collectors.toMap(DeviceState::getName, cacheState -> cacheState));
		
		if (event.checkIfTriggered(cachedStates, state) /*&& !activeSet.contains(event)*/)
		{
			try
			{		
				processor.trigger(event.getActionProfileName());
				//activeSet.add(event);
			}
			catch (ActionProfileDoesNotExist e)
			{
				SystemLogger.getLogger().severe("Removing event can't find action profile: " + event.getActionProfileName());
				eventRegistry.removeEvent(event.hashCode());
			}
		}
		else
		{
			//activeSet.remove(event); add untrigger
		}
	}
}
