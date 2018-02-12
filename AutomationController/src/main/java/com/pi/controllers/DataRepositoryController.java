package com.pi.controllers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.infrastructure.RepoistoryManager;
import com.pi.model.Model;

@Controller
@RequestMapping(value = "/repository")
public class DataRepositoryController
{
	@Autowired
	private RepoistoryManager repoistoryManager;

	public DataRepositoryController()
	{
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Collection<String> getRepositorys(HttpServletRequest request, HttpServletResponse response)
	{
		return repoistoryManager.getAllRepositorys();
	}
	
	@RequestMapping(value = "/{repository}/all", method = RequestMethod.GET)
	public @ResponseBody Collection<Model> getValues(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository)
	{
		Collection<Model> objects = repoistoryManager.getRepositoryValues(repository);
		
		return objects;
	}
	
	@RequestMapping(value = "/{repository}", method = RequestMethod.GET)
	public @ResponseBody Model getValue(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository
			,@RequestParam("key") String key)
	{
		Model object = repoistoryManager.getRepositoryValue(repository, key);
		
		return object;
	}
	
	@RequestMapping(value = "/{repository}/all", method = RequestMethod.DELETE)
	public void deleteValues(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository)
	{
		repoistoryManager.clearRepositoryValues(repository);
	}
	
	@RequestMapping(value = "/{repository}", method = RequestMethod.DELETE)
	public void deleteValues(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository
			,@RequestParam("key") String key)
	{
		repoistoryManager.clearRepositoryValue(repository, key);
	}
	
//	@RequestMapping(value = "/{repository}", method = RequestMethod.POST)
//	public void setValue(HttpServletRequest request, HttpServletResponse response, @PathVariable("repository") String repository
//			,@RequestParam("key") String key)
//	{
//		try(ObjectInputStream input = new ObjectInputStream(request.getInputStream()))
//		{
//			processor.setRepositoryValue(repository, key, (Serializable)input.readObject());
//		}
//		catch (Exception e)
//		{
//			response.setStatus(503);
//			SystemLogger.getLogger().severe(e.getMessage());
//		}	
//	}
//	
//	private void returnData(HttpServletResponse response, Object object)
//	{
//		try(ObjectOutputStream output = new ObjectOutputStream(response.getOutputStream()))
//		{
//			output.writeObject(object);
//			output.flush();
//		}
//		catch (Exception e)
//		{
//			response.setStatus(503);
//			SystemLogger.getLogger().severe(e.getMessage());
//		}
//	}
}
