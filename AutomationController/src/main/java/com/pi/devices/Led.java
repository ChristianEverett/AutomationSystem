/**
 * 
 */
package com.pi.devices;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi.model.LedSequence;
import com.pi.model.repository.RepositoryType;
import com.pi.services.TaskExecutorService.Task;

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

	private LedSequence recordingSequence;
	
	private Task ledEquencingTask = null;
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);

		this.RED_PIN = GPIO_PIN.getBCM_Pin(red);
		this.GREEN_PIN = GPIO_PIN.getBCM_Pin(green);
		this.BLUE_PIN = GPIO_PIN.getBCM_Pin(blue);
		
		rt.exec("sudo pigpiod");
	
		// Make task non-null
		ledEquencingTask = createTask(() ->
		{
		}, 0L, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		if(state.contains(Params.RESTART))
		{
			restartDemon();
			return;
		}
		
		synchronized (this)
		{
			if (!ledEquencingTask.isDone())
				ledEquencingTask.interruptAndCancel();
			
			if (shouldStartRecording(state))
			{
				Boolean loop = state.getParamTyped(Params.LOOP, false);
				Integer interval = state.getParamTyped(Params.INTERVAL, 30);
				String sequenceName = (String) state.getParamNonNull(Params.NAME);
				recordingSequence = new LedSequence(sequenceName, interval, loop);
			}
			else if(shouldStopRecording(state))
			{
				String sequenceName = (String) state.getParamNonNull(Params.NAME);
				
				if(recordingSequence != null)
					setRepositoryValue(RepositoryType.LedSequence, recordingSequence);
				
				recordingSequence = null;
			}
			else if (shouldPlaySequence(state))
			{
				playSequence((String) state.getParamNonNull(Params.NAME));
			}
			else
			{
				Integer red = state.getParamTyped(Params.RED, 0);
				Integer green = state.getParamTyped(Params.GREEN, 0);
				Integer blue = state.getParamTyped(Params.BLUE, 0);

				setLedColor(red, green, blue);
				
				currentColor = new Color(red, green, blue);
				updateSequenceIfRecording(red, green, blue);
			}
		}
	}

	private boolean shouldStartRecording(DeviceState state)
	{
		return state.contains(Params.RECORD) && (Boolean)state.getParam(Params.RECORD);
	}
	
	private boolean shouldStopRecording(DeviceState state)
	{
		return state.contains(Params.RECORD) && !(Boolean)state.getParam(Params.RECORD);
	}
	
	private boolean shouldPlaySequence(DeviceState state)
	{
		return !state.contains(Params.RECORD) && state.contains(Params.NAME);
	}

	private void playSequence(String sequenceName)
	{
		LedSequence sequence = getRepositoryValue(RepositoryType.LedSequence, sequenceName);

		if (sequence == null)
			throw new RuntimeException("No Sequence found for: " + sequenceName);

		ledEquencingTask = createTask(() ->
		{
			try
			{
				do 
				{
					for (Color color : sequence.getSequence())
					{
						setLedColor(color.getRed(), color.getGreen(), color.getBlue());
						Thread.sleep(sequence.getIntervalMiliseconds());
					} 
				} while (sequence.getLoopFlag());
			}
			catch (InterruptedException e)
			{	
			}
			catch (Exception e)
			{
				SystemLogger.getLogger().severe(e.getMessage());
			}	
		}, 0L, TimeUnit.MILLISECONDS);
		
		setLedColor(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
	}

	private  void updateSequenceIfRecording(int red, int green, int blue)
	{
		if (recordingSequence != null)
		{
			recordingSequence.addToSequence(red, green, blue);
		}
	}

	private void setLedColor(int red, int green, int blue)
	{
		try
		{
			rt.exec("pigs p " + RED_PIN + " " + (255 - red));
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - green));
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - blue));
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void tearDown()
	{
		setRGBPWM(name.hashCode(), 100, 100, 100);
	}

	@Override
	public DeviceState getState(DeviceState state)
	{
		state.setParam(Params.RED, currentColor.getRed());
		state.setParam(Params.GREEN, currentColor.getGreen());
		state.setParam(Params.BLUE, currentColor.getBlue());

		return state;
	}

	private void restartDemon()
	{	
		try
		{
			rt.exec("sudo pkill pigpiod");
			rt.exec("sudo pigpiod");
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not restart pigpiod");
		}
	}
	
	private native void initializeRGB(int id, int redPin, int greenPin, int bluePin);

	private native void setRGBPWM(int id, int red, int green, int blue);

	private native void closeRGB(int id);

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
