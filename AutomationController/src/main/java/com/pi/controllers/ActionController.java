/**
 * 
 */
package com.pi.controllers;

import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.util.DeviceLockedException;
import com.pi.model.ActionProfile;
import com.pi.model.ActionProfileRepository;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;
import com.pi.services.Processor;

/**
 * @author Christian Everett
 *
 */

@Controller
public class ActionController
{
	public static final String PATH = "/action";
	
	@Autowired
	private Processor bgp;
	
	@Autowired
	private ActionProfileRepository actionProfileRepository;
	
	public ActionController()
	{
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
		try
		{
			bgp.scheduleAction(state);
		}
		catch (DeviceLockedException e)
		{
			response.setStatus(403);
		}	
	}
	
	@RequestMapping(value = (PATH + "/getAllActionProfiles"), method = RequestMethod.GET)
	public @ResponseBody Collection<ActionProfile> getActionProfile(HttpServletRequest request, HttpServletResponse response)
	{
		return actionProfileRepository.getAll();	
	}
	
	@RequestMapping(value = (PATH + "/getActionProfile/{name}"), method = RequestMethod.GET)
	public @ResponseBody ActionProfile getActionProfiles(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String profileName)
	{
		ActionProfile profile = actionProfileRepository.get(profileName);	
		
		if(profile == null)
			response.setStatus(404);
		
		return profile;
	}
	
	@RequestMapping(value = (PATH + "/createActionProfile/{name}"), method = RequestMethod.POST)
	public void createActionProfile(HttpServletRequest request, HttpServletResponse response, @RequestBody Set<DeviceState> actions, 
			@PathVariable("name") String profileName)
	{
		ActionProfile profile = new ActionProfile(profileName, actions);
		
		actionProfileRepository.add(profile);
	}
	
	@RequestMapping(value = (PATH + "/createActionProfile/group"), method = RequestMethod.POST)
	public void createActionProfiles(HttpServletRequest request, HttpServletResponse response, @RequestBody Collection<ActionProfile> profiles)
	{
		for (ActionProfile profile : profiles)
		{
			actionProfileRepository.add(profile);
		}
	}
	
	@RequestMapping(value = (PATH + "/removeActionProfile/{name}"), method = RequestMethod.DELETE)
	public void removeActionProfile(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String profileName)
	{
		if(actionProfileRepository.remove(profileName) == null)
			response.setStatus(404);
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
