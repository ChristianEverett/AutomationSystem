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

public class Timer extends AsynchronousDevice
{
	private static final SimpleDateFormat formatter = new SimpleDateFormat("a hh:mm");
	private Task timer = null;

	public Timer(String name) throws IOException
	{
		this(name, LocalDateTime.now().getSecond());
	}
	
	private Timer(String name, long second) throws IOException
	{
		super(name, 60 - second, 60L, TimeUnit.SECONDS, true);
	}
	
	@Override
	protected void update() throws Exception
	{
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
	protected void tearDown() throws IOException
	{
		timer.cancel();
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
