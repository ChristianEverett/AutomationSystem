/**
 * 
 */
package com.pi.controllers;

import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.pi.Application;
import com.pi.backgroundprocessor.Processor;
import com.pi.repository.Action;
import com.pi.repository.StateRepository;

/**
 * @author Christian Everett
 *
 */

@Controller
public class ActionController
{
	private static final String PATH = "/action";
	
	@Autowired
	private StateRepository stateRepository;
	
	@RequestMapping(value = PATH, method = RequestMethod.GET)
	public @ResponseBody Collection<Action> getStates(HttpServletResponse response)
	{
		try
		{
			return Lists.newArrayList(stateRepository.findAll());
		}
		catch (Exception e)
		{
			response.setStatus(503);
			Application.LOGGER.severe("Can't access states table");
		}
		
		return null;
	}
	
	@RequestMapping(value = (PATH + "/add"), method = RequestMethod.POST)
	public void addAction(HttpServletRequest request, HttpServletResponse response, @RequestBody Action action)
	{
		Processor.getBackgroundProcessor().scheduleAction(action);
	}
}
