package com.pi.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi.infrastructure.BaseService;
import com.pi.infrastructure.util.PropertyManger;
import com.pi.infrastructure.util.PropertyManger.PropertyKeys;
import com.pi.model.DeviceState;
import com.pi.model.DeviceStateRecord;
import com.pi.model.repository.DeviceStateRecordJpaRepository;

@Service
public class DeviceLoggingService extends BaseService
{
	private ConcurrentLinkedQueue<DeviceStateRecord> loggingQueue = new ConcurrentLinkedQueue<>();
	private Set<String> loggingEnabledDevices = ConcurrentHashMap.newKeySet();
	private AtomicBoolean loggingEnabled = new AtomicBoolean(false);
	
	@Autowired
	private DeviceStateRecordJpaRepository deviceStateRecordJpaRepository;
	
	private DeviceLoggingService()
	{
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

	public List<DeviceStateRecord> getRecords(String start, String end)
	{
		LocalDateTime startDateTime = LocalDateTime.parse(start, DeviceStateRecord.sdf);
		LocalDateTime endDateTime = LocalDateTime.parse(end, DeviceStateRecord.sdf);
		
		return deviceStateRecordJpaRepository.findByDateBetween(startDateTime, endDateTime);
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
			deviceStateRecordJpaRepository.save(states);
	}
	
	@Override
	protected void close()
	{
		loggingEnabled.set(false);		
	}
}
