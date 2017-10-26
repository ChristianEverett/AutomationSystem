package com.pi.infrastructure;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.pi.SystemLogger;
import com.pi.model.DeviceState;
import com.pi.services.TaskExecutorService.Task;

public abstract class AsynchronousDevice extends Device implements Runnable
{
	private Task asynchTask = null;
	
	public AsynchronousDevice(String name) throws IOException
	{
		super(name);
	}
	
	protected void createAsynchronousTask(long intialDelay, long interval, TimeUnit timeUnit)
	{
		createAsynchronousTask(intialDelay, interval, timeUnit, false);
	}
	
	protected void createAsynchronousTask(long intialDelay, long interval, TimeUnit timeUnit, boolean fixedRate)
	{
		if(fixedRate)
		{
			asynchTask = createFixedRateTask(this, intialDelay, interval, timeUnit);
		}
		else
		{
			asynchTask = createTask(this, intialDelay, interval, timeUnit);
		}
	}
	
	public final void run()
	{
		try
		{
			update();
			super.update(getCurrentDeviceState());
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(this.getClass() + " - " + e.getClass().getName() + " - " + e.getMessage());
		}
	}

	protected abstract void update() throws Exception;
	
	@Override
	public boolean isAsynchronousDevice()
	{
		return true;
	}
	
	@Override
	public final void close() throws Exception
	{
		asynchTask.cancel();
		super.close();
	}
}
