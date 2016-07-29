/**
 * 
 */
package com.thermostat;

import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.thermostat.http.ThermostatServer;

/**
 * @author Christian Everett
 *
 */
public class Main
{
	public static final Logger LOGGER = Logger.getLogger("SystemLogger");
	
	public static void main(String[] args)
	{
		try
		{
			if(args.length != 0)
				LOGGER.setUseParentHandlers(false);
			LOGGER.addHandler(new FileHandler("./log"));
		}
		catch(Exception e)
		{
			System.out.println("Can't open log file");
		}
		
		ThermostatServer.runThermostatServer();
	}
}
