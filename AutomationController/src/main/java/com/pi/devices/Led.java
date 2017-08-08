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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.pi.SystemLogger;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.infrastructure.util.GPIO_PIN;
import com.pi.model.DeviceState;
import com.pi.model.LedSequence;
import com.pi.model.repository.LedSequenceRepoistory;
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

	private AtomicBoolean recording = new AtomicBoolean(false);
	private String recordingName = "";

	private Task ledEquencingTask = null;

	private boolean feature = false;
	
	public Led(String name, int red, int green, int blue) throws IOException
	{
		super(name);
		Process pr = rt.exec("sudo pigpiod");

		this.RED_PIN = GPIO_PIN.getBCM_Pin(red);
		this.GREEN_PIN = GPIO_PIN.getBCM_Pin(green);
		this.BLUE_PIN = GPIO_PIN.getBCM_Pin(blue);

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
			Boolean record = state.getParamTyped(Params.SEQUENCE_RECORD, Boolean.class, null);
			String sequenceName = state.getParamTyped(Params.NAME, String.class, null);

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
				Integer red = state.getParamTyped(Params.RED, Integer.class, 0);
				Integer green = state.getParamTyped(Params.GREEN, Integer.class, 0);
				Integer blue = state.getParamTyped(Params.BLUE, Integer.class, 0);

				setLedColor(red, green, blue);
				updateSequenceIfRecording(red, green, blue);
			}
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}
	}

	private void startRecording(DeviceState state, Boolean record, String sequenceName)
	{
		recording.set(record);

		if (record)
		{
			Boolean loop = state.getParamTyped(Params.LOOP, Boolean.class, false);
			Integer interval = state.getParamTyped(Params.INTERVAL, Integer.class, 15);
			setRepositoryValue(RepositoryType.LedSequence, sequenceName, new LedSequence(interval, loop));
			recordingName = sequenceName;
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

	private void updateSequenceIfRecording(int red, int green, int blue)
	{
		if (recording.get())
		{
			LedSequence sequence = getRepositoryValue(RepositoryType.LedSequence, recordingName);
			sequence.addToSequence(red, green, blue);
		}
	}

	@Override
	protected void load(DeviceState state) throws IOException
	{
		 this.execute(state);
	}

	public void setLedColor(int red, int green, int blue) throws IOException
	{
		currentColor = new Color(red, green, blue);

		if (feature)
		{
			initializeRGB(name.hashCode(), RED_PIN, GREEN_PIN, BLUE_PIN);
			setRGBPWM(name.hashCode(), (255 - red), (255 - green), (255 - blue));
		}
		else
		{
			rt.exec("pigs p " + RED_PIN + " " + (255 - currentColor.getRed()));
			rt.exec("pigs p " + GREEN_PIN + " " + (255 - currentColor.getGreen()));
			rt.exec("pigs p " + BLUE_PIN + " " + (255 - currentColor.getBlue()));
		}
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
