/**
 * 
 */
package com.pi;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.pi.Node.NodeController;

/**
 * @author Christian Everett
 *
 */
public class Main
{
	public static final Logger LOGGER = Logger.getLogger("SystemLogger");
	
	public static void main(String[] args) throws SecurityException, IOException
	{
		LOGGER.addHandler(new FileHandler("./log"));
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				Application.LOGGER.severe("Shutdown Hook Running");
			}
		});
		
		NodeController.getInstance();
	}
}
