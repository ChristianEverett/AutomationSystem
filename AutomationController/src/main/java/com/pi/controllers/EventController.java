package com.pi.controllers;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.SystemLogger;
import com.pi.infrastructure.util.ActionProfileDoesNotExist;
import com.pi.model.ActionProfileRepository;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;
import com.pi.services.Processor;

@Controller
public class EventController
{
	public static final String PATH = "/event";
	
	@Autowired
	private Processor processor;
	
	@Autowired
	private ActionProfileRepository actionProfileRepository;
	
	public EventController()
	{
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<EventHandler> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return processor.getEventProcessingService().getAllEvents();
	}
	
	@RequestMapping(value = PATH + "/group", method = RequestMethod.POST)
	public void createEvents(HttpServletRequest request, HttpServletResponse response, @RequestBody Collection<EventHandler> events)
	{
		for (EventHandler event : events)
		{
			try
			{
				verifiyActionProfileExists(event.getActionProfileName());
				processor.getEventProcessingService().createEvent(event);
			}
			catch (ActionProfileDoesNotExist e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.POST)
	public @ResponseBody Integer createEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody EventHandler event)
	{
		verifiyActionProfileExists(event.getActionProfileName());
		return processor.getEventProcessingService().createEvent(event);
	}
	
	@RequestMapping(value = (PATH + "/update/{hash}"), method = RequestMethod.POST)
	public @ResponseBody Integer changeEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody EventHandler event
			,@PathVariable("hash") Integer hash)
	{
		return processor.getEventProcessingService().changeEvent(hash, event);
	}
	
	@RequestMapping(value = (PATH + "/{hash}"), method = RequestMethod.DELETE)
	public void removeEvent(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash)
	{
		processor.getEventProcessingService().removeEvent(hash);
	}
	
	/*
	 * Device specific requests 
	 */
	@RequestMapping(value = PATH + "/{hash}", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getAllRegisterListeners(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("id") Integer hash)
	{
		return processor.getEventProcessingService().getAllListenersForEvent(hash);
	}
	
	/*
	 * Only used by automation clients
	 */
	@RequestMapping(value = (PATH + "/AC/update"), method = RequestMethod.POST)
	public void updateState(HttpServletRequest request, HttpServletResponse response, @RequestBody DeviceState state)
	{	
		try//(ObjectInputStream input = new ObjectInputStream(request.getInputStream()))
		{
			//DeviceState state = (DeviceState) input.readObject();
			processor.update(state);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	private void verifiyActionProfileExists(String profileName)
	{
		if(actionProfileRepository.get(profileName) == null)
			throw new ActionProfileDoesNotExist("No Profile by name of: " + profileName);
	}
}
