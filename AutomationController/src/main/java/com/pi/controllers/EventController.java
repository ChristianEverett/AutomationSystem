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
import com.pi.infrastructure.util.ActionProfileDoesNotExist;
import com.pi.infrastructure.util.EventRegistry;
import com.pi.model.DeviceState;
import com.pi.model.EventHandler;
import com.pi.model.repository.ActionProfileJpaRepository;
import com.pi.services.EventProcessingService;
import com.pi.services.PrimaryNodeControllerImpl;


@Controller
public class EventController
{
	public static final String PATH = "/event";
	
	@Autowired
	private EventRegistry eventRegistry;
	
	@Autowired
	private PrimaryNodeControllerImpl nodeController;
	
	@Autowired
	private EventProcessingService eventProcessingService;
	
	public EventController()
	{
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<EventHandler> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return eventRegistry.getAllEvents();
	}
	
	@RequestMapping(value = PATH + "/group", method = RequestMethod.POST)
	public void createEvents(HttpServletRequest request, HttpServletResponse response, @RequestBody List<EventHandler> events)
	{
		eventRegistry.createEvents(events, false);
		
		events.stream().forEach(event -> eventProcessingService.checkIfTriggered(event, null));
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.POST)
	public @ResponseBody Integer createEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody EventHandler event)
	{
		int id = eventRegistry.createEvent(event, false);
		
		eventProcessingService.checkIfTriggered(event, null);
		return id;
	}
	
	@RequestMapping(value = (PATH + "/{hash}"), method = RequestMethod.DELETE)
	public void removeEvent(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash)
	{
		eventRegistry.removeEvent(hash);
	}
	
	/*
	 * Device specific requests 
	 */
	@RequestMapping(value = PATH + "/{hash}", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getAllRegisterListeners(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("id") Integer hash)
	{
		return eventRegistry.getAllListenersForEvent(hash);
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
			nodeController.update(state);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
}
