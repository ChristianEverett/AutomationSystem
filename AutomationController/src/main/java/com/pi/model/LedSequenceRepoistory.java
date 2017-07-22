package com.pi.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.pi.SystemLogger;
import com.pi.devices.Led;

public class LedSequenceRepoistory
{
	private static LedSequenceRepoistory singleton;
	private Map<String, LedSequence> sequences = new HashMap<>();
	
	public static LedSequenceRepoistory getInstance()
	{
		if(singleton == null)
			singleton = new LedSequenceRepoistory();
		
		return singleton;
	}
	
	public LedSequence get(String name)
	{
		return sequences.get(name);
	}
	
	public void put(String name, LedSequence sequence)
	{
		sequences.put(name, sequence);
	}
	
	public static class LedSequence implements Serializable
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
}
