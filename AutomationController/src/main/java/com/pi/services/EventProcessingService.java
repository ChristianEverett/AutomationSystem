package com.pi.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.model.ActionProfile;
import com.pi.model.ActionProfileRepository;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;

@Service
public class EventProcessingService
{
	@Autowired
	private Processor processor;
	
	@Autowired
	private ActionProfileRepository actionProfileRepository;
	
	//Table to map IDs to events and to devices listening to the event
	private HashMap<Integer, EventHandler> events = new HashMap<>();
	//Table to map devices to the events they trigger
	private HashMap<String, Set<EventHandler>> mapDevicesToTriggerEvents = new HashMap<>();
	
	private EventProcessingService()
	{	

	}
	
	synchronized void update(DeviceState state)
	{
		Set<EventHandler> events = mapDevicesToTriggerEvents.get(state.getName());
		
		if (events != null)
		{
			for (EventHandler event : events)
			{
				checkIfTriggered(event, state);
			} 
		}
	}

	private void checkIfTriggered(EventHandler event, DeviceState state)
	{
		if (event.checkIfTriggered((String deviceName) -> (getStateFromCache(deviceName)), state))
		{
			ActionProfile profile = actionProfileRepository.get(event.getActionProfileName());
			applyTriggerStateToListnerDevices(profile.getDeviceStates());
		}
	}
	
	private DeviceState getStateFromCache(String deviceName)
	{
		return processor.getDeviceState(deviceName);
	}
	
	private void applyTriggerStateToListnerDevices(Set<DeviceState> states)
	{
		if (states != null)
		{
			for (DeviceState element : states)
			{
				if (!element.equals(processor.getDeviceState(element.getName())))
				{
					processor.scheduleAction(element);
				}
			} 
		}
	}
	
	public synchronized List<EventHandler> getAllEvents()
	{
		return new ArrayList<>(events.values());
	}
	
	public synchronized void createEvents(List<EventHandler> events)
	{
		for(EventHandler event : events)
			createEvent(event);
	}
	
	/**
	 * Add new event to the event table
	 * @param event
	 * @return id
	 */
	public synchronized Integer createEvent(EventHandler event)
	{
		Integer id = event.hashCode();

		events.put(id, event);
		
		addToDeviceToTriggerEventMap(event);
		
		processor.getPersistenceManger().createEvent(event);
		
		checkIfTriggered(event, null);
	
		return id;
	}
	
	public synchronized Integer changeEvent(Integer id, EventHandler event)
	{
		EventHandler oldEvent = events.remove(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		int oldID = oldEvent.getDatabaseIdentification();
		remoteFromDeviceToTriggerEventMap(oldEvent);
		oldEvent.replace(event);
		
		events.put(event.hashCode(), event);
		addToDeviceToTriggerEventMap(oldEvent);
		
		processor.getPersistenceManger().updateEvent(oldEvent, oldID);
		
		return oldEvent.hashCode();
	}
	
	public synchronized void removeEvent(Integer id)
	{
		EventHandler event = events.remove(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);

		remoteFromDeviceToTriggerEventMap(event);
		
		processor.getPersistenceManger().deleteEvent(event);
	}
	
	public synchronized List<String> getAllListenersForEvent(Integer id)
	{
		EventHandler event = events.get(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		return new ArrayList<>(event.getDependencyDevices());
	}
	
	private void addToDeviceToTriggerEventMap(EventHandler event)
	{
		for(String deviceName : event.getDependencyDevices())
		{
			Set<EventHandler> records = mapDevicesToTriggerEvents.get(deviceName);
			
			if(records == null)
			{
				records = new HashSet<>();
				mapDevicesToTriggerEvents.put(deviceName, records);
			}
			records.add(event);
		}
	}
	
	private void remoteFromDeviceToTriggerEventMap(EventHandler event)
	{
		for(String deviceName : event.getDependencyDevices())
		{
			Set<EventHandler> events = mapDevicesToTriggerEvents.get(deviceName);
			
			for(Iterator<EventHandler> iter = events.iterator(); iter.hasNext();)
			{
				if(iter.next().equals(event))
				{
					iter.remove();
				}
			}
		}
	}
}
