package com.pi.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pi.services.PrimaryNodeControllerImpl;

@Controller
@RequestMapping("/system")
public class SystemContoller
{
	public static final String UPTIME = "up_time";
	
	@Autowired
	private PrimaryNodeControllerImpl node;
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSystemStatus(HttpServletResponse response)
	{
		Map<String, Object> statusMap = new HashMap<>();
		statusMap.put(UPTIME, (PrimaryNodeControllerImpl.UP_TIME - System.currentTimeMillis())/1000);
		return statusMap;
	}
}
