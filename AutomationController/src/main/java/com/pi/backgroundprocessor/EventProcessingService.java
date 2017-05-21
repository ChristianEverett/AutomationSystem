package com.pi.backgroundprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.pi.model.DeviceState;
import com.pi.model.Event;

public class EventProcessingService
{
	private static EventProcessingService singlton = null;
	private Processor processor = null;
	
	//Table to map IDs to events and to devices listening to the event
	private HashMap<Integer, Event> events = new HashMap<>();
	//Table to map devices to the events they trigger
	private HashMap<String, Set<Event>> mapDevicesToTriggerEvents = new HashMap<>();
	
	private EventProcessingService(Processor processor)
	{
		this.processor = processor;
		
		List<Event> events = processor.getPersistenceManger().readAllEvent();
		createEvents(events);
	}
	
	public static EventProcessingService startEventProcessingService(Processor processor) throws Exception
	{
		if(singlton != null)
			throw new Exception("EventProcessingService already created");
		singlton = new EventProcessingService(processor);
		return singlton;
	}
	
	synchronized void update(DeviceState state)
	{
		Set<Event> events = mapDevicesToTriggerEvents.get(state.getName());
		
		if (events != null)
		{
			for (Event event : events)
			{
				checkIfTriggered(event, state);
			} 
		}
	}

	private void checkIfTriggered(Event event, DeviceState state)
	{
		if (event.checkIfTriggered((String deviceName) -> (getStateFromCache(deviceName)), state))
		{
			applyTriggerStateToListnerDevices(event.getApplyStates());
		}
	}
	
	private DeviceState getStateFromCache(String deviceName)
	{
		return processor.getDeviceState(deviceName);
	}
	
	private void applyTriggerStateToListnerDevices(List<DeviceState> states)
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
	
	public synchronized List<Event> getAllEvents()
	{
		return new ArrayList<>(events.values());
	}
	
	public synchronized void createEvents(List<Event> events)
	{
		for(Event event : events)
			createEvent(event);
	}
	
	/**
	 * Add new event to the event table
	 * @param event
	 * @return id
	 */
	public synchronized Integer createEvent(Event event)
	{
		Integer id = event.hashCode();

		events.put(id, event);
		
		addToDeviceToTriggerEventMap(event);
		
		processor.getPersistenceManger().createEvent(event);
		
		checkIfTriggered(event, null);
	
		return id;
	}
	
	public synchronized Integer changeEvent(Integer id, Event event)
	{
		Event oldEvent = events.remove(id);
		
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
		Event event = events.remove(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);

		remoteFromDeviceToTriggerEventMap(event);
		
		processor.getPersistenceManger().deleteEvent(event);
	}
	
	/**
	 * Register a device to listen for event mapped at id
	 * @param id
	 * @param deviceName
	 */
	public synchronized void addListener(Integer id, DeviceState state)
	{
		if(events.get(id) == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		events.get(id).registerListener(state);
		checkIfTriggered(events.get(id), null);
	}
	
	/**
	 * Unregister a device from listening for an event
	 * @param id
	 * @param deviceName
	 */
	public synchronized void removeListener(Integer id, String deviceName)
	{
		if(events.get(id) == null)
			throw new RuntimeException("There is no device event mapped at: " + id);
		
		events.get(id).unRegisterListener(deviceName);	
	}
	
	public synchronized List<String> getAllListenersForEvent(Integer id)
	{
		Event event = events.get(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		return new ArrayList<>(event.getDependencyDevices());
	}
	
	private void addToDeviceToTriggerEventMap(Event event)
	{
		for(String deviceName : event.getDependencyDevices())
		{
			Set<Event> records = mapDevicesToTriggerEvents.get(deviceName);
			
			if(records == null)
			{
				records = new HashSet<>();
				mapDevicesToTriggerEvents.put(deviceName, records);
			}
			records.add(event);
		}
	}
	
	private void remoteFromDeviceToTriggerEventMap(Event event)
	{
		for(String deviceName : event.getDependencyDevices())
		{
			Set<Event> events = mapDevicesToTriggerEvents.get(deviceName);
			
			for(Iterator<Event> iter = events.iterator(); iter.hasNext();)
			{
				if(iter.next().equals(event))
				{
					iter.remove();
					break;
				}
			}
		}
	}
}
