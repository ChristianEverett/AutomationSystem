package com.pi;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;

public class SystemLogger 
{
	public static final Logger LOGGER = Logger.getLogger("SystemLogger");
	public static final SimpleFormatter format = new SimpleFormatter();
	
	static
	{
		try
		{
			//LOGGER.setUseParentHandlers(false);
			FileHandler handle = new FileHandler(PropertyManger.loadPropertyNotNull(PropertyKeys.LOGFILE));
			handle.setFormatter(format);
			LOGGER.addHandler(handle);
		}
		catch (Exception e)
		{
			System.out.println("Can't open log file");
		}
	}
	
	private SystemLogger()
	{
	}

	public static Logger getLogger()
	{
		return LOGGER;
	}
}
