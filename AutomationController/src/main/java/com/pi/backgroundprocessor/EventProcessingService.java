package com.pi.backgroundprocessor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.pi.Application;
import com.pi.model.DeviceState;
import com.pi.model.Event;

public class EventProcessingService
{
	private static EventProcessingService singlton = null;
	private Processor processor = null;
	
	//Table to map IDs to events and to devices listening to the event
	private HashMap<Integer, Event> events = new HashMap<>();
	//Table to map devices to the events they trigger
	private HashMap<String, List<Event>> mapDevicesToTriggerEvents = new HashMap<>();
	
	private EventProcessingService(Processor processor)
	{
		this.processor = processor;
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
		List<Event> events = mapDevicesToTriggerEvents.get(state.getName());
		
		if (events != null)
		{
			for (Event event : events)
			{
				checkIfTriggered(event);
			} 
		}
	}

	private void checkIfTriggered(Event event)
	{
		if (event.checkIfTriggered(processor))
		{
			applyTriggerStateToListnerDevices(event.getTriggerStates());
		}
//		else 
//		{
//			applyTriggerStateToListnerDevices(event.getInvertedTriggerStates());
//		}
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
		checkIfTriggered(events.get(id));
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
	
	/**
	 * Add new event to the event table
	 * @param event
	 * @return id
	 */
	public synchronized Integer createEvent(Event event)
	{
		Integer id = event.hashCode();

		events.put(id, event);
		
		for(String deviceName : event.getDependencyDevices())
		{
			List<Event> records = mapDevicesToTriggerEvents.get(deviceName);
			
			if(records == null)
			{
				records = new LinkedList<>();
				mapDevicesToTriggerEvents.put(deviceName, records);
			}
			records.add(event);
		}
		
		return id;
	}
	
	public synchronized Integer changeEvent(Integer id, Event event)
	{
		Event oldEvent = events.remove(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		oldEvent.replace(event);
		events.put(event.hashCode(), event);
		
		return oldEvent.hashCode();
	}
	
	public synchronized void removeEvent(Integer id)
	{
		Event event = events.remove(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		try
		{
			processor.getPersistenceManger().deleteEvent(event);
		}
		catch (IOException | SQLException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
		
		for(String deviceName : event.getDependencyDevices())
		{
			List<Event> events = mapDevicesToTriggerEvents.get(deviceName);
			
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
	
	public synchronized void createEvents(List<Event> events)
	{
		for(Event event : events)
			createEvent(event);
	}
	
	public synchronized List<Event> getAllEvents()
	{
		return new ArrayList<>(events.values());
	}
	
	public synchronized List<String> getAllListenersForEvent(Integer id)
	{
		Event event = events.get(id);
		
		if(event == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		return new ArrayList<>(event.getDependencyDevices());
	}
}
