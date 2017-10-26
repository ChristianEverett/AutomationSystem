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
import com.pi.infrastructure.util.EventRegistry;
import com.pi.model.ActionProfile;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;
import com.pi.model.repository.ActionProfileJpaRepository;
import com.pi.services.DeviceLoggingService;
import com.pi.services.EventProcessingService;
import com.pi.services.PrimaryNodeControllerImpl;
import com.pi.services.RepositoryUpdateNotifierService;

/**
 * @author Christian Everett
 *
 */

@Controller
public class ActionController
{
	public static final String PATH = "/action";
	
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
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<DeviceState> getAllStates(HttpServletResponse response)
	{
		try
		{
			return nodeController.getStates();
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
	
	@RequestMapping(value = (PATH + "/{device}"), method = RequestMethod.POST)
	public void scheduleAction(HttpServletRequest request, HttpServletResponse response, @RequestBody DeviceState state)
	{		
		nodeController.scheduleAction(state);
	}
	
	@RequestMapping(value = (PATH + "/getAllActionProfiles"), method = RequestMethod.GET)
	public @ResponseBody Collection<ActionProfile> getActionProfile(HttpServletRequest request, HttpServletResponse response)
	{
		return actionProfileRepository.findAll();	
	}
	
	@RequestMapping(value = (PATH + "/trigger/{name}"), method = RequestMethod.POST)
	public void triggerActionProfile(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String actionProfileName)
	{
		nodeController.trigger(actionProfileName);
	}
	
	@RequestMapping(value = (PATH + "/getActionProfile/{name}"), method = RequestMethod.GET)
	public @ResponseBody ActionProfile getActionProfiles(HttpServletRequest request, HttpServletResponse response, @PathVariable("name") String profileName)
	{
		ActionProfile profile = actionProfileRepository.findOne(profileName);	
		
		if(profile == null)
			response.setStatus(404);
		
		return profile;
	}
	
	@RequestMapping(value = (PATH + "/createActionProfile/{name}"), method = RequestMethod.POST)
	public void createActionProfile(HttpServletRequest request, HttpServletResponse response, @RequestBody Set<DeviceState> actions, 
			@PathVariable("name") String profileName)
	{
		ActionProfile profile = new ActionProfile(profileName, actions);
		
		repositoryUpdateNotifierService.newActionProfile(actionProfileRepository.save(profile));
	}
	
	@RequestMapping(value = (PATH + "/createActionProfile/group"), method = RequestMethod.POST)
	public void createActionProfiles(HttpServletRequest request, HttpServletResponse response, @RequestBody Collection<ActionProfile> profiles)
	{
		repositoryUpdateNotifierService.newActionProfile(actionProfileRepository.save(profiles));
	}
	
	@RequestMapping(value = (PATH + "/removeActionProfile/{name}"), method = RequestMethod.DELETE)
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
