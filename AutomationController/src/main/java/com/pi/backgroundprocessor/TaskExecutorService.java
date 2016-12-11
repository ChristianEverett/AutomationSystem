/**
 * 
 */
package com.pi.backgroundprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
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
	AtomicInteger atomicInteger = new AtomicInteger();
	private ScheduledExecutorService executorService;
	
	public TaskExecutorService(int threads)
	{
		executorService = Executors.newScheduledThreadPool(threads);
	}
	
	public Future<?> scheduleTask(Runnable task, Long delay, TimeUnit unit)
	{
		return executorService.schedule(task, delay, unit);
	}
	
	public Integer scheduleTask(Runnable task, Long delay, Long interval, TimeUnit unit)
	{
		Future<?> scheduledTask = executorService.scheduleWithFixedDelay(task, delay, interval, unit);
		
		int id = atomicInteger.incrementAndGet();
		
		taskMap.put(id, scheduledTask);
		
		return id;
	}
	
	public Future<?> cancelTask(Integer id)
	{
		Future<?> task = taskMap.remove(id);
		
		if(task == null)
			throw new RuntimeException("Attempt to cancel task that does not exist");
		
		task.cancel(false);
		return task;
	}
	
	public void cancelAllTasks()
	{
		for(Map.Entry<Integer, Future<?>> entry : taskMap.entrySet())
		{
			entry.getValue().cancel(false);
		}
		
		taskMap.clear();
	}
}
