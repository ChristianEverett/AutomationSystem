package com.pi.controllers;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import com.pi.SystemLogger;
import com.pi.services.PrimaryNodeControllerImpl;

@Controller
@RequestMapping(value = "/repository")
public class DataRepositoryController
{
	@Autowired
	private PrimaryNodeControllerImpl processor;

	public DataRepositoryController()
	{
	}
	
	@RequestMapping(value = "/{repository}/all", method = RequestMethod.GET)
	public void getValues(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository)
	{
		Object object = processor.getRepositoryValues(repository);
		
		returnData(response, object);
	}
	
	@RequestMapping(value = "/{repository}", method = RequestMethod.GET)
	public void getValue(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository
			,@RequestParam("key") String key)
	{
		Object object = processor.getRepositoryValue(repository, key);
		
		returnData(response, object);
	}
	
	@RequestMapping(value = "/{repository}", method = RequestMethod.POST)
	public void setValue(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository
			,@RequestParam("key") String key)
	{
		try(ObjectInputStream input = new ObjectInputStream(request.getInputStream()))
		{
			processor.setRepositoryValue(repository, key, (Serializable)input.readObject());
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
		}	
	}
	
	private void returnData(HttpServletResponse response, Object object)
	{
		try(ObjectOutputStream output = new ObjectOutputStream(response.getOutputStream()))
		{
			output.writeObject(object);
			output.flush();
		}
		catch (Exception e)
		{
			response.setStatus(503);
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
}