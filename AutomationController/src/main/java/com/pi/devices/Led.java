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

	private boolean feature = false;
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);

		this.RED_PIN = GPIO_PIN.getWiringPI_Pin(red).getAddress();
		this.GREEN_PIN = GPIO_PIN.getWiringPI_Pin(green).getAddress();
		this.BLUE_PIN = GPIO_PIN.getWiringPI_Pin(blue).getAddress();
		
		initializeRGB(name.hashCode(), RED_PIN, GREEN_PIN, BLUE_PIN);
		
		// Make task non-null
		ledEquencingTask = createTask(() ->
		{
		}, 0L, TimeUnit.MILLISECONDS);
	}

	@Override
	protected void performAction(DeviceState state)
	{
		try
		{
			Boolean record = state.getParamTyped(Params.SEQUENCE_RECORD, null);
			String sequenceName = state.getParamTyped(Params.NAME, null);

			if (!ledEquencingTask.isDone())
				ledEquencingTask.interruptAndCancel();

			if (record != null)
			{
				startRecording(state, record, sequenceName);
			}
			else if (sequenceName != null)
			{
				playSequence(sequenceName);
			}
			else
			{
				Integer red = state.getParamTyped(Params.RED, 0);
				Integer green = state.getParamTyped(Params.GREEN, 0);
				Integer blue = state.getParamTyped(Params.BLUE, 0);

				setLedColor(red, green, blue);
				updateSequenceIfRecording(red, green, blue);
			}
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private synchronized void startRecording(DeviceState state, Boolean record, String sequenceName)
	{
		if (record)
		{
			Boolean loop = state.getParamTyped(Params.LOOP, false);
			Integer interval = state.getParamTyped(Params.INTERVAL, 15);
			setRepositoryValue(RepositoryType.LedSequence, sequenceName, new LedSequence(sequenceName, interval, loop));
			recordingSequence = getRepositoryValue(RepositoryType.LedSequence, sequenceName);
		}
		else
		{
			if(recordingSequence != null)
				setRepositoryValue(RepositoryType.LedSequence, sequenceName, recordingSequence);
			
			recordingSequence = null;
		}
	}

	private void playSequence(String sequenceName)
	{
		LedSequence sequence = getRepositoryValue(RepositoryType.LedSequence, sequenceName);

		if (sequence == null)
			throw new RuntimeException("No Sequence found for: " + sequenceName);

		ledEquencingTask = createTask(() ->
		{
			sequence.play(this);
		}, 0L, TimeUnit.MILLISECONDS);
	}

	private synchronized void updateSequenceIfRecording(int red, int green, int blue)
	{
		if (recordingSequence != null)
		{
			recordingSequence.addToSequence(red, green, blue);
		}
	}

	public void setLedColor(int red, int green, int blue) throws IOException
	{
		currentColor = new Color(red, green, blue);

		red = (red  * 100) / 255;
		green = (green  * 100) / 255;
		blue = (blue  * 100) / 255;
		
		setRGBPWM(name.hashCode(), (100 - red), (100 - green), (100 - blue));
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
