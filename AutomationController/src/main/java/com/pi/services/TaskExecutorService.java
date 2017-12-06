/**
 * 
 */
package com.pi.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.pi.SystemLogger;

/**
 * @author Christian Everett
 */

public class TaskExecutorService
{
	private Map<Integer, ScheduledFuture<?>> taskMap = new HashMap<>();
	private AtomicInteger atomicInteger = new AtomicInteger();
	private ScheduledExecutorService executorService;
	
	public TaskExecutorService(int threads)
	{
		executorService = Executors.newScheduledThreadPool(threads);
	}
	
	public Task scheduleTask(Runnable task, Long delay, TimeUnit unit)
	{
		return new Task(executorService.schedule(task, delay, unit), this);
	}
	
	public Task scheduleTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		ScheduledFuture<?> scheduledTask = executorService.scheduleWithFixedDelay(task, delay, interval, unit);

		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new ReocurringTask(id, scheduledTask, this);
	}
	
	public Task scheduleSafeTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		ScheduledFuture<?> scheduledTask = executorService.scheduleWithFixedDelay(new SafeRunnable(task), delay, interval, unit);

		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new ReocurringTask(id, scheduledTask, this);
	}
	
	public Task scheduleFixedRateTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		ScheduledFuture<?> scheduledTask = executorService.scheduleAtFixedRate(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new ReocurringTask(id, scheduledTask, this);
	}
	
	public boolean cancel(Integer id, boolean interrupt)
	{
		ScheduledFuture<?> task = taskMap.remove(id);
		
		if(task == null)
			return false;
		
		return task.cancel(interrupt);
	}
	
	public void cancelAllTasks()
	{
		for(Map.Entry<Integer, ScheduledFuture<?>> entry : taskMap.entrySet())
		{
			entry.getValue().cancel(false);
		}
		
		taskMap.clear();
	}
	
	public static class SafeRunnable implements Runnable
	{
		private Runnable runnable;
		
		public SafeRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}
		
		@Override
		public void run()
		{
			try
			{
				runnable.run();
			}
			catch (Throwable e)
			{
				SystemLogger.LOGGER.severe(e.getMessage());
			}	
		}
	}
	
	public static class Task
	{
		protected ScheduledFuture <?> task = null;
		protected TaskExecutorService service = null;
		
		public Task(ScheduledFuture <?> task, TaskExecutorService service)
		{
			this.task = task;
			this.service = service;
		}
		
		public boolean isCancelled()
		{
			return task.isCancelled();
		}
		
		public boolean cancel()
		{
			return task.cancel(false);
		}
		
		public boolean interruptAndCancel()
		{
			return task.cancel(true);
		}
		
		public boolean isDone()
		{
			return task.isDone();
		}
		
		public long minutesUntilExecution()
		{
			return task.getDelay(TimeUnit.MINUTES);
		}
	}
	
	public static class ReocurringTask extends Task
	{
		protected Integer id;
		
		public ReocurringTask(Integer id, ScheduledFuture<?> task, TaskExecutorService service)
		{
			super(task, service);		
			this.id = id;
		}

		@Override
		public boolean cancel()
		{
			return service.cancel(id, false);
		}
		
		@Override
		public boolean interruptAndCancel()
		{
			return service.cancel(id, true);
		}
	}
}
