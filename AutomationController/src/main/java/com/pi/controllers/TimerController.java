/**
 * 
 */
package com.pi.controllers;

import java.util.Collection;
import java.util.Map.Entry;

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

import com.google.common.collect.Lists;
import com.pi.repository.Timer;
import com.pi.repository.TimerRepository;

/**
 * @author Christian Everett
 *
 */

@Controller
public class TimerController
{
	private static final String PATH = "/timer";
	
	TimerRepository timerRepository;
	
	public TimerController()
	{
		timerRepository = timerRepository.getInstance();
	}
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Entry<Long, Timer>> getTimers(HttpServletResponse response)
	{
		return timerRepository.getAllElement();
	}
	
	@RequestMapping(value = (PATH + "/add"), method = RequestMethod.POST)
	public @ResponseBody Long addTimer(HttpServletRequest request, HttpServletResponse response, @RequestBody Timer timer)
	{
		return timerRepository.add(timer);
	}
	
	@RequestMapping(value = (PATH + "/{id}"), method = RequestMethod.POST)
	public @ResponseBody Long changeTimer(HttpServletRequest request, HttpServletResponse response, @RequestBody Timer timer, @PathVariable("id") Long id)
	{
		if(timerRepository.delete(id) == null)
			response.setStatus(404);
		return timerRepository.add(timer);
	}
	
	@RequestMapping(value = (PATH + "/delete"), method = RequestMethod.DELETE)
	public void deleteTimer(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id)
	{
		if(timerRepository.get(id) != null)
			timerRepository.delete(id);
		else
			response.setStatus(404);
	}
	
	@RequestMapping(value = (PATH + "/delete/all"), method = RequestMethod.DELETE)
	public void deleteAllTimers(HttpServletRequest request, HttpServletResponse response)
	{
		timerRepository.deleteAll();
	}
}
