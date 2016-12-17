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

import com.pi.backgroundprocessor.Processor;
import com.pi.model.TimedAction;

/**
 * @author Christian Everett
 *
 */

@Controller
public class TimerController
{
	private static final String PATH = "/timer";
	
	Processor bgp; 
	
	public TimerController()
	{
		bgp = Processor.getBackgroundProcessor();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Entry<Integer, TimedAction>> getTimers(HttpServletResponse response)
	{
		return bgp.getTimeActionProcessor().retrieveAllTimedActions();
	}
	
	@RequestMapping(value = (PATH + "/add"), method = RequestMethod.POST)
	public @ResponseBody Integer addTimer(HttpServletRequest request, HttpServletResponse response, @RequestBody TimedAction timer)
	{
		bgp.getTimeActionProcessor().load(timer);
		return timer.hashCode();
	}
	
	@RequestMapping(value = (PATH + "/{id}"), method = RequestMethod.POST)
	public @ResponseBody void changeTimer(HttpServletRequest request, HttpServletResponse response, @RequestBody TimedAction timer, @PathVariable("id") Integer id)
	{
		if(bgp.getTimeActionProcessor().getTimedActionByID(id) == null)
		{
			response.setStatus(404);
			return;
		}
		
		bgp.getTimeActionProcessor().updateTimedActionByID(id, timer);
	}
	
	@RequestMapping(value = (PATH + "/{id}"), method = RequestMethod.DELETE)
	public void deleteTimer(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") Integer id)
	{
		if(bgp.getTimeActionProcessor().delete(id) == null)
			response.setStatus(404);
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.DELETE)
	public void deleteAllTimers(HttpServletRequest request, HttpServletResponse response)
	{
		bgp.getTimeActionProcessor().deleteAll();
	}
}
