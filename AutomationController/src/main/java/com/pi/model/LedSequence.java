package com.pi.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.pi.SystemLogger;
import com.pi.devices.Led;

public class LedSequence extends DatabaseElement
{
	private List<Color> sequence = new LinkedList<>();
	private Integer intervalMiliseconds = 15;
	private Boolean loop = new Boolean(false);
	
	public LedSequence(Integer interval, Boolean loop)
	{
		this.intervalMiliseconds = interval;
		this.loop = loop;
	}
	
	public void addToSequence(int red, int green, int blue)
	{
		sequence.add(new Color(red, green, blue));
	}
	
	public void play(Led led)
	{
		try
		{
			do 
			{
				for (Color color : sequence)
				{
					led.setLedColor(color.getRed(), color.getGreen(), color.getBlue());
					Thread.sleep(intervalMiliseconds);
				} 
			} while (loop);
		}
		catch (InterruptedException e)
		{	
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
}
