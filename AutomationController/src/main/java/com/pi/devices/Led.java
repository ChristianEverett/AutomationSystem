/**
 * 
 */
package com.pi.devices;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;


/**
 * @author Christian Everett
 *
 */
public class Led extends Device
{
	private final int RED_PIN;
	private final int GREEN_PIN;
	private final int BLUE_PIN;
	private Color currentColor = new Color(0, 0, 0);
	
	private AtomicBoolean recording = new AtomicBoolean(false);
	private String recordingName = "";
	
	private Map<String, LedSequence> sequences = new HashMap<>();
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);
		Process pr = rt.exec("sudo pigpiod");
		
		this.RED_PIN = GPIO_PIN.getBCM_Pin(red);
		this.GREEN_PIN = GPIO_PIN.getBCM_Pin(green);
		this.BLUE_PIN = GPIO_PIN.getBCM_Pin(blue);
		
//		initializeRGB(name.hashCode(), RED_PIN, GREEN_PIN, BLUE_PIN);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		try
		{
			Boolean record = state.getParamTyped(Params.SEQUENCE_RECORD, Boolean.class);
			String sequenceName = state.getParamTyped(Params.NAME, String.class);
			
			if (record != null)
			{
				recording.set(record);
				
				if (record)
				{
					Integer interval = state.getParamTyped(Params.INTERVAL, Integer.class);
					sequences.put(sequenceName, new LedSequence(interval));
					recordingName = sequenceName;
				}
			}
			else if(sequenceName != null)
			{
				LedSequence sequence = sequences.get(sequenceName);
				
				if(sequence == null)
					throw new RuntimeException("No Sequence found for: " + sequenceName);
				
				sequence.play(this);
			}
			else
			{
				Integer red = (Integer) state.getParam(Params.RED);
				Integer green = (Integer) state.getParam(Params.GREEN);
				Integer blue = (Integer) state.getParam(Params.BLUE);
				
				setLedColor(red, green, blue);
				
				if(recording.get())
				{
					LedSequence sequence = sequences.get(recordingName);
					sequence.addToSequence(red, green, blue);
				}
			}
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void load(DeviceState state) throws IOException
	{
		sequences = (Map<String, LedSequence>) state.getParam(Params.SEQUENCES);
		this.execute(state);
	}

	private void setLedColor(Integer red, Integer green, Integer blue) throws IOException
	{
//		setRGBPWM(name.hashCode(), (255 - red), (255 - green), (255 - blue));
		rt.exec("pigs p " + RED_PIN + " " + (255 - red));
		rt.exec("pigs p " + GREEN_PIN + " " + (255 - green));
		rt.exec("pigs p " + BLUE_PIN + " " + (255 - blue));
		
		currentColor = new Color(red, green, blue);
	}

	@Override
	protected void tearDown()
	{
		try
		{
			rt.exec("pigs p " + RED_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - 0) + " &");
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - 0) + " &");
		}
		catch (IOException e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	@Override
	public DeviceState getState(Boolean forDatabase)
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.RED, currentColor.getRed());
		state.setParam(Params.GREEN, currentColor.getGreen());
		state.setParam(Params.BLUE, currentColor.getBlue());
		
		if(forDatabase)
		{
			state.setParam(Params.SEQUENCES, sequences);
		}
		
		return state;
	}
	
	private native void initializeRGB(int id, int redPin, int greenPin, int bluePin);
	private native void setRGBPWM(int id, int red, int green, int blue) ;
	private native void closeRGB(int id);
	
	private static class LedSequence implements Serializable
	{
		private List<Color> sequence = new LinkedList<>();
		private Integer intervalMiliseconds = 12;
		private Boolean loop = false;
		
		public LedSequence(Integer interval)
		{
			this(interval, false);
		}
		
		public LedSequence(Integer interval, Boolean loop)
		{
			this.intervalMiliseconds = interval;
			this.loop = loop;
		}
		
		public synchronized void addToSequence(int red, int green, int blue)
		{
			sequence.add(new Color(red, green, blue));
		}
		
		public synchronized void play(Led led)
		{
			try
			{
				for(Color color : sequence)
				{
					led.setLedColor(color.getRed(), color.getGreen(), color.getBlue());
					Thread.sleep(intervalMiliseconds);
				}
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}
		}
	}
	
	@XmlRootElement(name = DEVICE)
	public static class LedConfig extends DeviceConfig
	{
		private int red, green, blue;

		@Override
		public Device buildDevice() throws IOException
		{
			return new Led(name, red, green, blue);
		}

		@XmlElement
		public void setRed(int red)
		{
			this.red = red;
		}

		@XmlElement
		public void setGreen(int green)
		{
			this.green = green;
		}

		@XmlElement
		public void setBlue(int blue)
		{
			this.blue = blue;
		}
	}
}
