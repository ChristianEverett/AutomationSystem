package com.pi.infrastructure;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.pi.Application;
import com.pi.SystemLogger;
import com.pi.backgroundprocessor.Processor;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.model.DeviceState;

public abstract class AsynchronousDevice extends Device implements Runnable
{
	private Task asynchTask = null;
	
	public AsynchronousDevice(String name) throws IOException
	{
		super(name);
	}
	
	public AsynchronousDevice(String name, long intialDelay, long interval, TimeUnit timeUnit) throws IOException
	{
		this(name, intialDelay, interval, timeUnit, false);	
	}
	
	public AsynchronousDevice(String name, long intialDelay, long interval, TimeUnit timeUnit, boolean fixedRate) throws IOException
	{
		super(name);
		
		if(fixedRate)
		{
			asynchTask = createFixedRateTask(this, intialDelay, interval, timeUnit);
		}
		else
		{
			asynchTask = createTask(this, intialDelay, interval, timeUnit);
		}
	}
	
	public void run()
	{
		try
		{
			update();
			super.update(getState());
		}
		catch (Throwable e)
		{
			SystemLogger.getLogger().severe(this.getClass() + " - " + e.getClass().getName() + " - " + e.getMessage());
		}
	}

	protected abstract void update() throws Exception;
	
	@Override
	public final void close() throws Exception
	{
		asynchTask.cancel();
		super.close();
	}
}
