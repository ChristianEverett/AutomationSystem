package com.pi.controllers;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.Application;
import com.pi.backgroundprocessor.Processor;
import com.pi.model.DeviceState;
import com.pi.model.Event;

@Controller
public class EventController
{
	public static final String PATH = "/event";
	private Processor processor;
	
	public EventController()
	{
		processor = Processor.getInstance();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Event> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return processor.getEventProcessingService().getAllEvents();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.POST)
	public @ResponseBody Integer createEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody Event event)
	{
		return processor.getEventProcessingService().createEvent(event);
	}
	
	@RequestMapping(value = (PATH + "/update/{hash}"), method = RequestMethod.POST)
	public @ResponseBody Integer changeEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody Event event
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
	
	@RequestMapping(value = (PATH + "/{hash}"), method = RequestMethod.POST)
	public void registerListener(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash,
			@RequestBody DeviceState state)
	{
		processor.getEventProcessingService().addListener(hash, state);
	}
	
	@RequestMapping(value = (PATH + "/group/{hash}"), method = RequestMethod.POST)
	public void registerListeners(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash,
			@RequestBody Collection<DeviceState> states)
	{
		for(DeviceState state : states)
			processor.getEventProcessingService().addListener(hash, state);
	}
	
	@RequestMapping(value = (PATH + "/{hash}/{deviceName}"), method = RequestMethod.DELETE)
	public void unRegisterListener(HttpServletRequest request, HttpServletResponse response, @PathVariable("hash") Integer hash,
			@PathVariable("deviceName") String deviceName)
	{
		processor.getEventProcessingService().removeListener(hash, deviceName);
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
			Application.LOGGER.severe(e.getMessage());
		}
	}
}
