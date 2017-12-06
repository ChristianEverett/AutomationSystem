package com.pi.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.pi.SystemLogger;
import com.pi.devices.Led;

@Entity
public class LedSequence extends Model
{
	@Id
	@Column(length = 100)
	private String ledSequenceName;
	@ElementCollection
	private List<Color> sequence = new LinkedList<>();
	private Integer intervalMiliseconds = 15;
	private Boolean loopFlag = false;
	
	public LedSequence()
	{
		
	}
	
	public LedSequence(String name, Integer interval, Boolean loop)
	{
		this.intervalMiliseconds = interval;
		this.loopFlag = loop;
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
			} while (loopFlag);
		}
		catch (InterruptedException e)
		{	
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	public void setLedSequenceName(String ledSequenceName)
	{
		this.ledSequenceName = ledSequenceName;
	}

	public void setSequence(List<Color> sequence)
	{
		this.sequence = sequence;
	}

	public void setIntervalMiliseconds(Integer intervalMiliseconds)
	{
		this.intervalMiliseconds = intervalMiliseconds;
	}

	public void setLoop(Boolean loop)
	{
		this.loopFlag = loop;
	}
}
