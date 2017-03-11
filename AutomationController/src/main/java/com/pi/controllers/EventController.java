package com.pi.controllers;

import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.Application;
import com.pi.backgroundprocessor.EventProcessingService;
import com.pi.backgroundprocessor.Processor;
import com.pi.model.DeviceState;
import com.pi.model.Event;

@Controller
public class EventController
{
	public static final String PATH = "/event";
	private EventProcessingService eventProcessingService;
	
	public EventController()
	{
		eventProcessingService = Processor.getBackgroundProcessor().geEventProcessingService();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Event> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return eventProcessingService.getAllEvents();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.POST)
	public @ResponseBody Integer createEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody Event event)
	{
		return eventProcessingService.createEvent(event);
	}
	
	/*
	 * Device specific requests 
	 */
	@RequestMapping(value = PATH + "/{hash}", method = RequestMethod.GET)
	public @ResponseBody Collection<String> getAllRegisterListeners(HttpServletRequest request, HttpServletResponse response, 
			@PathVariable("id") Integer hash)
	{
		return eventProcessingService.getAllListenersForEvent(hash);
	}
	
	@RequestMapping(value = (PATH + "/{hash}"), method = RequestMethod.POST)
	public void registerListener(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash,
			@RequestBody DeviceState state)
	{
		eventProcessingService.mapEvent(hash, state);
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
			eventProcessingService.update(state);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
	}
}
