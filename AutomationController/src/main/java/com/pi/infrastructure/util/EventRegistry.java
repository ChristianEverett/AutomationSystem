package com.pi.infrastructure.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.SystemLogger;
import com.pi.model.EventHandler;
import com.pi.model.repository.ActionProfileJpaRepository;
import com.pi.model.repository.EventJpaRepository;

@Service
public class EventRegistry
{
	@Autowired
	private EventJpaRepository eventJpaRepository;
	
	@Autowired
	private ActionProfileJpaRepository actionProfileRepository;
	
	// Table to map IDs to events and to devices listening to the event
	private HashMap<Integer, EventHandler> events = new HashMap<>();
	// Table to map devices to the events they trigger
	private HashMap<String, Set<EventHandler>> mapDevicesToTriggerEvents = new HashMap<>();

	@PostConstruct
	private void load()
	{
		SystemLogger.getLogger().info("Loading Events");
		List<EventHandler> events = eventJpaRepository.findAll();
		createEvents(events, true);	
	}
	
	public synchronized List<EventHandler> getAllEvents()
	{
		return new ArrayList<>(events.values());
	}

	public synchronized Set<EventHandler> getAllEventsThatAreTriggerByDevice(String deviceName)
	{
		return mapDevicesToTriggerEvents.get(deviceName);
	}
	
	public synchronized void createEvents(List<EventHandler> events, boolean loadingFromDatabase)
	{
		for (EventHandler event : events)
			createEvent(event, loadingFromDatabase);
	}

	/**
	 * Add new event to the event table
	 * 
	 * @param event
	 * @return id
	 */
	public synchronized Integer createEvent(EventHandler event, boolean loadingFromDatabase)
	{
		if(!actionProfileRepository.exists(event.getActionProfileName()))
			throw new ActionProfileDoesNotExist(event.getActionProfileName());
		
		Integer id = event.hashCode();

		if (events.containsKey(id))
		{
			eventJpaRepository.delete(events.get(id));
		}

		events.put(id, event);

		addToDeviceToTriggerEventMap(event);

		if (!loadingFromDatabase)
		{
			eventJpaRepository.save(event);
		}
		return id;
	}

	public synchronized void removeEvent(Integer id)
	{
		EventHandler event = events.remove(id);

		if (event == null)
			throw new RuntimeException("There is no event mapped at: " + id);

		remoteFromDeviceToTriggerEventMap(event);

		eventJpaRepository.delete(event);
	}

	public synchronized List<String> getAllListenersForEvent(Integer id)
	{
		EventHandler event = events.get(id);

		if (event == null)
			throw new RuntimeException("There is no event mapped at: " + id);

		return new ArrayList<>(event.getDependencyDevices());
	}

	public synchronized void removeEventsWithActionProfile(String actionProfileName)
	{
		List<EventHandler> eventsToRemove = events.values().stream().filter(eventHandler -> eventHandler.getActionProfileName().equals(actionProfileName)).collect(Collectors.toList());

		for (EventHandler eventHandler : eventsToRemove)
			removeEvent(eventHandler.hashCode());
	}

	private void addToDeviceToTriggerEventMap(EventHandler event)
	{
		for (String deviceName : event.getDependencyDevices())
		{
			Set<EventHandler> records = mapDevicesToTriggerEvents.get(deviceName);

			if (records == null)
			{
				records = new HashSet<>();
				mapDevicesToTriggerEvents.put(deviceName, records);
			}
			records.add(event);
		}
	}

	private void remoteFromDeviceToTriggerEventMap(EventHandler event)
	{
		for (String deviceName : event.getDependencyDevices())
		{
			Set<EventHandler> events = mapDevicesToTriggerEvents.get(deviceName);

			for (Iterator<EventHandler> iter = events.iterator(); iter.hasNext();)
			{
				if (iter.next().equals(event))
				{
					iter.remove();
				}
			}
		}
	}
}
