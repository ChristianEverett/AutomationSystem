package com.pi.backgroundprocessor;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;

public class DeviceLoggingService implements Runnable
{
	private ConcurrentLinkedQueue<DeviceState> loggingQueue = new ConcurrentLinkedQueue<>();
	private static DeviceLoggingService singlton = null;
	private Task loggingTask = null;
	private Processor processor = null;
	private AtomicBoolean loggingEnabled = new AtomicBoolean(false);
	
	private DeviceLoggingService(Processor processor)
	{
		long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DEVICE_STATE_LOG_FREQUENCY, "5"));
		loggingTask = processor.getTaskExecutorService().scheduleTask(this, 10L, interval, TimeUnit.SECONDS);
	}
	
	static DeviceLoggingService start(Processor processor)
	{
		if(singlton == null)
			singlton = new DeviceLoggingService(processor);
		return singlton;
	}
	
	public void log(DeviceState state)
	{
		if(loggingEnabled.get())
			loggingQueue.add(state);
	}

	@Override
	public void run()
	{
		try
		{
			loggingEnabled.set(true);
			List<DeviceState> states = new LinkedList<>();
			
			while(!loggingQueue.isEmpty())
			{
				DeviceState state = loggingQueue.poll();
				states.add(state);
			}
			
			if(!states.isEmpty())
				processor.getPersistenceManger().commitToDeviceLog(states);
		}
		catch (SQLException e)
		{
			Application.LOGGER.severe(e.getMessage());
		}
	}
	
	void stop()
	{
		loggingEnabled.set(false);
		loggingTask.cancel();
		singlton = null;
	}
}
