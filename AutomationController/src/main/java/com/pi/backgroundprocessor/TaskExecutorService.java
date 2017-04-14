/**
 * 
 */
package com.pi.backgroundprocessor;

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
		return new Task(-1, executorService.schedule(task, delay, unit), this);
	}
	
	public Task scheduleTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		Future<?> scheduledTask = executorService.scheduleWithFixedDelay(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new Task(id, scheduledTask, this);
	}
	
	public Task scheduleFixedRateTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		Future<?> scheduledTask = executorService.scheduleAtFixedRate(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return new Task(id, scheduledTask, this);
	}
	
	public boolean cancel(Integer id)
	{
		Future<?> task = taskMap.remove(id);
		
		if(task == null)
			return false;
		
		return task.cancel(false);
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
		private Integer id;
		private Future <?> task = null;
		private TaskExecutorService service = null;
		
		public Task(Integer id, Future <?> task, TaskExecutorService service)
		{
			this.id = id;
			this.task = task;
			this.service = service;
		}
		
		public boolean isCancelled()
		{
			return task.isCancelled();
		}
		
		public boolean cancel()
		{
			return service.cancel(id);
		}
		
		public boolean isDone()
		{
			return task.isDone();
		}
	}
}
