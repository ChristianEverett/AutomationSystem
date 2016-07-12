/**
 * 
 */
package com.pi.devices;

import java.io.IOException;

import com.pi.Application;
import com.pi.infrastructure.Device;
import com.pi.repository.Action;


/**
 * @author Christian Everett
 *
 */
public class Led extends Device
{
	private final int RED_PIN;
	private final int GREEN_PIN;
	private final int BLUE_PIN;

	public Led(int red, int green, int blue) throws IOException
	{
		this.RED_PIN = red;
		this.GREEN_PIN = green;
		this.BLUE_PIN = blue;
	}

	@Override
	public boolean performAction(Action action) throws IOException
	{
		if(isClosed)
			return false;
		
		int RED, GREEN, BLUE;

		String[] splited = action.getData().split("\\s+");
		
		if(splited.length != 3)
			return false;
		
		RED = Integer.parseInt(splited[0]);
		GREEN = Integer.parseInt(splited[1]);
		BLUE = Integer.parseInt(splited[2]);

		rt.exec("pigs p " + RED_PIN + " " + (255 - RED) + " &");
		rt.exec("pigs p " + GREEN_PIN + " " + (255 - GREEN) + " &");
		rt.exec("pigs p " + BLUE_PIN + " " + (255 - BLUE) + " &");
		
		return true;
	}

	@Override
	public void close()
	{
		try
		{
			rt.exec("pigs p " + RED_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - 0) + " &");
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
}
