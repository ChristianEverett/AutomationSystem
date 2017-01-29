package com.pi.model;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.DatabaseElement;
import com.pi.infrastructure.Device;

public class Event extends DatabaseElement
{
	private Task stateRestoreTask = null;
	private Integer durationMinutes = 0;
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

	public void triggerEvent()
	{
		if (!stateRestoreTask.isDone())
			stateRestoreTask.cancel();

		Device device = Device.lookupDevice(eventState.getName());
		preEventState = device.getState();
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

	public DeviceState getEventAction()
	{
		return eventState;
	}

	public DeviceState getPreEventState()
	{
		return preEventState;
	}

	public void setDurationMinutes(Integer durationMinutes)
	{
		this.durationMinutes = durationMinutes;
	}

	public void setEventAction(DeviceState eventAction)
	{
		this.eventState = eventAction;
	}

	public void setPreEventAction(DeviceState preEventAction)
	{
		this.preEventState = preEventAction;
	}

	public Boolean isTriggered()
	{
		return isTriggered.get();
	}
}
