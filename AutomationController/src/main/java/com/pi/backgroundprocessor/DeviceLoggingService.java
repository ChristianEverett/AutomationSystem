package com.pi.backgroundprocessor;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;

public class DeviceLoggingService implements Runnable
{
	private ConcurrentLinkedQueue<DeviceStateRecord> loggingQueue = new ConcurrentLinkedQueue<>();
	private Set<String> loggingEnabledDevices = ConcurrentHashMap.newKeySet();
	private static DeviceLoggingService singlton = null;
	private Task loggingTask = null;
	private Processor processor = null;
	private AtomicBoolean loggingEnabled = new AtomicBoolean(false);
	
	private DeviceLoggingService(Processor processor)
	{
		this.processor = processor;
		long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DEVICE_STATE_LOG_FREQUENCY, "10"));
		loggingTask = processor.getTaskExecutorService().scheduleTask(this, 10L, interval, TimeUnit.SECONDS);
	}
	
	static DeviceLoggingService start(Processor processor)
	{
		if(singlton == null)
			singlton = new DeviceLoggingService(processor);
		return singlton;
	}
	
	public void enableLogging(String deviceName)
	{
		loggingEnabledDevices.add(deviceName);
	}
	
	public void disableLogging(String deviceName)
	{
		loggingEnabledDevices.remove(deviceName);
	}
	
	public void log(DeviceState state)
	{
		if(loggingEnabled.get() && loggingEnabledDevices.contains(state) && loggingQueue.size() < 600)
		{
			loggingQueue.add(new DeviceStateRecord(state));
		}
	}

	@Override
	public void run()
	{
		try
		{
			loggingEnabled.set(true);
			List<DeviceStateRecord> states = new LinkedList<>();
			
			while(!loggingQueue.isEmpty())
			{
				DeviceStateRecord state = loggingQueue.poll();
				states.add(state);
			}
			
			if(!states.isEmpty())
				processor.getPersistenceManger().commitToDeviceLog(states);
		}
		catch (Exception e)
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
