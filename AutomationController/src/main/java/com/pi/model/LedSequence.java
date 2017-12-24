package com.pi.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import com.pi.SystemLogger;
import com.pi.devices.Led;

@Entity
public class LedSequence extends Model
{
	@Id
	@Column(length = 100)
	private String ledSequenceName;
	@ElementCollection(fetch = FetchType.EAGER)
	private List<Color> sequence = new LinkedList<>();
	private Integer intervalMiliseconds = 30;
	private Boolean loopFlag = false;
	
	public LedSequence()
	{
		
	}
	
	public LedSequence(String name, Integer interval, Boolean loop)
	{
		this.ledSequenceName = name;
		this.intervalMiliseconds = interval;
		this.loopFlag = loop;
	}
	
	public void addToSequence(int red, int green, int blue)
	{
		sequence.add(new Color(red, green, blue));
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

	public String getLedSequenceName()
	{
		return ledSequenceName;
	}

	public List<Color> getSequence()
	{
		return sequence;
	}

	public Integer getIntervalMiliseconds()
	{
		return intervalMiliseconds;
	}

	public Boolean getLoopFlag()
	{
		return loopFlag;
	}
}
