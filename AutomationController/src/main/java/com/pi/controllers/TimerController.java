/**
 * 
 */
package com.pi.controllers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
	
	@Autowired
	TimerRepository timerRepository;
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Timer> getTimers(HttpServletResponse response)
	{
		return Lists.newArrayList(timerRepository.findAll());
	}
	
	@RequestMapping(value = (PATH + "/add"), method = RequestMethod.POST)
	public void addTimer(HttpServletRequest request, HttpServletResponse response, @RequestBody Timer timer)
	{
		timerRepository.save(timer);
	}
	
	@RequestMapping(value = (PATH + "/delete"), method = RequestMethod.DELETE)
	public void deleteTimer(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id)
	{
		if(timerRepository.findOne(id) != null)
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
