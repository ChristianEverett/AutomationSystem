/**
 * 
 */
package com.pi.controllers;

import java.io.ObjectOutputStream;
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
import com.pi.infrastructure.Device;
import com.pi.model.DeviceState;

/**
 * @author Christian Everett
 *
 */

@Controller
public class ActionController
{
	public static final String PATH = "/action";
	
	private static Processor bgp = null;
	
	public ActionController()
	{
		bgp = Processor.getBackgroundProcessor();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceState> getAllStates(HttpServletResponse response)
	{
		try
		{
			return Device.getStates(false);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
		
		return null;
	}
	
	@RequestMapping(value = (PATH + "/{name}"), method = RequestMethod.GET)
	public @ResponseBody DeviceState getState(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String deviceName)
	{
		return getState(response, deviceName);
	}
	
	@RequestMapping(value = (PATH + "/AC/{name}"), method = RequestMethod.GET)
	public void getStateForAutomationClient(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String deviceName)
	{
		try(ObjectOutputStream output = new ObjectOutputStream(response.getOutputStream()))
		{
			output.writeObject(getState(response, deviceName));
			output.flush();
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	@RequestMapping(value = (PATH + "/{device}"), method = RequestMethod.POST)
	public void scheduleAction(HttpServletRequest request, HttpServletResponse response, @RequestBody DeviceState state)
	{
		bgp.scheduleAction(state);
	}
	
	private DeviceState getState(HttpServletResponse response, String deviceName)
	{
		try
		{
			DeviceState state = Device.getDeviceState(deviceName);
			
			if(state == null)
				response.setStatus(404);
			
			return state;
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe(e.getMessage());
		}
		
		return null;
	}
}
