package com.pi.controllers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.model.Event;

public class EventController
{
	private static final String PATH = "/event";
	
	public EventController()
	{
		
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Event> getEvents(HttpServletRequest request, HttpServletResponse response)
	{
		return null;
		
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.POST)
	public void createEvent(HttpServletRequest request, HttpServletResponse response, @RequestBody Event state)
	{
		
	}
	
	/*
	 * Device specific requests 
	 */
	@RequestMapping(value = PATH + "/{device}", method = RequestMethod.GET)
	public @ResponseBody Collection<Event> getLinkedEvents(HttpServletRequest request, HttpServletResponse response, @PathVariable("device") String deviceName)
	{
		return null;
		
	}
	
	@RequestMapping(value = (PATH + "/{device}"), method = RequestMethod.POST)
	public void scheduleEvent(HttpServletRequest request, HttpServletResponse response, @PathVariable("device") String deviceName)
	{
		
	}
}
