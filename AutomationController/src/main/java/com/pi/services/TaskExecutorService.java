/**
 * 
 */
package com.pi.services;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Christian Everett
 */
public class TaskExecutorService
{
	private Map<Integer, Future<?>> taskMap = new HashMap<>();
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
		Future<?> scheduledTask = executorService.scheduleWithFixedDelay(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new ReocurringTask(id, scheduledTask, this);
	}
	
	public Task scheduleFixedRateTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		Future<?> scheduledTask = executorService.scheduleAtFixedRate(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new ReocurringTask(id, scheduledTask, this);
	}
	
	public boolean cancel(Integer id, boolean interrupt)
	{
		Future<?> task = taskMap.remove(id);
		
		if(task == null)
			return false;
		
		return task.cancel(interrupt);
	}
	
	public void cancelAllTasks()
	{
		for(Map.Entry<Integer, Future<?>> entry : taskMap.entrySet())
		{
			entry.getValue().cancel(false);
		}
		
		taskMap.clear();
	}
	
	public static class Task
	{
		protected Future <?> task = null;
		protected TaskExecutorService service = null;
		
		public Task(Future <?> task, TaskExecutorService service)
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
	}
	
	public static class ReocurringTask extends Task
	{
		protected Integer id;
		
		public ReocurringTask(Integer id, Future<?> task, TaskExecutorService service)
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
