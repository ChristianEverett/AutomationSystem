package com.pi.model;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.Device;

public class Event extends DatabaseElement
{
	private Task stateRestoreTask = null;
	private Integer durationMinutes = 0;
	private HashMap<String, DeviceState> triggerEvents = new HashMap<>();
	private HashMap<String, DeviceState> triggerSetStateCache = new HashMap<>();
	private DeviceState eventState;
	private DeviceState preEventState;

	private AtomicBoolean isTriggered = new AtomicBoolean(false);

	public Event(DeviceState eventAction)
	{
		this.eventState = eventAction;
		// make task non-null
		stateRestoreTask = Device.createTask(() ->
		{
		}, 0L, TimeUnit.MINUTES);
	}

	public boolean updateAndCheckIfTriggered(DeviceState state)
	{
		triggerSetStateCache.put(state.getName(), state);

		for(Entry<String, DeviceState> pair: triggerEvents.entrySet())
		{
			if(!pair.getValue().equals(triggerSetStateCache.get(pair.getKey())))
				return false;
		};

		return true;
	}

	public void triggerEvent()
	{
		if (!stateRestoreTask.isDone())
			stateRestoreTask.cancel();

		Device device = Device.lookupDevice(eventState.getName());
		// preEventState = device.getState(); TODO
		Device.queueAction(eventState);
		isTriggered.set(true);

		stateRestoreTask = Device.createTask(() ->
		{
			try
			{
				Device.queueAction(preEventState);
				isTriggered.set(false);
			}
			catch (Exception e)
			{
				Application.LOGGER.severe(e.getMessage());
			}
		}, durationMinutes.longValue(), TimeUnit.MINUTES);
	}

	public void cancel()
	{
		if (!stateRestoreTask.isDone())
		{
			stateRestoreTask.cancel();
			if (preEventState != null)
				Device.queueAction(preEventState);
			isTriggered.set(false);
		}
	}

	public Integer getDurationMinutes()
	{
		return durationMinutes;
	}

	public Set<String> getDependencyDevices()
	{
		return triggerEvents.keySet();
	}

	public void setDurationMinutes(Integer durationMinutes)
	{
		this.durationMinutes = durationMinutes;
	}

	/**
	 * 
	 * @return true if the event is still in an active period
	 */
	public Boolean isTriggered()
	{
		return isTriggered.get();
	}

	@Override
	public Object getDatabaseIdentification()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDatabaseIdentificationForQuery()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashCode()
	{
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
