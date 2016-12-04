/**
 * 
 */
package com.pi.controllers;

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
import com.pi.backgroundprocessor.Processor;
import com.pi.model.Action;

/**
 * @author Christian Everett
 *
 */

@Controller
public class ActionController
{
	private static final String PATH = "/action";
	
	private static Processor PROCESSOR = Processor.getBackgroundProcessor();
	
	public ActionController()
	{
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Action> getAllStates(HttpServletResponse response)
	{
		try
		{
			return PROCESSOR.getStates();
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
		
		return null;
	}
	
	@RequestMapping(value = (PATH + "/{name}"), method = RequestMethod.GET)
	public @ResponseBody Action getState(HttpServletResponse response, @PathVariable("name") String deviceName)
	{
		try
		{
			Action action = PROCESSOR.getStateByDeviceName(deviceName);
			
			if(action == null)
				response.setStatus(404);
			
			return action;
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
		
		return null;
	}
	
	@RequestMapping(value = (PATH + "/add"), method = RequestMethod.POST)
	public void addAction(HttpServletRequest request, HttpServletResponse response, @RequestBody Action action)
	{
		PROCESSOR.scheduleAction(action);
	}
}
