/**
 * 
 */
package com.pi.controllers;

import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.SystemLogger;
import com.pi.backgroundprocessor.Processor;
import com.pi.infrastructure.Device;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;

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
		bgp = Processor.getInstance();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceState> getAllStates(HttpServletResponse response)
	{
		try
		{
			return bgp.getStates(false);
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
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
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	@RequestMapping(value = (PATH + "/records"), method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceStateRecord> getRecordsFor(HttpServletResponse response, 
			@RequestParam("start") String start, @RequestParam("end") String end)
	{
		try
		{
			List<DeviceStateRecord> list = bgp.getPersistenceManger().getRecords(start, end);
			Collections.sort(list);
			return list;
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
			response.setStatus(503);
		}
		
		return null;
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
			SystemLogger.getLogger().severe(e.getMessage());
		}
		
		return null;
	}
}
