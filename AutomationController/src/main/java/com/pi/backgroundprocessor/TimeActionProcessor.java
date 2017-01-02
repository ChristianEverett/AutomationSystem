/**
 * 
 */
package com.pi.backgroundprocessor;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.pi.Application;
import com.pi.backgroundprocessor.TaskExecutorService.Task;
import com.pi.infrastructure.TaskMap;
import com.pi.model.TimedAction;

/**
 * @author Christian Everett
 *
 */
public class TimeActionProcessor
{
	private static TimeActionProcessor singlton = null;
	private static Processor bgp = null;

	private TaskMap<TimedAction> timerMap = new TaskMap<>();

	public static void createTimeActionProcessor(Processor bgp) throws Exception
	{
		TimeActionProcessor.bgp = bgp;
		
		if(singlton != null)
			throw new Exception("TimeActionProcessor already created");
		singlton = new TimeActionProcessor();
	}
	
	public static TimeActionProcessor getTimeActionProcessor()
	{
		return singlton;
	}
	
	private TimeActionProcessor()
	{
		
	}
	
	private Task scheduleTimer(TimedAction timedAction)
	{
		int second = Calendar.getInstance().get(Calendar.SECOND);
		int minute = Calendar.getInstance().get(Calendar.MINUTE);
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		
		long currentTimeInSeconds = (hour * 3600L) + (minute * 60L);
		long futureTimeInSeconds = (timedAction.getHour() * 3600L) + (timedAction.getMinute() * 60L);
		long initalDelay = futureTimeInSeconds - currentTimeInSeconds;
		
		if(initalDelay < 0)
			initalDelay = 86_400 + initalDelay;
		
		TimerRunnable task = new TimerRunnable(timedAction);
		return bgp.getThreadExecutorService().scheduleTask(task, initalDelay, 60L * 60L * 24L, TimeUnit.SECONDS);
	}
	
	public synchronized void load(TimedAction timedAction)
	{
		timerMap.put(timedAction, scheduleTimer(timedAction));
	}
	
	public synchronized void load(Collection<TimedAction> timedActions)
	{
		for(TimedAction timedAction : timedActions)
		{
			timerMap.put(timedAction, scheduleTimer(timedAction));
		}
	}
	
	public TimedAction getTimedActionByID(Integer id)
	{
		return timerMap.get(id);	
	}
	
	public Collection<Entry<Integer, TimedAction>> retrieveAllTimedActions()
	{
		return timerMap.getAllValues();
	}
	
	public boolean updateTimedActionByID(Integer id, TimedAction timedAction)
	{
		Task task = timerMap.getTaskID(id);
		if(task == null)
			return false;
		task.cancel();
		
		return timerMap.update(id, timedAction, scheduleTimer(timedAction));
	}
	
	public TimedAction delete(Integer id)
	{	
		Task task = timerMap.getTaskID(id);
		if(task == null)
			return null;
		task.cancel();
		
		return timerMap.delete(id);
	}
	
	public void deleteAll()
	{
		timerMap.getAllTaskIDs().forEach((task) -> 
		{
			task.cancel();
		});
		timerMap.clear();
	}
	
	public void shutdown()
	{
		deleteAll();
		singlton = null;
	}
	
	class TimerRunnable implements Runnable
	{
		private TimedAction timedAction = null;
		
		public TimerRunnable(TimedAction timedAction)
		{
			this.timedAction = timedAction;
		}

		@Override
		public void run()
		{
			bgp.scheduleAction(timedAction.getAction());			
		}
	}
}
