package com.pi.infrastructure;

import java.util.concurrent.TimeUnit;

import com.pi.SystemLogger;
import com.pi.services.TaskExecutorService;
import com.pi.services.TaskExecutorService.Task;

public abstract class BaseService implements Runnable
{
	private static TaskExecutorService taskService = new TaskExecutorService(2);
	private Task serviceTask;
	
	public final synchronized void start()
	{
		start(10);
	}
	
	public final synchronized void start(int seconds)
	{
		if(serviceTask == null || serviceTask.isDone())
			serviceTask = taskService.scheduleTask(this, 1L, (long) seconds, TimeUnit.SECONDS);
	}
	
	public final synchronized void stop()
	{
		serviceTask.cancel();
		close();
	}
	
	@Override
	public final void run()
	{
		try
		{
			executeService();
		}
		catch (Exception e)
		{
			SystemLogger.getLogger().severe(e.getMessage());
		}	
	}
	
	protected abstract void executeService() throws Exception;
	protected abstract void close();
}
