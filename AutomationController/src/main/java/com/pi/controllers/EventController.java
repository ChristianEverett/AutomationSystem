package com.pi.controllers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
import com.pi.infrastructure.EventRegistry;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;
import com.pi.services.EventProcessingService;
import com.pi.services.PrimaryNodeControllerImpl;


@Controller
@RequestMapping(value = "/event")
public class EventController
{
	@Autowired
	private EventRegistry eventRegistry;
	
	@Autowired
	private PrimaryNodeControllerImpl nodeController;
	
	@Autowired
	private EventProcessingService eventProcessingService;
	
	public EventController()
	{
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Collection<EventHandler> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return eventRegistry.getAllEvents();
	}
	
	@RequestMapping(value = "/active", method = RequestMethod.GET)
	public @ResponseBody Collection<EventHandler> getActiveEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return eventRegistry.getAllEvents().stream().filter(eventProcessingService::isActive).collect(Collectors.toList());
	}
	
	@RequestMapping(value = "/group", method = RequestMethod.POST)
	public void createEvents(HttpServletRequest request, HttpServletResponse response, @RequestBody List<EventHandler> events)
	{
		eventRegistry.addEvents(events, false);
		
		eventProcessingService.checkAllIfTriggered(events);
	}
	
	@RequestMapping(value = "/{hash}", method = RequestMethod.DELETE)
	public void removeEvent(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer id)
	{
		eventRegistry.removeEvent(id);
		eventProcessingService.clearActiveStatus(id);
	}
	
	@RequestMapping(method = RequestMethod.DELETE)
	public void removeEvents(HttpServletRequest request, HttpServletResponse response)
	{
		eventRegistry.removeAllEvents();
	}
	
	/*
	 * Device specific requests 
	 */
	@RequestMapping(value = "/{hash}", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getAllRegisterListeners(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("id") Integer hash)
	{
		return eventRegistry.getAllListenersForEvent(hash);
	}
	
	/*
	 * Only used by automation clients
	 */
	@RequestMapping(value = "/AC/update", method = RequestMethod.POST)
	public void updateState(HttpServletRequest request, HttpServletResponse response, @RequestBody DeviceState state)
	{	
		try//(ObjectInputStream input = new ObjectInputStream(request.getInputStream()))
		{
			//DeviceState state = (DeviceState) input.readObject();
			nodeController.update(state);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
}
