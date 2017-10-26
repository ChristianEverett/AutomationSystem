/**
 * 
 */
package com.pi;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import com.pi.Node.NodeControllerImpl;

/**
 * @author Christian Everett
 *
 */
public class Main
{
	public static void main(String[] args) throws SecurityException, IOException
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				SystemLogger.getLogger().severe("Shutdown Hook Running");
			}
		});
		
		NodeControllerImpl.start(args[0]);
	}
}
