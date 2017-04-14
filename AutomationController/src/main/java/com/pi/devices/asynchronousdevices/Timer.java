package com.pi.devices.asynchronousdevices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlRootElement;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.AsynchronousDevice;
import com.pi.infrastructure.Device;
import com.pi.infrastructure.DeviceType;
import com.pi.infrastructure.DeviceType.Params;
import com.pi.model.DeviceState;

public class Timer extends AsynchronousDevice implements Runnable
{
	private static final SimpleDateFormat formatter = new SimpleDateFormat("a hh:mm");
	private Task timer = null;

	public Timer(String name) throws IOException
	{
		super(name);
		LocalDateTime now = LocalDateTime.now();
		long second = now.getSecond();
		
		timer = createFixedRateTask(this, 60 - second, 60L, TimeUnit.SECONDS);
	}

	@Override
	public void run()
	{
		try
		{
			update(getState(false));
		}
		catch (IOException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}	
	}
	
	@Override
	protected void performAction(DeviceState state)
	{
	}

	@Override
	public DeviceState getState(Boolean forDatabase) throws IOException
	{
		DeviceState state = Device.createNewDeviceState(name);
		state.setParam(Params.TIME, formatter.format(new Date()));
		return state;
	}

	@Override
	public void close() throws IOException
	{
		timer.cancel();
	}

	@Override
	public String getType()
	{
		return DeviceType.TIMER;
	}
	
	@Override
	public List<String> getExpectedParams()
	{
		return null;
	}
	
	@XmlRootElement(name = DEVICE)
	public static class TimerConfig extends DeviceConfig
	{
		@Override
		public Device buildDevice() throws IOException
		{
			return new Timer(name);
		}
	}
}
