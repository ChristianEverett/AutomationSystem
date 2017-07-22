package com.pi.services;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.infrastructure.Service;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;

public class DeviceLoggingService extends Service
{
	private ConcurrentLinkedQueue<DeviceStateRecord> loggingQueue = new ConcurrentLinkedQueue<>();
	private Set<String> loggingEnabledDevices = ConcurrentHashMap.newKeySet();
	private Processor processor = null;
	private AtomicBoolean loggingEnabled = new AtomicBoolean(false);
	
	public DeviceLoggingService(Processor processor)
	{
		this.processor = processor;
		long interval = Long.parseLong(PropertyManger.loadProperty(PropertyKeys.DEVICE_STATE_LOG_FREQUENCY, "10"));
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
	public void executeService() throws Exception
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
	
	@Override
	protected void close()
	{
		loggingEnabled.set(false);		
	}
}
