package com.pi.backgroundprocessor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.pi.model.DeviceState;
import com.pi.model.Event;

public class EventProcessingService
{
	private static EventProcessingService singlton = null;
	private Processor processor = null;
	
	//Table to map IDs to events and to devices listening to the event
	private HashMap<Integer, EventRecord> eventRecords = new HashMap<>();
	//Table to map devices to the events they trigger
	private HashMap<String, List<EventRecord>> mapDevicesToTriggerEvents = new HashMap<>();
	
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
	
	public synchronized void update(String deviceName, DeviceState state)
	{
		List<EventRecord> eventRecords = mapDevicesToTriggerEvents.get(deviceName);
		
		for(EventRecord eventRecord : eventRecords)
		{
			Event event = eventRecord.getEvent();
			if(event.updateAndCheckIfTriggered(state))
			{
				for(Entry<String, DeviceState> element : eventRecord.getRegisteredDevices())
				{
					processor.scheduleAction(element.getValue());
				}
			}
		}
	}
	
	/**
	 * Register a device to listen for event mapped at id
	 * @param id
	 * @param deviceName
	 */
	public synchronized void mapEvent(Integer id, String deviceName, DeviceState state)
	{
		if(eventRecords.get(id) == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		List<Entry<String, DeviceState>> pairs = eventRecords.get(id).getRegisteredDevices();
		
		pairs.add(new AbstractMap.SimpleEntry<String, DeviceState>(deviceName, state));
	}
	
	/**
	 * Unregister a device from listening for an event
	 * @param id
	 * @param deviceName
	 */
	public synchronized void unmapEvent(Integer id, String deviceName)
	{
		if(eventRecords.get(id) == null)
			throw new RuntimeException("There is no device event mapped at: " + id);
		
		List<Entry<String, DeviceState>> pairs = eventRecords.get(id).getRegisteredDevices();
		
		for(Iterator<Entry<String, DeviceState>> iter = pairs.iterator(); iter.hasNext();)
		{
			if(iter.next().getKey().equals(deviceName))
			{
				iter.remove();	
			}
		}
	}
	
	/**
	 * Add new event to the event table
	 * @param event
	 * @return id
	 */
	public synchronized Integer createEvent(Event event)
	{
		Integer id = event.hashCode();
		
		EventRecord eventRecord = new EventRecord(event);
		eventRecords.put(id, eventRecord);
		
		for(String deviceName : event.getDependencyDevices())
		{
			List<EventRecord> records = mapDevicesToTriggerEvents.get(deviceName);
			
			if(records == null)
				records = new LinkedList<>();
			records.add(eventRecord);
		}
		
		return id;
	}
	
	public synchronized void removeEvent(Integer id)
	{
		EventRecord eventRecord = eventRecords.remove(id);
		
		if(eventRecord == null)
			throw new RuntimeException("There is no event mapped at: " + id);
		
		for(String deviceName : eventRecord.getEvent().getDependencyDevices())
		{
			List<EventRecord> eventRecords = mapDevicesToTriggerEvents.get(deviceName);
			
			for(Iterator<EventRecord> iter = eventRecords.iterator(); iter.hasNext();)
			{
				if(iter.next().getEvent().equals(eventRecord))
				{
					iter.remove();
					break;
				}
			}
		}
	}
	
	private static class EventRecord
	{
		private Event event = null;
		private List<Entry<String, DeviceState>> registeredDevices = new LinkedList<>();
		
		public EventRecord(Event event)
		{
			this.event = event;
		}

		public Event getEvent()
		{
			return event;
		}

		public List<Entry<String, DeviceState>> getRegisteredDevices()
		{
			return registeredDevices;
		}
	}
}
