/**
 * 
 */
package com.pi.controllers;

import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.util.EventRegistry;
import com.pi.model.ActionProfile;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;
import com.pi.model.repository.ActionProfileJpaRepository;
import com.pi.services.DeviceLoggingService;
import com.pi.services.PrimaryNodeControllerImpl;
import com.pi.services.RepositoryUpdateNotifierService;

/**
 * @author Christian Everett
 *
 */

@Controller
@RequestMapping(value = "/action")
public class ActionController
{
	@Autowired
	private PrimaryNodeControllerImpl nodeController;
	
	@Autowired
	private ActionProfileJpaRepository actionProfileRepository;
	
	@Autowired
	private DeviceLoggingService deviceLoggingService;
	
	@Autowired
	private EventRegistry eventRegistry;
	
	@Autowired
	private RepositoryUpdateNotifierService repositoryUpdateNotifierService;
	
	public ActionController()
	{
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceState> getAllStates(HttpServletResponse response)
	{
		List<DeviceState> states = nodeController.getStates();
		return states.stream().sorted((state1, state2) -> state1.getName().compareTo(state2.getName())).collect(Collectors.toList());
	}
	
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public @ResponseBody DeviceState getState(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String deviceName)
	{
		return getState(response, deviceName);
	}
	
	@RequestMapping(value = "/AC/{name}", method = RequestMethod.GET)
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
	
	@RequestMapping(value = "/records", method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceStateRecord> getRecordsFor(HttpServletResponse response, 
			@RequestParam("start") String start, @RequestParam("end") String end)
	{
		try
		{
			List<DeviceStateRecord> list = deviceLoggingService.getRecords(start, end);
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
	
	@RequestMapping(value = "/{device}", method = RequestMethod.POST)
	public void scheduleAction(HttpServletRequest request, HttpServletResponse response, @RequestBody DeviceState state)
	{		
		nodeController.scheduleAction(state);	
	}
	
	@RequestMapping(value = "/getAllActionProfiles", method = RequestMethod.GET)
	public @ResponseBody Collection<ActionProfile> getActionProfile(HttpServletRequest request, HttpServletResponse response)
	{
		return actionProfileRepository.findAll();	
	}
	
	@RequestMapping(value = "/trigger/{name}", method = RequestMethod.POST)
	public void triggerActionProfile(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String actionProfileName)
	{
		nodeController.trigger(actionProfileName);
	}
	
	@RequestMapping(value = "/getActionProfile/{name}", method = RequestMethod.GET)
	public @ResponseBody ActionProfile getActionProfiles(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String profileName)
	{
		ActionProfile profile = actionProfileRepository.findOne(profileName);	
		
		if(profile == null)
			response.setStatus(404);
		
		return profile;
	}
	
	@RequestMapping(value = "/createActionProfile/group", method = RequestMethod.POST)
	public void createActionProfiles(HttpServletRequest request, HttpServletResponse response, @RequestBody Collection<ActionProfile> profiles)
	{
		profiles = profiles.stream().filter(profile -> !actionProfileRepository.exists(profile.getName())).collect(Collectors.toList());
		
		if(!profiles.isEmpty())
			repositoryUpdateNotifierService.newActionProfile(actionProfileRepository.save(profiles));
	}
	
	@RequestMapping(value = "/removeActionProfile/{name}", method = RequestMethod.DELETE)
	public void removeActionProfile(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String profileName)
	{
		actionProfileRepository.delete(profileName);
		eventRegistry.removeEventsWithActionProfile(profileName);
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
